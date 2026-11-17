package com.shemim.core.sync

import androidx.room.withTransaction
import com.shemim.core.db.ShemimDatabase
import com.shemim.core.db.entity.SyncMetaEntity
import com.shemim.core.db.mapper.toSnapshot
import com.shemim.core.model.AppDataJson
import com.shemim.core.model.LoadDataResponse
import com.shemim.core.network.ApiResult
import com.shemim.core.network.ShemimApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * نتیجهٔ یک همگام‌سازی — WP-0.5 (سند ۰۲ §۴).
 *
 * تفکیک صریح حالت‌ها تا UI بتواند «آفلاین/کش قدیمی» را از «خطای سرور» و
 * «نشست باطل» جدا کند؛ `lastSyncAtMillis` در حالت‌های ناموفق یعنی اینکه
 * هنوز کش قدیمی معتبری از sync قبلی در Room هست (stale-while-revalidate).
 */
sealed interface SyncResult {
    /** appData کامل رسید و کل کش در یک تراکنش جایگزین شد. */
    data class Synced(val dataVersion: String, val atMillis: Long) : SyncResult

    /** سرور همان نسخهٔ محلی را دارد (`unchanged:true`) — هیچ نگاشتی لازم نبود. */
    data class Unchanged(val dataVersion: String, val atMillis: Long) : SyncResult

    /** شبکه قطع/تایم‌اوت — کش موجود است و همچنان قابل نمایش. */
    data class Offline(val lastSyncAtMillis: Long?) : SyncResult

    /** نشست از سمت سرور باطل است — اپ باید به صفحهٔ ورود برگردد (authEvents). */
    data class AuthRequired(val lastSyncAtMillis: Long?) : SyncResult

    /** خطای منطقی/پارس — جزئیات برای نمایش یا لاگ. */
    data class Failed(val detail: String, val lastSyncAtMillis: Long?) : SyncResult
}

/** وضعیت همگام‌سازی برای UI — همیشه از Room (منبع حقیقت) + رویدادهای همین کلاس. */
data class SyncUiState(
    val running: Boolean = false,
    val lastSyncAtMillis: Long? = null,
    val dataVersion: String? = null,
    val offline: Boolean = false,
    val error: String? = null,
)

/**
 * موتور همگام‌سازی — WP-0.5.
 *
 * قرارداد ۱:۱ با `loadFromServer` وب (auth-core.js):
 *  1) `POST ?action=loadData` با `sinceVersion` نسخهٔ محلی (یا بدون آن برای کامل)
 *  2) `unchanged:true` → فقط مُهر زمان نو می‌شود (بهینه‌سازی بزرگ اینترنتِ خود سرور)
 *  3) appData کامل → نگاشت به Room و جایگزینی تمام جداول سینک‌شونده در «یک» تراکنش
 *  4) `dataVersion` + زمان sync در `sync_meta` ذخیره می‌شود
 *
 * UI هرگز از نتیجهٔ refresh نمی‌سازد؛ همیشه `state` را جمع می‌کند که خودش از
 * `observeMeta()` (Room) تغذیه می‌شود — پس هر داده‌دهندهٔ دیگری که جدول‌ها را
 * عوض کند هم بلافاصله در UI می‌نشیند.
 *
 * همگام‌سازی‌ها همیشه به‌صورت پشت‌سرهم اجرا می‌شوند (Mutex): محرک‌های مختلف
 * (شروع اپ، بازگشت به اپ، دکمهٔ دستی) هیچ‌وقت دو loadData موازی نمی‌سازند.
 */
