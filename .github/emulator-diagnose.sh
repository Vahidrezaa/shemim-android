#!/usr/bin/env bash
# ============================================================
# عیب‌یابی فریز اپ شمیم روی امولاتور واقعی اندروید — نسخهٔ ۲
# دو سناریو در هر سطح API:
#   s1-light     : حالت روشن + اجرای مستقیم (am start) — خط پایه
#   s2-dark-launcher: حالت تیره + زبان فارسی اپ + اجرای «تپ روی
#                     آیکون لانچر» (monkey LAUNCHER) — نزدیک‌ترین حالت
#                     به گوشی واقعی کاربر
# هر سناریو: ویدیو ۳۰ ثانیه، اسکرین‌شات، استک تردها (SIGQUIT)،
# dumpsys پنجره‌ها، logcat جدا. در پایان: تست ورودی + uiautomator
# dump + فایل‌های داخلی اپ (گزارش عیب‌یابی خودِ اپ) + ردهای ANR.
# بدون set -e و همه‌جا || true — خطای یک قدم نباید بقیه را بکشد.
# ============================================================
set -uo pipefail

OUT="$GITHUB_WORKSPACE/diag"
mkdir -p "$OUT"
log() { echo "[diag] $(date +%H:%M:%S) $*"; }

# --- اطلاعات ریپو و دستگاه ---
git log --oneline -8 > "$OUT/repo-head.txt" 2>&1 || true
{
  adb shell getprop ro.build.version.release
  adb shell getprop ro.build.version.sdk
  adb shell wm size
  adb shell wm density
} > "$OUT/android-version.txt" 2>&1 || true

# --- نصب APK ---
log "installing APK"
adb install -r -g app/build/outputs/apk/debug/app-debug.apk > "$OUT/install.txt" 2>&1 || true
cat "$OUT/install.txt"

# یک اجرا در هر سناریو: لاگ تمیز + ویدیو + اسکرین‌شات + استک تردها + پنجره‌ها
capture_scenario() {
  local tag="$1"; shift
  adb logcat -c 2>/dev/null || true
  adb shell screenrecord --time-limit 30 /sdcard/diag-$tag.mp4 > /dev/null 2>&1 &
  local rec=$!

  log "[$tag] launching: $*"
  timeout 60 "$@" > "$OUT/$tag-launch.txt" 2>&1 || true
  cat "$OUT/$tag-launch.txt" || true

  sleep 18
  adb exec-out screencap -p > "$OUT/$tag-screen.png" 2>/dev/null || true

  local pid
  pid=$(adb shell pidof com.shemim.app | tr -d '\r\n')
  adb shell dumpsys cpuinfo > "$OUT/$tag-cpuinfo.txt" 2>&1 || true
  grep -i shemim "$OUT/$tag-cpuinfo.txt" || true

  if [ -n "$pid" ]; then
    log "[$tag] pid=$pid — SIGQUIT for full thread dump"
    adb shell kill -3 "$pid" > /dev/null 2>&1 || true
    sleep 3
  fi

  adb shell dumpsys window > "$OUT/$tag-window.txt" 2>&1 || true
  adb logcat -d -v time > "$OUT/$tag-logcat.txt" 2>&1 || true

  wait $rec 2>/dev/null || true
  adb pull /sdcard/diag-$tag.mp4 "$OUT/$tag-launch.mp4" > /dev/null 2>&1 || true
  log "[$tag] done (pid=$pid)"
}

# ---------- سناریو ۱: حالت روشن + am start ----------
capture_scenario s1-light adb shell am start -W -n com.shemim.app/.MainActivity

# ---------- سناریو ۲: تیره + فارسی + اجرا از لانچر ----------
adb shell am force-stop com.shemim.app > /dev/null 2>&1 || true
adb shell cmd uimode night yes > /dev/null 2>&1 || true
# زبان اپ فارسی (API 33+؛ در نسخه‌های قدیمی‌تر بی‌اثر و بی‌خطا)
adb shell cmd locale set-app-locales com.shemim.app --locales fa-IR > /dev/null 2>&1 || true
sleep 3
capture_scenario s2-dark-launcher adb shell monkey -p com.shemim.app -c android.intent.category.LAUNCHER 1

# ---------- تست ورودی روی آخرین حالت ----------
log "input test: tap + type"
SIZE=$(adb shell wm size | grep -oE '[0-9]+x[0-9]+' | head -1 | tr -d '\r\n')
W=${SIZE%x*}; H=${SIZE#*x}
log "screen ${W}x${H} — tapping username field area"
adb shell input tap $((W / 2)) $((H * 40 / 100)) || true
sleep 2
adb shell input text "testuser" || true
sleep 2
adb exec-out screencap -p > "$OUT/03-after-tap-type.png" 2>/dev/null || true
adb shell uiautomator dump /sdcard/ui.xml > /dev/null 2>&1 || true
adb pull /sdcard/ui.xml "$OUT/ui-dump.xml" > /dev/null 2>&1 || true

# ---------- اطلاعات نهایی فرایند ----------
adb shell dumpsys gfxinfo com.shemim.app > "$OUT/gfxinfo.txt" 2>&1 || true
adb shell ps -A > "$OUT/ps.txt" 2>&1 || true
adb logcat -d -b crash -v time > "$OUT/logcat-crash.txt" 2>&1 || true

# ---------- فایل‌های داخلی اپ + ردهای ANR ----------
log "adb root + pulling app diagnostics"
adb root > /dev/null 2>&1 || true
sleep 3
adb pull /data/data/com.shemim.app/files "$OUT/app-files" > /dev/null 2>&1 || true
adb pull /data/anr "$OUT/anr-traces" > /dev/null 2>&1 || true
adb unroot > /dev/null 2>&1 || true

log "done. artifacts:"
ls -la "$OUT"
