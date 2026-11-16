package com.shemim.app.ui

/**
 * مدل ناوبری نقش‌محور — WP-0.7.
 *
 * پورتِ وفادارِ `menusByRole` از js/members.js وب: عنوان‌ها، توضیح‌ها،
 * ایموجی‌ها و ترتیب آیتم‌ها دقیقاً همان منوی سایت هر نقش است؛ فقط فیلد
 * `phase` (فازی که این بخش در سند ۰۴ برنامهٔ اجرایش است) به هر آیتم
 * افزوده شده تا placeholder صادقانه بگوید کی فعال می‌شود.
 *
 * نکتهٔ ناظر (overlay): وب برای ناظر، کاشی‌های خواندنیِ مدیر را به منوی
 * نقش خودش ملحق می‌کند (`_getMenuItemsRaw`). آن منطق به دادهٔ دسترسی
 * (`PAGE_PERM_KEY`/`hasPerm` در appData اعضا) وابسته است که مصرفش در
 * فاز ۲ است؛ تا آن موقع ناظر منوی نقش اصلی خودش را می‌بیند (ثبت در سند ۰۴).
 */

/** یک بخش اپ — معادل یک کاشی/آیتم منوی وب. */
data class AppSection(
    /** شناسهٔ صفحهٔ وب — همان `page` در منوی سایت (کلید پایدار برای ناوبری). */
    val page: String,
    val title: String,
    val desc: String,
    val emoji: String,
    /** فازی که این بخش در آن فعال می‌شود (سند ۰۴). */
    val phase: Int,
)

object AppNav {

    // ---------- تب‌های نوار پایین — گزینش از منوی خود نقش ----------
    // چهار بخش پرکاربردِ روزمرهٔ هر نقش + دکمهٔ «بیشتر» (کل منو).

    fun bottomTabs(role: String): List<AppSection> = when (normalize(role)) {
        Role.ADMIN -> listOf(adminDashboard, members, adminTransactions, loans)
        Role.LEADER -> listOf(myDashboard, myMembers, leaderTransactions, leaderLoans)
        else -> listOf(myDashboard, userMyMembers, userTransactions, userLoans)
    }

    /** بخش‌های اصلی (کاشی‌های منوی اصلی وب) — محتوای صفحهٔ «بیشتر». */
    fun mainSections(role: String): List<AppSection> = when (normalize(role)) {
        Role.ADMIN -> adminMain
        Role.LEADER -> leaderMain
        else -> userMain
    }

    /** منوی کناری وب — «سایر» در صفحهٔ «بیشتر». */
    fun sideSections(role: String): List<AppSection> = when (normalize(role)) {
        Role.ADMIN -> adminSide
        Role.LEADER -> leaderSide
        else -> userSide
    }

    /** صفحات‌ای که به‌جای placeholder، پیشخوان فاز ۰ را نشان می‌دهند. */
    fun isDashboardPage(page: String): Boolean = page == "dashboard" || page == "myDashboard"

    private fun normalize(role: String): Role = when (role) {
        "admin" -> Role.ADMIN
        "groupLeader" -> Role.LEADER
        else -> Role.USER
    }

    private enum class Role { ADMIN, LEADER, USER }

    // ---------- پیشخوان‌ها ----------
    private val adminDashboard = AppSection("dashboard", "پیشخوان", "خلاصه وضعیت", "📊", 2)
    private val myDashboard = AppSection("myDashboard", "پیشخوان من", "وضعیت کلی", "📊", 1)

