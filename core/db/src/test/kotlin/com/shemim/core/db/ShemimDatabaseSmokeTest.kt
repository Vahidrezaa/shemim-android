package com.shemim.core.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shemim.core.db.mapper.toSnapshot
import com.shemim.core.model.AppDataJson
import com.shemim.core.model.LoadDataResponse
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * تست دود دیتابیس — بازکردن ShemimDatabase درون‌حافظه‌ای، ریختن یک snapshot
 * کامل و خواندنش از طریق مشاهده‌گرهای Flow (بدون گوشی — Robolectric).
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class ShemimDatabaseSmokeTest {

    private lateinit var db: ShemimDatabase

    private val responseJson = """
    {
      "success": true, "dataVersion": "smoke-1",
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
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ShemimDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `snapshot roundtrips through database`() = runBlocking {
        val snap = AppDataJson
            .decodeFromString(LoadDataResponse.serializer(), responseJson)
            .appData!!
            .toSnapshot("smoke-1")

        db.memberDao().replaceAll(
            snap.members, snap.transactions, snap.loans,
            snap.installments, snap.resignedMembers,
        )
        db.fundDao().replaceAll(
            snap.incomeExpenses, snap.groups, snap.groupMembers,
            snap.settings, snap.profitReports,
        )
        db.workflowDao().replaceAll(
            snap.requests, snap.requestsV2, snap.loanQueue, snap.notifications,
        )
        db.syncMetaDao().put(
            com.shemim.core.db.entity.SyncMetaEntity(dataVersion = "smoke-1", syncedAtMillis = 42L),
        )

        // خواندن از هر چهار DAO
        assertEquals(1, db.memberDao().countMembers())
        assertEquals(1, db.memberDao().countTransactions())
        assertEquals(1, db.fundDao().countGroups())
        assertEquals(1, db.workflowDao().countNotifications())

        val members = db.memberDao().observeMembers().first()
        assertEquals("آ", members.single().firstName)

        val tx = db.memberDao().observeTransactionsOf("m-1").first()
        assertEquals(1000L, tx.single().amount)

        val inst = db.memberDao().observeInstallmentsOf("l-1").first()
        assertEquals(250L, inst.single().amount)

        val settings = db.fundDao().observeSettings().first()
        assertNotNull(settings)
        assertEquals("صندوق تست", settings!!.fundName)
        assertEquals("toman", settings.currency)

        val notif = db.workflowDao().observeNotifications().first()
        assertEquals("عنوان", notif.single().title)

        val meta = db.syncMetaDao().get()
        assertEquals("smoke-1", meta?.dataVersion)
        assertEquals("smoke-1", db.syncMetaDao().currentVersion())

        // جایگزینی دوم (sync بعدی) — شمارنده‌ها نباید دوبل شوند
        db.memberDao().replaceAll(snap.members, snap.transactions, snap.loans, snap.installments, emptyList())
        db.fundDao().replaceAll(emptyList(), snap.groups, emptyList(), snap.settings, emptyList())
        db.workflowDao().replaceAll(emptyList(), emptyList(), emptyList(), emptyList())

        assertEquals(1, db.memberDao().countMembers())
        assertEquals(1, db.memberDao().countTransactions())
        assertEquals(0, db.workflowDao().countNotifications())
        assertEquals("smoke-1", db.syncMetaDao().currentVersion())
    }
}
