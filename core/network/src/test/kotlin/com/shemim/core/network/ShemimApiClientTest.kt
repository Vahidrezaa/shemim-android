package com.shemim.core.network

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * تست‌های کلاینت API با سرور جعلی (MockWebServer) — WP-0.3.
 *
 * پاسخ‌های ضبط‌شده از رفتار واقعی `api.php` (سند ۰۱ §۳):
 *  - login موفق: {success:true, user:{…}} + Set-Cookie نشست
 *  - رمز اشتباه: {success:false, error:"…فارسی…"}
 *  - تلاش زیاد: code=too_many_attempts
 *  - نشست باطل: error=auth_required → پاک‌شدن کوکی + رویداد سراسری
 *  - پاسخ خراب HTML → MalformedResponse
 *  - قطع شبکه → NetworkError(likelyOffline)
 * (نام متدها به دلیل محدودیت شناسهٔ JVM روی نویسه‌های فارسی، ASCII است.)
 */
class ShemimApiClientTest {

    private lateinit var server: MockWebServer
    private lateinit var jar: PersistentCookieJar
    private lateinit var api: ShemimApiClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        jar = PersistentCookieJar(InMemoryCookieStore())
        api = ShemimApiClient(
            baseUrl = server.url("/api.php").toString(),
            cookieJar = jar,
        )
    }

    @After
    fun tearDown() {
        runCatching { server.shutdown() }
    }

    @Test
    fun loginSuccess_returnsUser_andStoresCookie() = runBlocking {
        server.enqueue(
            MockResponse()
                .addHeader("Set-Cookie", "PHPSESSID=sess123; Path=/")
                .setBody(
                    """{"success":true,"user":{"id":"m-1","username":"ali","firstName":"علی","lastName":"رضایی","role":"user"}}""",
                ),
        )

        val result = api.login("ali", "1234")

        assertTrue("expected Ok but was $result", result is ApiResult.Ok)
        val user = parseLoginUser((result as ApiResult.Ok).body)
        assertNotNull(user)
        assertEquals("ali", user!!.username)
        assertEquals("علی", user.firstName)
        assertEquals("m-1", user.id)

        // شکل درخواست — دقیقاً مثل callApi وب: POST + ?action= + JSON
        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertTrue(recorded.path!!.startsWith("/api.php?action=login"))
        assertTrue(
            recorded.getHeader("Content-Type")!!.startsWith("application/json"),
        )
        val bodyText = recorded.body.readUtf8()
        assertTrue(bodyText.contains("\"username\":\"ali\""))
        assertTrue(bodyText.contains("\"password\":\"1234\""))

        // کوکی نشست باید در Jar ذخیره شده باشد
        val cookies = jar.loadForRequest(server.url("/api.php"))
        assertEquals(1, cookies.size)
        assertEquals("sess123", cookies[0].value)
    }

    @Test
    fun wrongPassword_returnsServerError_withPersianMessage() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """{"success":false,"error":"نام کاربری یا رمز عبور اشتباه است"}""",
            ),
        )

        val result = api.login("ali", "wrong")

        assertTrue(result is ApiResult.ServerError)
        assertEquals("نام کاربری یا رمز عبور اشتباه است", (result as ApiResult.ServerError).error)
    }

    @Test
    fun tooManyAttempts_returnsCode() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """{"success":false,"code":"too_many_attempts","error":"تعداد تلاش‌های ناموفق زیاد است. لطفاً ۱۰ دقیقه بعد دوباره تلاش کنید."}""",
            ),
        )

        val result = api.login("ali", "x")

        val error = result as ApiResult.ServerError
        assertEquals("too_many_attempts", error.code)
    }

    @Test
    fun authRequired_clearsCookie_andEmitsEvent() = runBlocking {
        // اول یک نشست بگذار
        server.enqueue(
            MockResponse()
                .addHeader("Set-Cookie", "PHPSESSID=sess123; Path=/")
                .setBody("""{"success":true}"""),
        )
        api.call("getTrialStatus")
        assertTrue(jar.loadForRequest(server.url("/api.php")).isNotEmpty())

        // مشترکِ رویداد سراسری
        val eventReceived = CompletableDeferred<Unit>()
        val collector = launch { api.authEvents.first(); eventReceived.complete(Unit) }
        delay(200) // بگذار مشترک شود

        // سرور نشست را باطل می‌کند (مثل انقضای نشست هنگام خروج از اپ دیگر)
        server.enqueue(
            MockResponse().setBody(
                """{"success":false,"error":"auth_required","message":"ابتدا باید وارد حساب کاربری خود شوید."}""",
            ),
        )
        val result = api.call("loadData")

        assertTrue(result is ApiResult.AuthRequired)
        // کوکی محلی پاک شده است
        assertTrue(jar.loadForRequest(server.url("/api.php")).isEmpty())
        // و رویداد سراسری رسیده است
        withTimeout(3_000) { eventReceived.await() }
        collector.cancel()
    }

    @Test
    fun cookieSent_onSubsequentRequests() = runBlocking {
        server.enqueue(
            MockResponse()
                .addHeader("Set-Cookie", "PHPSESSID=sess123; Path=/")
                .setBody("""{"success":true}"""),
        )
        api.login("ali", "1234")

        server.enqueue(MockResponse().setBody("""{"success":true}"""))
        api.call("getTrialStatus")

        server.takeRequest() // درخواست login
        val secondRequest = server.takeRequest() // درخواست بعدی
        assertEquals("PHPSESSID=sess123", secondRequest.getHeader("Cookie"))
    }

    @Test
    fun htmlErrorResponse_isMalformedResponse() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(500).setBody("<html>Fatal error...</html>"),
        )

        val result = api.call("loadData")

        assertTrue(result is ApiResult.MalformedResponse)
    }

    @Test
    fun connectionRefused_isNetworkError_likelyOffline() = runBlocking {
        val deadUrl = server.url("/api.php").toString()
        server.shutdown()

        val deadApi = ShemimApiClient(
            baseUrl = deadUrl,
            cookieJar = PersistentCookieJar(InMemoryCookieStore()),
        )

        val result = deadApi.call("login", buildJsonObject {
            put("username", "a")
            put("password", "b")
        })

        assertTrue("expected NetworkError but was $result", result is ApiResult.NetworkError)
        assertTrue((result as ApiResult.NetworkError).likelyOffline)
    }
}
