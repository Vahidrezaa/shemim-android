package com.shemim.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/**
 * کلاینت واحد API — WP-0.3.
 *
 * قرارداد ۱:۱ با وب (`callApi` در auth-core.js):
 *  - `POST {base}?action=<action>` با بدنهٔ JSON و Content-Type: application/json
 *  - پاسخ همیشه JSON «{success: bool, …}» (حتی خطاها — HTTP 200)
 *  - احراز هویت با کوکی نشست PHP از طریق CookieJar پایدار
 *  - gzip به‌صورت خودکار توسط OkHttp (Accept-Encoding)
 *
 * OkHttp خالص به‌جای Retrofit — ADR-017: قرارداد تک‌endpoint اکشن‌محور است و
 * wrapper عمومی + اکشن رشته‌ای، مطابق طراحی سند ۰۲ §۳.
 */
class ShemimApiClient(
    baseUrl: String = DEFAULT_BASE_URL,
    private val cookieJar: PersistentCookieJar,
    readTimeoutSeconds: Long = DEFAULT_READ_TIMEOUT_SECONDS,
) {

    private val _authEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 8)

    /**
     * رویداد سراسری «نشست از سمت سرور باطل شد».
     * اپ این جریان را جمع می‌کند و به صفحهٔ ورود برمی‌گردد
     * (همان رفتار `forceLogoutToLoginPage` وب — سند ۰۲ §۳).
     */
    val authEvents: SharedFlow<Unit> = _authEvents

    private val baseUrlHttp: HttpUrl = baseUrl.toHttpUrl()

    private val http: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        // loadData برای صندوق‌های بزرگ می‌تواند چند مگابایت باشد — سند ۰۲ §۶
        .callTimeout(120, TimeUnit.SECONDS)
        .build()

    /**
     * فراخوانی عمومی — همتای `callApi(action, data)` وب.
     * اکشن‌های نوشتنی فقط از طریق همین تابع صدا زده می‌شوند؛
     * `saveData` هرگز از سمت اپ صدا زده نمی‌شود (ADR-002).
     */
    suspend fun call(action: String, body: JsonObject = JsonObject(emptyMap())): ApiResult =
        withContext(Dispatchers.IO) {
            val url = baseUrlHttp.newBuilder()
                .setQueryParameter("action", action)
                .build()
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .post(body.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()
            try {
                http.newCall(request).execute().use { response ->
                    val text = response.body?.string().orEmpty()
                    parseResponse(response.code, text)
                }
            } catch (e: IOException) {
                ApiResult.NetworkError(
                    detail = e.message ?: e.javaClass.simpleName,
                    likelyOffline = e.isLikelyOffline(),
                )
            }
        }

    /**
     * ورود — همان `callApi('login')` وب؛ فقط چک اعتبار است و داده برنمی‌گرداند؛
     * کلاینت بعداً داده را جداگانه با `loadData` می‌گیرد (WP-0.5).
     * کوکی نشست توسط CookieJar ذخیره و در چرخش لاگین جایگزین می‌شود.
     */
    suspend fun login(username: String, password: String): ApiResult = call(
        action = "login",
        body = buildJsonObject {
            put("username", username)
            put("password", password)
        },
    )

    /** خروج — نشست سمت سرور پایان می‌یابد و کوکیِ منقضی برمی‌گردد. */
    suspend fun logout(): ApiResult = call("logout")

    // ---------- پارس پاسخ ----------

    private fun parseResponse(httpCode: Int, text: String): ApiResult {
        val element = try {
            Json.parseToJsonElement(text)
        } catch (e: SerializationException) {
            return ApiResult.MalformedResponse(detail = "HTTP $httpCode: ${text.take(120)}")
        }
        val obj = element as? JsonObject
            ?: return ApiResult.MalformedResponse(detail = "HTTP $httpCode: پاسخ JSON آبجکت نیست")

        val success = (obj["success"] as? JsonPrimitive)?.booleanOrNull ?: false
        if (success) return ApiResult.Ok(obj)

        val error = obj.str("error")
        if (error == ERROR_AUTH_REQUIRED) {
            // مثل وب: نشست باطل است → پاک‌کردن محلی + رویداد سراسری
            cookieJar.clear()
            _authEvents.tryEmit(Unit)
            return ApiResult.AuthRequired
        }
        return ApiResult.ServerError(
            error = error,
            code = obj.str("code"),
            message = obj.str("message"),
        )
    }

    private fun JsonObject.str(key: String): String? {
        val primitive = this[key] as? JsonPrimitive ?: return null
        return primitive.contentOrNull
    }

    private fun IOException.isLikelyOffline(): Boolean =
        this is UnknownHostException || this is ConnectException || this is SocketTimeoutException

    companion object {
        /** آدرس ثابت API صندوق — تک‌منبع حقیقت (سند ۰۱ §۱). */
        const val DEFAULT_BASE_URL = "https://abcd.khaneh-soorati.ir/api.php"

        const val USER_AGENT = "ShemimAndroid/0.1"

        const val DEFAULT_READ_TIMEOUT_SECONDS = 60L

        private const val ERROR_AUTH_REQUIRED = "auth_required"
    }
}
