package com.shemim.app

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * عیب‌یابی فریز/کرش روی گوشی — WP-0.3 (نسخهٔ ۲).
 *
 * چون adb در اختیار نیست، گزارش‌ها داخل حافظهٔ خصوصی اپ نوشته می‌شوند و
 * به‌محض ثبت فریز/کرش، یک نسخه هم به پوشهٔ عمومی Downloads صادر می‌شود
 * (بدون نیاز به هیچ UI — حتی وسط فریز جواب می‌دهد). کاربر فایل را از
 * تلگرام/فایل‌منیجر می‌فرستد.
 *
 * دو جریان جدا:
 *  - milestones.log — نقاط عطف چرخهٔ اجرا (app-onCreate، اولین فریم رسم‌شده و…)
 *  - diagnostics.log — فقط رخدادهای مشکوک: توقف ترد اصلی (استک کامل) و کرش
 *    (جدا بودن مهم است: میل‌استون‌ها نباید «گزارش جدید» را تریگر کنند)
 *
 * همهٔ عملیات در runCatching است: عیب‌یابی هرگز نباید خودش علت کرش شود.
 */
object FreezeDiagnostics {

    private const val TAG = "ShemimDiag"

    private const val HEARTBEAT_MS = 1_500L
    private const val STALL_MS = 8_000L

    private const val REPORT_FILE = "diagnostics.log"
    private const val MILESTONES_FILE = "milestones.log"
    private const val EXPORT_MARKER = "diagnostics.exported"

    /** نام فایل صادرشده در Downloads. */
    const val EXPORT_NAME = "shemim-diagnostics.txt"

    private const val REPORT_MAX_BYTES = 512 * 1024
    private const val MILESTONES_MAX_BYTES = 64 * 1024

    @Volatile
    private var beat = 0L

    private val lock = Any()
    private lateinit var appContext: Context

    // ---------- نصب ----------

