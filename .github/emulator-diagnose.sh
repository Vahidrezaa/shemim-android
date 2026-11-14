#!/usr/bin/env bash
# ============================================================
# عیب‌یابی فریز اپ شمیم روی امولاتور واقعی اندروید — در GitHub Actions
# اجرا می‌شود (داخل reactivecircus/android-emulator-runner).
# خروجی: پوشهٔ diag/ (logcat، استک تردها، اسکرین‌شات، ویدیو، dumpsys ها)
# که به‌عنوان artifact آپلود می‌شود.
# هیچ قدمی نباید کل برداشت را بکشد → بدون set -e و همه‌جا || true
# ============================================================
set -uo pipefail

OUT="$GITHUB_WORKSPACE/diag"
mkdir -p "$OUT"
log() { echo "[diag] $(date +%H:%M:%S) $*"; }

# --- اطلاعات ریپو و دستگاه ---
git log --oneline -8 > "$OUT/repo-head.txt" 2>&1 || true
adb shell getprop ro.build.version.release > "$OUT/android-version.txt" 2>&1 || true
adb shell getprop ro.build.version.sdk >> "$OUT/android-version.txt" 2>&1 || true
adb shell wm size >> "$OUT/android-version.txt" 2>&1 || true

# --- نصب APK ---
log "installing APK"
adb install -r -g app/build/outputs/apk/debug/app-debug.apk > "$OUT/install.txt" 2>&1 || true
cat "$OUT/install.txt"

# --- پاک‌سازی لاگ و شروع ضبط ویدیو (۴۵ ثانیه — کل اجرا را می‌گیرد) ---
adb logcat -c 2>/dev/null || true
adb shell screenrecord --time-limit 45 /sdcard/diag-launch.mp4 > /dev/null 2>&1 &
RECORD_PID=$!

# --- اجرای اپ ---
log "launching app (am start -W)"
timeout 90 adb shell am start -W -n com.shemim.app/.MainActivity | tee "$OUT/am-start.txt" || true

# --- ۲۰ ثانیه صبر؛ بعد نمونه‌برداری ---
log "waiting 20s (freeze window)"
sleep 20
adb exec-out screencap -p > "$OUT/01-launch-20s.png" 2>/dev/null || true

PID=$(adb shell pidof com.shemim.app | tr -d '\r\n')
log "app pid = '$PID'"

# مصرف CPU اپ — فریزِ در حال اسپین = CPU بالا؛ بلاک کامل = صفر
adb shell dumpsys cpuinfo > "$OUT/cpuinfo.txt" 2>&1 || true
grep -i shemim "$OUT/cpuinfo.txt" || true

# استک کامل همهٔ تردها در همین لحظه (SIGQUIT → ART dump → logcat)
if [ -n "$PID" ]; then
  log "sending SIGQUIT for full thread dump"
  adb shell kill -3 "$PID" > /dev/null 2>&1 || true
  sleep 4
fi

# --- تست واکنش ورودی: تپ روی فیلد نام کاربری + تایپ ---
SIZE=$(adb shell wm size | grep -oE '[0-9]+x[0-9]+' | head -1 | tr -d '\r\n')
W=${SIZE%x*}; H=${SIZE#*x}
log "screen ${W}x${H} — tapping username field area"
adb shell input tap $((W / 2)) $((H * 40 / 100)) || true
sleep 2
adb shell input text "testuser" || true
sleep 2
adb exec-out screencap -p > "$OUT/02-after-tap-type.png" 2>/dev/null || true

# ساختار درختی واقعی UI از مسیر accessibility
adb shell uiautomator dump /sdcard/ui.xml > /dev/null 2>&1 || true
adb pull /sdcard/ui.xml "$OUT/ui-dump.xml" > /dev/null 2>&1 || true

# --- وضعیت پنجره‌ها (اسپلش گیرکرده اینجا معلوم می‌شود) و اکتیویتی ---
adb shell dumpsys window > "$OUT/dumpsys-window.txt" 2>&1 || true
adb shell dumpsys activity top > "$OUT/dumpsys-activity-top.txt" 2>&1 || true
adb shell dumpsys gfxinfo com.shemim.app > "$OUT/gfxinfo.txt" 2>&1 || true
adb shell ps -A > "$OUT/ps.txt" 2>&1 || true

# --- لاگ‌ها ---
log "collecting logcat"
adb logcat -d -v time > "$OUT/logcat.txt" 2>&1 || true
adb logcat -d -b crash -v time > "$OUT/logcat-crash.txt" 2>&1 || true

# --- فایل‌های داخلی اپ (گزارش عیب‌یابی خودِ اپ + ANR های سیستم) ---
log "adb root + pulling app diagnostics"
adb root > /dev/null 2>&1 || true
sleep 3
adb pull /data/data/com.shemim.app/files "$OUT/app-files" > /dev/null 2>&1 || true
adb pull /data/anr "$OUT/anr-traces" > /dev/null 2>&1 || true
adb unroot > /dev/null 2>&1 || true

# --- ویدیو (صبر برای بسته‌شدن فایل ۴۵ ثانیه‌ای) ---
log "waiting for screenrecord to finalize"
wait $RECORD_PID 2>/dev/null || true
adb pull /sdcard/diag-launch.mp4 "$OUT/diag-launch.mp4" > /dev/null 2>&1 || true

log "done. artifacts:"
ls -la "$OUT"
