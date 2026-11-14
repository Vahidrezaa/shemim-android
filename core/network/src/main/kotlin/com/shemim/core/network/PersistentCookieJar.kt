package com.shemim.core.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.util.concurrent.ConcurrentHashMap

/**
 * CookieJar پایدار — WP-0.3.
 *
 * همان نقشی که مرورگر برای وب دارد:
 *  - کوکی نشست PHP (`PHPSESSID`) بین اجراهای اپ نگه داشته می‌شود → ورود خودکار
 *    بعد از بستن اپ (معیار M0).
 *  - چرخش کوکی بعد از لاگین (`session_regenerate_id`) خودکار اعمال می‌شود:
 *    کلید نگهداری «دامنه + مسیر + نام» است، پس کوکی جدید جایگزین قبلی می‌شود.
 *  - کوکی منقضی (خروج سرور زمان انقضا را به گذشته ست می‌کند) حذف می‌شود.
 */
class PersistentCookieJar(private val store: CookieStore) : CookieJar {

    @Serializable
    private data class StoredCookie(
        val name: String,
        val value: String,
        val domain: String,
        val path: String,
        val expiresAt: Long,
        val secure: Boolean,
        val httpOnly: Boolean,
        val hostOnly: Boolean,
    )

    private val json = Json { ignoreUnknownKeys = true }
    private val cache = ConcurrentHashMap<String, Cookie>()

    init {
        // «اجرای بعدی اپ»: بازیابی از ذخیره‌گاه پایدار
        store.load()?.let { persisted ->
            runCatching {
                json.decodeFromString(ListSerializer(StoredCookie.serializer()), persisted)
            }.getOrNull()?.forEach { stored ->
                if (stored.expiresAt > System.currentTimeMillis()) {
                    cache[key(stored.domain, stored.path, stored.name)] = stored.toOkHttp()
                }
            }
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> =
        cache.values.filter { it.matches(url) }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        if (cookies.isEmpty()) return
        val now = System.currentTimeMillis()
        for (cookie in cookies) {
            val k = key(cookie.domain, cookie.path, cookie.name)
            // خروج سرور: کوکی با انقضای گذشته (یا مقدار خالی) → حذف
            if (cookie.expiresAt <= now || cookie.value.isEmpty()) {
                cache.remove(k)
            } else {
                cache[k] = cookie
            }
        }
        persist()
    }

    /** پاک‌کردن همهٔ کوکی‌ها — بعد از logout یا auth_required. */
    fun clear() {
        cache.clear()
        store.clear()
    }

    @Synchronized
    private fun persist() {
        val list = cache.values.map { it.toStored() }
        val text = json.encodeToString(ListSerializer(StoredCookie.serializer()), list)
        store.save(text)
    }

    private fun key(domain: String, path: String, name: String) = "$domain|$path|$name"

    private fun Cookie.toStored() = StoredCookie(
        name = name,
        value = value,
        domain = domain,
        path = path,
        expiresAt = expiresAt,
        secure = secure,
        httpOnly = httpOnly,
        hostOnly = hostOnly,
    )

    private fun StoredCookie.toOkHttp(): Cookie {
        val builder = Cookie.Builder()
            .name(name)
            .value(value)
            .path(path)
            .expiresAt(expiresAt)
        if (hostOnly) builder.hostOnlyDomain(domain) else builder.domain(domain)
        if (secure) builder.secure()
        if (httpOnly) builder.httpOnly()
        return builder.build()
    }
}
