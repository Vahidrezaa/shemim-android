package com.shemim.core.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shemim.core.db.ShemimDatabase
import com.shemim.core.db.entity.SyncMetaEntity
import com.shemim.core.network.CookieStore
import com.shemim.core.network.PersistentCookieJar
import com.shemim.core.network.ShemimApiClient
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * تست موتور همگام‌سازی با سرور جعلی HTTP + Room درون‌حافظه‌ای — WP-0.5.
 *
 * رفتارهای ضبط‌شده از `api/core.php` (سند ۰۱ §۳):
 *  - اولین sync: `{success, appData, dataVersion}` → نگاشت کامل جداول + ذخیرهٔ نسخه
 *  - sync بعدی: بدنه حامل `sinceVersion` است و `unchanged:true` → بدون نگاشت
 *  - sync دستی (force): بدون sinceVersion → همیشه دادهٔ کامل
 *  - قطع سرور → Offline؛ کش قبلی دست‌نخورده می‌ماند
 *  - `auth_required` → AuthRequired (نشست باطل)
 *  - نرخ‌حد محرک خودکار: maybeRefresh با فاصلهٔ کم کاری نمی‌کند
 *
 * (نام متدها به دلیل محدودیت شناسهٔ JVM روی نویسه‌های فارسی، ASCII است.)
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class SyncEngineTest {

    /** کش کوکی درون‌حافظه‌ای — Keystore واقعی در تست واحد در دسترس نیست. */
    private class FakeCookieStore : CookieStore {
        var json: String? = null
        override fun load(): String? = json
        override fun save(json: String) { this.json = json }
        override fun clear() { json = null }
    }

    private lateinit var server: MockWebServer
    private lateinit var db: ShemimDatabase
    private lateinit var engine: SyncEngine
    private lateinit var scope: CoroutineScope

    /** همان فیکسچر تست دود core:db — پاسخ کامل v1 با یک عضو/تراکنش/وام/قسط/گروه/اطلاعیه. */
    private val fullDataV1 = """
    {
      "success": true, "dataVersion": "v1",
      "appData": {
        "fundName": "صندوق تست",
        "mainAccount": 1000, "incomeExpenseAccount": -200,
        "members": [
          {"id":"m-1","membershipNumber":1,"firstName":"آ","lastName":"ب",
           "username":"a","role":"admin","isOwner":true,"email":"",
           "transactions":[{"id":"t-1","type":"deposit","amount":1000,"date":"1403/01/01"}],
           "loans":[{"id":"l-1","loanNumber":1,"amount":500,"totalInstallments":2,
                     "paidInstallments":1,"fee":0,"startDate":"1403/01/01",
                     "installmentsHistory":[{"installmentNumber":1,"amount":250,"date":"1403/02/01"}]}]}
        ],
        "incomeExpenseTransactions": [],
        "groups": [{"id":"g-1","leaderId":"m-1","memberIds":["m-1"]}],
        "resignedMembers": [],
        "settings": {"currency":"toman"},
        "profitReports": [],
        "requests": [], "requestsV2": [], "loanQueue": [],
        "notifications": [{"id":"n-1","title":"عنوان","message":"متن","date":"1403/01/01"}],
        "nextMemberNumber": 2, "nextLoanNumber": 2
      }
    }
    """.trimIndent()

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ShemimDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        scope = CoroutineScope(SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)
        val client = ShemimApiClient(
            baseUrl = server.url("/api.php").toString(),
            cookieJar = PersistentCookieJar(FakeCookieStore()),
        )
        engine = SyncEngine(api = client, db = db, scope = scope)
    }

    @After
    fun tearDown() {
        scope.cancel()
        db.close()
        server.shutdown()
    }

    private fun enqueueJson(json: String) {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(json),
        )
    }

    /** تا وقتی collector وضعیت، Room را دیده باشد صبر می‌کند (به‌روزرسانی UI ناهمگام است). */
    private fun awaitState(predicate: (SyncUiState) -> Boolean, timeoutMs: Long = 5_000): SyncUiState {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val s = engine.state.value
            if (predicate(s)) return s
            Thread.sleep(20)
        }
        return engine.state.value
    }

    @Test
    fun `first full sync maps all tables and stores version`() = runBlocking {
        enqueueJson(fullDataV1)

        val result = engine.refresh("app-start")

        assertTrue("نتیجه باید Synced باشد: $result", result is SyncResult.Synced)
        assertEquals("v1", (result as SyncResult.Synced).dataVersion)

        // درخواست اولیه بدون sinceVersion است (کش خالی)
        val request = server.takeRequest(2, TimeUnit.SECONDS)!!
        assertEquals("/api.php?action=loadData", request.path)
        assertEquals("{}", request.body.readUtf8())

        // همهٔ جداول از طریق Room خوانده می‌شوند
        assertEquals(1, db.memberDao().countMembers())
        assertEquals(1, db.memberDao().countTransactions())
        assertEquals(1, db.fundDao().countGroups())
        assertEquals(1, db.workflowDao().countNotifications())
        assertEquals("صندوق تست", db.fundDao().observeSettings().first()!!.fundName)

        // متا هم ذخیره شد و از طریق observeMeta (منبع حقیقت UI) دیده می‌شود
        assertEquals("v1", db.syncMetaDao().currentVersion())
        val s = awaitState { it.dataVersion == "v1" && it.lastSyncAtMillis != null }
        assertNotNull(s.lastSyncAtMillis)
        assertEquals(false, s.running)
    }

    @Test
    fun `second sync sends sinceVersion and skips mapping when unchanged`() = runBlocking {
        enqueueJson(fullDataV1)
        engine.refresh("first")

        // پاسخ unchanged دقیقاً مثل api/core.php برای نسخهٔ برابر
        enqueueJson("""{"success":true,"unchanged":true,"dataVersion":"v1"}""")

        val result = engine.refresh("resume")

        assertTrue("نتیجه باید Unchanged باشد: $result", result is SyncResult.Unchanged)

        // بدنه باید sinceVersion نسخهٔ محلی را حمل کند — بهینه‌سازی اینترنت سرور
        server.takeRequest(2, TimeUnit.SECONDS) // اولین (کامل)
        val second = server.takeRequest(2, TimeUnit.SECONDS)!!
        assertEquals("""{"sinceVersion":"v1"}""", second.body.readUtf8())

        // داده دست‌نخورده ماند و مُهر زمان نو شد
        assertEquals(1, db.memberDao().countMembers())
        assertEquals("v1", db.syncMetaDao().currentVersion())
        val meta = db.syncMetaDao().get()
        assertTrue(meta is SyncMetaEntity && meta.syncedAtMillis > 0)
    }

    @Test
    fun `force sync omits sinceVersion and applies fresh data`() = runBlocking {
        enqueueJson(fullDataV1)
        engine.refresh("first")

        val freshV2 = fullDataV1
            .replace("\"dataVersion\": \"v1\"", "\"dataVersion\": \"v2\"")
            .replace("صندوق تست", "صندوق تست ۲")
        enqueueJson(freshV2)

        val result = engine.refresh("manual", force = true)

        assertTrue("نتیجه باید Synced(v2) باشد: $result", result is SyncResult.Synced)
        assertEquals("v2", (result as SyncResult.Synced).dataVersion)

        server.takeRequest(2, TimeUnit.SECONDS) // اولی
        val forced = server.takeRequest(2, TimeUnit.SECONDS)!!
        // force مثل دکمهٔ دستی وب: بدون sinceVersion → همیشه کامل
        assertEquals("{}", forced.body.readUtf8())

        assertEquals("صندوق تست ۲", db.fundDao().observeSettings().first()!!.fundName)
        assertEquals("v2", db.syncMetaDao().currentVersion())
        awaitState { it.dataVersion == "v2" }
    }

    @Test
    fun `server down reports offline and keeps cache`() = runBlocking {
        enqueueJson(fullDataV1)
        engine.refresh("first")
        assertEquals(1, db.memberDao().countMembers())

        server.shutdown()

        val result = engine.refresh("manual", force = true)

        assertTrue("نتیجه باید Offline باشد: $result", result is SyncResult.Offline)
        assertNotNull("باید زمان sync قبلی را حفظ کند", (result as SyncResult.Offline).lastSyncAtMillis)

        // کش قبلی دست‌نخورده و UI همچنان دادهٔ قدیمی را نشان می‌دهد
        assertEquals(1, db.memberDao().countMembers())
        assertEquals("v1", db.syncMetaDao().currentVersion())
        val s = awaitState { it.offline }
        assertTrue(s.offline)
        assertEquals(false, s.running)
    }

    @Test
    fun `auth_required surfaces as AuthRequired`() = runBlocking {
        enqueueJson("""{"success":false,"error":"auth_required","message":"ابتدا باید وارد حساب کاربری خود شوید."}""")

        val result = engine.refresh("app-start")

        assertTrue("نتیجه باید AuthRequired باشد: $result", result is SyncResult.AuthRequired)
    }

    @Test
    fun `maybeRefresh respects minimum interval`() = runBlocking {
        enqueueJson(fullDataV1)
        engine.refresh("app-start")
        assertEquals(1, server.requestCount)

        // بلافاصله بعد: نرخ‌حد جلوی تلاش دوم را می‌گیرد (محرک خودکار)
        val skipped = engine.maybeRefresh("on-resume", minIntervalMs = 3_600_000)
        assertNull("باید رد شود", skipped)
        assertEquals("هیچ درخواستی نباید رفته باشد", 1, server.requestCount)

        // با فاصلهٔ صفر: تلاش انجام می‌شود
        enqueueJson("""{"success":true,"unchanged":true,"dataVersion":"v1"}""")
        val done = engine.maybeRefresh("on-resume", minIntervalMs = 0)
        assertTrue("باید Unchanged باشد: $done", done is SyncResult.Unchanged)
        assertEquals(2, server.requestCount)
    }
}