    fun install(context: Context) {
        appContext = context.applicationContext
        milestone("install")

        // ۱) کرش‌های مدیریت‌نشده — زنجیره با handler قبلی تا رفتار سیستم عوض نشود
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                append(
                    reportFile(),
                    "CRASH thread=${thread.name}\n${throwable.stackTraceToString()}\n",
                    REPORT_MAX_BYTES,
                )
            }
            runCatching { exportReport() }
            previous?.uncaughtException(thread, throwable)
        }

        // ۲) نگهبان فریز: ضربان منظم از ترد اصلی؛ اگر قطع شود، استک‌ها ثبت
        //    و مستقیم به Downloads صادر می‌شود (ترد اصلی که خواب است فایل می‌سازیم)
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
                    runCatching { recordStall() }
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

    // ---------- میل‌استون‌ها ----------

    /**
     * ثبت نقطهٔ عطف چرخهٔ اجرا — logcat (برای برداشت CI) + milestones.log.
     * سبک و ارزان؛ در دل composition هم می‌شود صدا زد.
     */
    fun milestone(name: String) {
        Log.i(TAG, "MILESTONE $name")
        if (!::appContext.isInitialized) return
        runCatching {
            append(File(appContext.filesDir, MILESTONES_FILE), "MILESTONE $name\n", MILESTONES_MAX_BYTES)
        }
    }

    // ---------- خروجی گرفتن (onResume اکتیویتی) ----------

    /**
     * در onResume اکتیویتی — اینجا اپ در پیش‌زمینه است (علی‌رغم
     * Application.onCreate که اندروید ۱۰+ اجازهٔ بازکردن UI را از آن نمی‌دهد).
     * اگر از آخرین صادرات، فریز/کرش تازه‌ای ثبت شده باشد:
     * صادرات به Downloads + اعلان Toast (سیستمی — حتی اگر بلافاصله دوباره
     * فریز شود، اعلان دیده می‌شود).
     */
    fun onAppForeground(context: Context) {
        runCatching {
            milestone("activity-resumed")

            val report = reportFile()
            if (!report.exists() || report.length() == 0L) return

            val marker = File(context.filesDir, EXPORT_MARKER)
            if (marker.exists() && marker.lastModified() >= report.lastModified()) return

            exportReport()
            marker.writeText("exported")

            Toast.makeText(
                context,
                context.getString(R.string.diag_saved_toast, EXPORT_NAME),
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    // ---------- ثبت فریز ----------

    private fun recordStall() {
        val file = reportFile()
        val mainThread = Looper.getMainLooper().thread

        // در logcat هم دیده شود (فایل به‌تنهایی در logcat ظاهر نمی‌شود)
        Log.w(TAG, "MAIN-THREAD STALL ${STALL_MS / 1000}s+ — exporting $EXPORT_NAME")

        // ابتدا استک همهٔ تردها؛ در سقف‌گیری «انتهای» فایل حفظ می‌شود،
        // پس استکِ خودِ ترد اصلی را آخر می‌نویسیم که قطعاً باقی بماند.
        append(
            file,
            buildString {
                append("MAIN-THREAD STALL ${STALL_MS / 1000}s+ at ").append(ts()).append('\n')
                append("ALL-THREADS:\n")
                Thread.getAllStackTraces().forEach { (thread, stack) ->
                    append("Thread[").append(thread.name).append("] ").append(thread.state).append('\n')
                    stack.forEach { frame -> append("  at ").append(frame).append('\n') }
                }
                append("MAIN-THREAD STACK:\n")
                mainThread.stackTrace.forEach { frame -> append("  at ").append(frame).append('\n') }
                append('\n')
            },
            REPORT_MAX_BYTES,
        )

        // صادرات مستقیم از همین ترد پس‌زمینه: binder call است و ترد اصلی لازم ندارد
        runCatching { exportReport() }
    }

    // ---------- صادرات به Downloads ----------

    /** محتوای کامل (میل‌استون‌ها + گزارش) را به Downloads می‌نویسد. */
    private fun exportReport() {
        if (!::appContext.isInitialized) return
        val text = buildString {
            append("شمیم — گزارش عیب‌یابی\n")
            append("device: Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n")
            append("app: ").append(appContext.packageName).append('\n')
            append("\n===== MILESTONES =====\n")
            runCatching { append(File(appContext.filesDir, MILESTONES_FILE).readText()) }
            append("\n===== REPORT (فریز/کرش) =====\n")
            runCatching { append(reportFile().readText()) }
        }.take(REPORT_MAX_BYTES)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = appContext.contentResolver
            runCatching {
                // حذف نسخهٔ قبلی با همین نام
                resolver.delete(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    "${MediaStore.Downloads.DISPLAY_NAME} = ?",
                    arrayOf(EXPORT_NAME),
                )
            }
            runCatching {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, EXPORT_NAME)
                    put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return
                resolver.openOutputStream(uri)?.use { stream ->
                    stream.write(text.toByteArray(Charsets.UTF_8))
                    stream.flush()
                }
            }
        } else {
            // اندروید ۹ و پایین‌تر: بدون مجوز نوشتن در Downloads عمومی ممکن نیست؛
            // پوشهٔ اختصاصی اپ روی حافظهٔ خارجی (قابل دسترس با کابل/فایل‌منیجر)
            runCatching {
                val dir = appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    ?: appContext.getExternalFilesDir(null)
                    ?: return
                File(dir, EXPORT_NAME).writeText(text, Charsets.UTF_8)
            }
        }
    }

    // ---------- نوشتن (سقف‌دار و sync) ----------

    private fun reportFile(): File =
        if (::appContext.isInitialized) File(appContext.filesDir, REPORT_FILE) else File(REPORT_FILE)

    /** الحاق با سقف حجم؛ در صورت سرریز، «انتهای» فایل (تازه‌ترین) حفظ می‌شود. */
    private fun append(file: File, text: String, maxBytes: Int) = synchronized(lock) {
        runCatching {
            file.appendText("${ts()} $text")
            if (file.length() > maxBytes) {
                val keep = file.readText().takeLast(maxBytes / 2)
                file.writeText(keep)
            }
        }
    }

    private fun ts(): String =
        SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
}
