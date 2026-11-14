package com.shemim.core.network

/**
 * ذخیره‌گاه پایدار کوکی‌ها — WP-0.3.
 *
 * پیاده‌سازی واقعی (`KeystoreCookieStore`) کوکی‌ها را رمزنگاری‌شده نگه می‌دارد؛
 * پیاده‌سازی حافظه‌ای برای تست‌های واحد.
 */
interface CookieStore {
    /** رشتهٔ JSON کوکی‌های ذخیره‌شده؛ null یعنی چیزی ذخیره نشده. */
    fun load(): String?

    fun save(json: String)

    fun clear()
}

/** ذخیره‌گاه حافظه‌ای — فقط برای تست‌ها. */
class InMemoryCookieStore : CookieStore {
    private var value: String? = null

    override fun load(): String? = value

    override fun save(json: String) {
        value = json
    }

    override fun clear() {
        value = null
    }
}
