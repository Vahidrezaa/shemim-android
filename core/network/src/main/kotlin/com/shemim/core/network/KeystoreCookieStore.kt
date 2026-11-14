package com.shemim.core.network

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * ذخیره‌گاه رمزنگاری‌شدهٔ کوکی نشست — WP-0.3 (ADR-018).
 *
 * کوکی `PHPSESSID` عملاً «کلید ۱۰ سالهٔ» حساب کاربری است؛ پس با کلید
 * AES-256-GCM داخل AndroidKeyStore رمز شده و در فایل خصوصی اپ ذخیره می‌شود
 * (نگهداری در ذخیره‌گاه رمزنگاری‌شده — سند ۰۱ §۸ و سند ۰۲ §۹).
 *
 * سیاست خطا — صادقانه و بی‌درز:
 * اگر به هر دلیلی (خرابی Keystore در برخی گوشی‌ها، فایل ناقص و…) رمزگشایی
 * ممکن نباشد، کوکی دور ریخته می‌شود و کاربر فقط یک بار دیگر وارد می‌شود؛
 * هیچ دادهٔ مالی‌ای از دست نمی‌رود (منبع حقیقت، سرور است).
 */
class KeystoreCookieStore(context: Context) : CookieStore {

    private val file: File = File(context.filesDir, FILE_NAME)

    override fun load(): String? = try {
        if (!file.exists()) return null
        val blob = file.readBytes()
        if (blob.size <= IV_SIZE_BYTES) return null
        val iv = blob.copyOfRange(0, IV_SIZE_BYTES)
        val ciphertext = blob.copyOfRange(IV_SIZE_BYTES, blob.size)
        val cipher = Cipher.getInstance(TRANSFORM)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, iv))
        String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    } catch (t: Throwable) {
        // فقط نام کلاس خطا لاگ می‌شود؛ محتوای کوکی هرگز لاگ نمی‌شود (سند ۰۲ §۹)
        Log.w(TAG, "session cookie unreadable, dropping (${t.javaClass.simpleName})")
        runCatching { file.delete() }
        null
    }

    override fun save(json: String) {
        try {
            val cipher = Cipher.getInstance(TRANSFORM)
            cipher.init(Cipher.ENCRYPT_MODE, key())
            val iv = cipher.iv
            val ciphertext = cipher.doFinal(json.toByteArray(Charsets.UTF_8))
            // نوشتن اتمیک: اول فایل موقت، بعد جایگزینی
            val tmp = File(file.parentFile, "$FILE_NAME.tmp")
            tmp.writeBytes(iv + ciphertext)
            if (!tmp.renameTo(file)) {
                file.delete()
                check(tmp.renameTo(file)) { "atomic rename failed" }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "session cookie save failed (${t.javaClass.simpleName})")
        }
    }

    override fun clear() {
        runCatching { file.delete() }
    }

    /** کلید AES داخل Keystore؛ در اولین استفاده ساخته می‌شود. */
    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    private companion object {
        const val TAG = "KeystoreCookieStore"
        const val FILE_NAME = "session_cookies.bin"
        const val ALIAS = "shemim_session_cookies"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORM = "AES/GCM/NoPadding"
        const val IV_SIZE_BYTES = 12
        const val TAG_BITS = 128
    }
}
