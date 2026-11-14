package com.shemim.core.network

import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * تست‌های CookieJar پایدار — WP-0.3.
 *
 * سناریوهای حیاتی معیار M0:
 *  ۱) کوکی نشست ذخیره و فقط برای همان دامنه ارسال می‌شود
 *  ۲) چرخش کوکی لاگین (session_regenerate_id) جایگزین می‌کند، نه تکرار
 *  ۳) کوکی منقضی‌شدهٔ خروج حذف می‌شود
 *  ۴) کوکی بین «اجراهای اپ» (jar جدید از همان store) باقی می‌ماند
 * (نام متدها به دلیل محدودیت شناسهٔ JVM روی نویسه‌های فارسی، ASCII است.)
 */
class PersistentCookieJarTest {

    private val apiUrls = "https://abcd.khaneh-soorati.ir/api.php?action=login".toHttpUrl()
    private val sameHost = "https://abcd.khaneh-soorati.ir/anything".toHttpUrl()
    private val otherHost = "https://example.com/api.php".toHttpUrl()

    private fun cookie(headerValue: String): Cookie =
        Cookie.parse(apiUrls, headerValue)!!

    @Test
    fun sessionCookie_isStored_andSent_toSameDomainOnly() {
        val jar = PersistentCookieJar(InMemoryCookieStore())

        jar.saveFromResponse(apiUrls, listOf(cookie("PHPSESSID=abc123; Path=/")))

        val sent = jar.loadForRequest(apiUrls)
        assertEquals(1, sent.size)
        assertEquals("PHPSESSID", sent[0].name)
        assertEquals("abc123", sent[0].value)

        // همان دامنه، مسیر دیگر: کوکی Path=/ است → می‌رود
        assertEquals(1, jar.loadForRequest(sameHost).size)

        // دامنهٔ دیگر: هرگز
        assertTrue(jar.loadForRequest(otherHost).isEmpty())
    }

    @Test
    fun loginCookieRotation_replacesOldCookie_notDuplicates() {
        val jar = PersistentCookieJar(InMemoryCookieStore())

        jar.saveFromResponse(apiUrls, listOf(cookie("PHPSESSID=first; Path=/")))
        // لاگین موفق: سرور session_regenerate_id می‌زند → کوکی جدید
        jar.saveFromResponse(apiUrls, listOf(cookie("PHPSESSID=second; Path=/")))

        val sent = jar.loadForRequest(apiUrls)
        assertEquals(1, sent.size)
        assertEquals("second", sent[0].value)
    }

    @Test
    fun expiredLogoutCookie_isRemoved() {
        val jar = PersistentCookieJar(InMemoryCookieStore())
        jar.saveFromResponse(apiUrls, listOf(cookie("PHPSESSID=abc; Path=/")))
        assertTrue(jar.loadForRequest(apiUrls).isNotEmpty())

        // خروج سرور: انقضا در گذشته
        jar.saveFromResponse(
            apiUrls,
            listOf(cookie("PHPSESSID=abc; Path=/; Expires=Thu, 01 Jan 1970 00:00:00 GMT")),
        )

        assertTrue(jar.loadForRequest(apiUrls).isEmpty())
    }

    @Test
    fun cookie_survivesAppRestart_viaStore() {
        val store = InMemoryCookieStore()
        PersistentCookieJar(store)
            .saveFromResponse(apiUrls, listOf(cookie("PHPSESSID=persist; Path=/")))

        // «اجرای بعدی اپ»: نمونهٔ جدید از همان ذخیره‌گاه
        val jarAfterRestart = PersistentCookieJar(store)
        val sent = jarAfterRestart.loadForRequest(apiUrls)
        assertEquals(1, sent.size)
        assertEquals("persist", sent[0].value)
    }

    @Test
    fun expiredCookie_isNotRestored_fromStore() {
        val store = InMemoryCookieStore()
        // شبیه‌سازی ذخیره‌گاهی که فقط کوکی منقضی دارد
        PersistentCookieJar(store)
            .saveFromResponse(apiUrls, listOf(cookie("PHPSESSID=old; Path=/; Expires=Thu, 01 Jan 1970 00:00:00 GMT")))

        val jarAfterRestart = PersistentCookieJar(store)
        assertTrue(jarAfterRestart.loadForRequest(apiUrls).isEmpty())
    }

    @Test
    fun clear_wipesCacheAndStore() {
        val store = InMemoryCookieStore()
        val jar = PersistentCookieJar(store)
        jar.saveFromResponse(apiUrls, listOf(cookie("PHPSESSID=abc; Path=/")))

        jar.clear()

        assertTrue(jar.loadForRequest(apiUrls).isEmpty())
        assertNull(store.load())
    }
}