    // ---------- آیتم‌های مشترک بین نقش‌ها (عنوان/توضیح هر نقش همان وب) ----------
    private val members = AppSection("members", "اعضا", "لیست همه اعضا", "👥", 2)
    private val myMembers = AppSection("myMembers", "اعضای من", "زیرمجموعه‌ها", "👥", 2)
    private val userMyMembers = AppSection("myMembers", "اعضای من", "حساب من", "👥", 1)
    private val leaderMyMembers = AppSection("myMembers", "اعضای من", "مدیریت اعضا", "👥", 2)
    private val adminTransactions = AppSection("transactions", "تراکنش‌ها", "همه تراکنش‌ها", "💰", 2)
    private val leaderTransactions = AppSection("transactions", "تراکنش‌ها", "تراکنش‌های اعضا", "💰", 2)
    private val userTransactions = AppSection("transactions", "تراکنش‌های من", "تاریخچه مالی", "💰", 1)
    private val loans = AppSection("loans", "وام‌ها", "مدیریت وام‌ها", "💳", 2)
    private val leaderLoans = AppSection("loans", "وام‌ها", "وام‌های اعضا", "💳", 2)
    private val userLoans = AppSection("loans", "وام‌های من", "وام‌های شخصی", "💳", 1)
    private val userRequests = AppSection("requests", "درخواست‌های من", "درخواست‌های شخصی", "📋", 1)
    private val leaderRequests = AppSection("requests", "درخواست‌ها", "درخواست‌های زیرمجموعه", "📋", 2)
    private val adminRequests = AppSection("requests", "درخواست‌ها", "مدیریت درخواست‌ها", "📋", 2)
    private val userInstallments = AppSection("installments", "اقساط من", "قسط‌های شخصی", "📅", 1)
    private val leaderInstallments = AppSection("installments", "اقساط", "قسط‌های اعضا", "📅", 2)
    private val adminInstallments = AppSection("installments", "اقساط", "لیست اقساط", "📅", 2)
    private val rotatingFund = AppSection("rotatingFund", "وام صندوق گردشی", "صندوق قرض‌الحسنه گردشی", "🔃", 3)
    private val loanQueue = AppSection("loanQueue", "نوبت وام", "وضعیت نوبت", "🎯", 2)
    private val userLoanQueue = AppSection("loanQueue", "نوبت وام من", "وضعیت نوبت", "🎯", 2)
    private val analytics = AppSection("analytics", "گزارشات تحلیلی", "نمودار و آمار پیشرفته", "📊", 5)
    private val calendar = AppSection("calendar", "تقویم", "تقویم وظایف صندوق", "🗓️", 1)
    private val userCalendar = AppSection("calendar", "تقویم من", "تقویم وظایف شخصی", "🗓️", 1)
    private val leaderCalendar = AppSection("calendar", "تقویم", "تقویم وظایف زیرمجموعه", "🗓️", 1)
    private val investFunds = AppSection("investFunds", "صندوق سرمایه‌گذاری", "سهام، سود و سررسید", "📈", 4)
    private val loanCalc = AppSection("loanCalc", "ماشین حساب وام", "محاسبه وام و اقساط", "🧮", 2)
    private val notifications = AppSection("notifications", "اطلاعیه‌ها", "مشاهده اطلاعیه‌ها", "📢", 1)
    private val adminNotifications = AppSection("notifications", "اطلاعیه‌ها", "مدیریت اطلاعیه‌ها", "📢", 2)
    private val settings = AppSection("settings", "تنظیمات", "تنظیمات شخصی", "⚙️", 1)
    private val adminSettings = AppSection("settings", "تنظیمات", "تنظیمات سیستم", "⚙️", 2)

    // ---------- منوی مدیر — ۲۴ کاشی اصلی + ۵ کناری (همان وب) ----------
    private val adminMain = listOf(
        adminDashboard,
        adminRequests,
        members,
        myMembers,
        AppSection("groupManagement", "مدیریت گروه‌ها", "تنظیم گروه‌ها", "🏢", 2),
        AppSection("resignedMembers", "اعضای انصرافی", "لیست انصراف‌ها", "🚫", 2),
        adminTransactions,
        AppSection("deposit", "واریز", "ثبت واریز جدید", "📥", 2),
        AppSection("withdraw", "برداشت", "ثبت برداشت جدید", "📤", 2),
        loans,
        AppSection("ownerPower", "پاداش مدیران", "قدرت وام مدیر کل و ناظران", "👑", 4),
        rotatingFund,
        loanQueue,
        adminInstallments,
        AppSection("incomeExpense", "درآمد/هزینه", "گزارش مالی", "📈", 2),
        AppSection("transfer", "انتقال وجه", "انتقال بین حساب‌ها", "🔄", 2),
        AppSection("profitCalcV2", "توزیع سود", "محاسبه و توزیع سود", "🧮", 2),
        analytics,
        calendar,
        investFunds,
        loanCalc,
        AppSection("roadmap", "نقشه راه و پیش‌بینی", "همراه مدیر: ۱۲ ماه آینده", "🧭", 4),
        AppSection("dueReport", "گزارش مطالبات", "معوقات و پیش‌بینی ماه بعد", "📑", 4),
    )

    private val adminSide = listOf(
        adminSettings,
        AppSection("backup", "پشتیبان", "پشتیبان‌گیری", "💾", 5),
        AppSection("loginReports", "فعالیت کاربران", "آخرین ورود و وضعیت آنلاین", "🕒", 2),
        adminNotifications,
        analytics,
    )

    // ---------- منوی سرگروه — ۱۲ کاشی اصلی + ۳ کناری ----------
    private val leaderMain = listOf(
        myDashboard,
        leaderRequests,
        leaderMyMembers,
        leaderTransactions,
        leaderLoans,
        rotatingFund,
        loanQueue,
        leaderInstallments,
        analytics,
        leaderCalendar,
        investFunds,
        loanCalc,
    )

    private val leaderSide = listOf(settings, notifications, analytics)

    // ---------- منوی عضو عادی — ۱۳ کاشی اصلی + ۳ کناری ----------
    private val userMain = listOf(
        myDashboard,
        AppSection("myProfile", "پروفایل من", "مشخصات و حساب", "👤", 1),
        userMyMembers,
        userRequests,
        userTransactions,
        userLoans,
        rotatingFund,
        userLoanQueue,
        userInstallments,
        analytics,
        userCalendar,
        investFunds,
        loanCalc,
    )

    private val userSide = listOf(settings, notifications, analytics)
}
