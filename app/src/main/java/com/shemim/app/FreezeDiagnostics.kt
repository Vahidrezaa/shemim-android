package com.shemim.app

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * عیب‌یابی فریز/کرش روی گوشی — WP-0.3.
 *
 * چون adb در اختیار نیست، گزارش‌ها داخل حافظهٔ خصوصی اپ
 * (files/diagnostics.log) نوشته می‌شوند و دفعهٔ بعدی که اپ باز می‌شود،
 * اگر گزارش تازه‌ای هست، صفحهٔ اشتراک‌گذاری سیستم باز می‌شود تا
 * کاربر متن را برای توسعه‌دهنده بفرستد.
 *
 * سه منبع گزارش:
 *  ۱) کرش مدیریت‌نشده (استک کامل)؛
 *  ۲) توقف بیش از ۸ ثانیه‌ای ترد اصلی (استک لحظهٔ فریز)؛
 *  ۳) همه‌چیز سقف ۲۵۶KB دارد که فایل بی‌نهایت نشود.
 *
 * همهٔ عملیات در runCatching است: عیب‌یابی هرگز نباید خودش
 * باعث کرش یا فریز شود.
 */
object FreezeDiagnostics {

    private const val HEARTBEAT_MS = 1_500L
    private const val STALL_MS = 8_000L
    private const val MAX_BYTES = 256 * 1024
    private const val FILE_NAME = "diagnostics.log"
    private const val SHARED_MARKER = "diagnostics.shared"

    @Volatile
    private var beat = 0L

    private val lock = Any()

    // ---------- نصب ----------

    fun install(context: Context) {
        val file = File(context.filesDir, FILE_NAME)

        // ۱) کرش‌های مدیریت‌نشده — زنجیره با handler قبلی تا رفتار سیستم عوض نشود
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { append(file, "CRASH thread=${thread.name}\n${throwable.stackTraceToString()}\n") }
            previous?.uncaughtException(thread, throwable)
        }

        // ۲) نگهبان فریز: ضربان منظم از ترد اصلی؛ اگر قطع شود، استک ثبت می‌شود
        val main = Handler(Looper.getMainLooper())
        val tick = object : Runnable {
            override fun run() {
                beat++
                main.postDelayed(this, HEARTBEAT_MS)
            }
        }
        Thread {
            var seen = -1L
            while (true) {
                Thread.sleep(STALL_MS)
                val current = beat
                if (current == seen) {
                    runCatching {
                        append(
                            file,
                            "MAIN-THREAD STALL ${STALL_MS / 1000}s+ stack:\n" +
                                Looper.getMainLooper().thread.stackTrace
                                    .joinToString("\n") { "  at $it" } + "\n",
                        )
                    }
                    // استراحت برای جلوگیری از پرشدن لاگ در فریز طولانی
                    Thread.sleep(STALL_MS * 3)
                }
                seen = beat
            }
        }.apply {
            name = "shemim-freeze-watchdog"
            isDaemon = true
            start()
        }
        main.post(tick)
    }

    // ---------- اشتراک گزارش تازه در باز شدن بعدی ----------

    /**
     * اگر بعد از آخرین اشتراک، گزارش تازه‌ای نوشته شده باشد،
     * صفحهٔ اشتراک‌گذاری باز می‌شود (فقط یک بار به‌ازای هر گزارش).
     */
    fun shareReportIfNew(context: Context) {
        runCatching {
            val file = File(context.filesDir, FILE_NAME)
            if (!file.exists() || file.length() == 0L) return

            val marker = File(context.filesDir, SHARED_MARKER)
            if (marker.exists() && marker.lastModified() >= file.lastModified()) return

            val text = file.readText().takeLast(MAX_BYTES)
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "گزارش عیب‌یابی اپ شمیم")
                putExtra(Intent.EXTRA_TEXT, text)
            }
            val chooser = Intent.createChooser(send, context.getString(R.string.diag_share_title))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            marker.writeText("shared")
        }
    }

    // ---------- نوشتن (سقف‌دار و sync) ----------

    private fun append(file: File, text: String) = synchronized(lock) {
        runCatching {
            file.appendText("${ts()} $text")
            if (file.length() > MAX_BYTES) {
                val keep = file.readText().takeLast(MAX_BYTES / 2)
                file.writeText(keep)
            }
        }
    }

    private fun ts(): String =
        SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
}