class SyncEngine(
    private val api: ShemimApiClient,
    private val db: ShemimDatabase,
    scope: CoroutineScope,
) {

    private val _state = MutableStateFlow(SyncUiState())
    val state: StateFlow<SyncUiState> = _state.asStateFlow()

    /** اجرای پشت‌سرهم (single-flight) همهٔ refreshها. */
    private val gate = Mutex()

    /** زمان آخرین «تلاش» (نه لزوماً موفق) — برای نرخ‌حدِ محرک‌های خودکار. */
    @Volatile
    private var lastAttemptAtMillis: Long = 0L

    init {
        // آخرین وضعیت کش (نسخه + زمان sync) همیشه از خود Room می‌آید؛
        // این جمع‌کننده تا عمر scope زنده است.
        scope.launch {
            db.syncMetaDao().observeMeta().collect { meta ->
                _state.update {
                    it.copy(
                        lastSyncAtMillis = meta?.syncedAtMillis,
                        dataVersion = meta?.dataVersion,
                    )
                }
            }
        }
    }

    /**
     * محرک خودکار با نرخ‌حد (شروع اپ / بازگشت به اپ): اگر از آخرین تلاش
     * کمتر از `minIntervalMs` گذشته باشد کاری نمی‌کند و null برمی‌گرداند.
     */
    suspend fun maybeRefresh(reason: String, minIntervalMs: Long): SyncResult? {
        val last = lastAttemptAtMillis
        val now = System.currentTimeMillis()
        if (last != 0L && now - last < minIntervalMs) return null
        return refresh(reason)
    }

    /**
     * همگام‌سازی صریح. `force=true` مثل دکمهٔ دستیِ وب (خط ۳۳۳ auth-core.js)
     * `sinceVersion` نمی‌فرستد و همیشه نسخهٔ کامل و تازه می‌گیرد.
     */
    suspend fun refresh(reason: String = "manual", force: Boolean = false): SyncResult = gate.withLock {
        lastAttemptAtMillis = System.currentTimeMillis()
        _state.update { it.copy(running = true, error = null, offline = false) }
        val result = doRefresh(force)
        _state.update {
            it.copy(
                running = false,
                offline = result is SyncResult.Offline,
                error = (result as? SyncResult.Failed)?.detail,
            )
        }
        result
    }

    private suspend fun doRefresh(force: Boolean): SyncResult = withContext(Dispatchers.IO) {
        val lastSynced = _state.value.lastSyncAtMillis
        val since = if (force) "" else db.syncMetaDao().currentVersion().orEmpty()
        val body: JsonObject = if (since.isEmpty()) {
            JsonObject(emptyMap())
        } else {
            buildJsonObject { put("sinceVersion", since) }
        }

        when (val r = api.call("loadData", body)) {
            is ApiResult.AuthRequired -> SyncResult.AuthRequired(lastSynced)

            is ApiResult.NetworkError -> SyncResult.Offline(lastSynced)

            is ApiResult.MalformedResponse -> SyncResult.Failed(r.detail, lastSynced)

            is ApiResult.ServerError -> SyncResult.Failed(r.error ?: r.message ?: "خطای سرور", lastSynced)

            is ApiResult.Ok -> {
                val resp = try {
                    AppDataJson.decodeFromJsonElement(LoadDataResponse.serializer(), r.body)
                } catch (e: SerializationException) {
                    return@withContext SyncResult.Failed("پاسخ loadData قابل پارس نبود: ${e.message?.take(80)}", lastSynced)
                }
                if (!resp.success) {
                    return@withContext SyncResult.Failed("پاسخ loadData ناموفق بود", lastSynced)
                }
                // گرفتن در متغیر محلی: پراپرتیِ ماژول دیگر (core:model) قابل
                // smart cast نیست — الگوی استاندارد null-check روی نسخهٔ محلی.
                val fresh = resp.appData
                if (resp.unchanged || fresh == null) {
                    // همان بهینه‌سازی وب: نسخه‌ها برابرند؛ فقط مُهر زمان نو شود
                    // تا «آخرین به‌روزرسانی» صادقانه رفرش لمس‌شده را نشان دهد.
                    touchMeta(resp.dataVersion.ifEmpty { since }, lastSynced)
                    SyncResult.Unchanged(resp.dataVersion.ifEmpty { since }, System.currentTimeMillis())
                } else {
                    applySnapshot(fresh.toSnapshot(resp.dataVersion))
                    SyncResult.Synced(resp.dataVersion, System.currentTimeMillis())
                }
            }
        }
    }

    /** جایگزینی اتمیک کل کش — همهٔ جداول + متا در «یک» تراکنش Room. */
    private suspend fun applySnapshot(snapshot: com.shemim.core.db.mapper.AppDataSnapshot) {
        db.withTransaction {
            db.memberDao().replaceAll(
                snapshot.members,
                snapshot.transactions,
                snapshot.loans,
                snapshot.installments,
                snapshot.resignedMembers,
            )
            db.fundDao().replaceAll(
                snapshot.incomeExpenses,
                snapshot.groups,
                snapshot.groupMembers,
                snapshot.settings,
                snapshot.profitReports,
            )
            db.workflowDao().replaceAll(
                snapshot.requests,
                snapshot.requestsV2,
                snapshot.loanQueue,
                snapshot.notifications,
            )
            db.syncMetaDao().put(
                SyncMetaEntity(
                    dataVersion = snapshot.dataVersion,
                    syncedAtMillis = System.currentTimeMillis(),
                ),
            )
        }
    }

    /** نوکردن مُهر زمان بدون نگاشت (حالت unchanged) — نسخهٔ موجود حفظ می‌شود. */
    private suspend fun touchMeta(dataVersion: String, lastSynced: Long?) {
        if (dataVersion.isEmpty()) {
            if (lastSynced == null) return
            // نسخه‌ای نداریم و سرور هم نگفت؛ فقط اگر قبلاً رکوردی هست مُهر نو شود
            db.syncMetaDao().get()?.let {
                db.syncMetaDao().put(it.copy(syncedAtMillis = System.currentTimeMillis()))
            }
            return
        }
        db.syncMetaDao().put(
            SyncMetaEntity(dataVersion = dataVersion, syncedAtMillis = System.currentTimeMillis()),
        )
    }
}
