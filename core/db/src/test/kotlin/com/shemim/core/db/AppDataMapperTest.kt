package com.shemim.core.db

import com.shemim.core.db.mapper.toSnapshot
import com.shemim.core.model.AppDataJson
import com.shemim.core.model.LoadDataResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * تست نگاشت JSON→Room — معیار پذیرش WP-0.4: همان فیکسچر JSON لایهٔ مدل،
 * سرتاسر تا موجودیت‌های Room.
 */
class AppDataMapperTest {

    private val responseJson = """
    {
      "success": true, "dataVersion": "v77",
      "appData": {
        "fundName": "صندوق خانوادگی",
        "mainAccount": 48300000, "incomeExpenseAccount": -2100000,
        "members": [
          {"id":"m-1","membershipNumber":1,"firstName":"مدیر","lastName":"صندوق",
           "username":"admin","role":"admin","isOwner":true,
           "fatherName":"احمد","nationalCode":"001","isSupervisor":null,
           "joinDate":"1400/01/01","email":"a@x.ir","colorTheme":"royal",
           "transactions":[],
           "loans":[]},
          {"id":"m-101","membershipNumber":101,"firstName":"زهرا","lastName":"محمدی",
           "username":"zahra","role":"member","isOwner":false,
           "joinDate":"1401/03/15","email":"","colorTheme":"emerald",
           "transactions":[
             {"id":"t-1","type":"deposit","amount":2500000,"description":"واریز",
              "date":"1403/05/12","isInstallment":false,"loanId":null},
             {"id":"t-2","type":"withdraw","amount":800000,"date":"1403/06/01",
              "isInstallment":true,"loanId":"l-9",
              "fromMonth":5,"fromYear":1403,"toMonth":7,"toYear":1403}
           ],
           "loans":[
             {"id":"l-9","loanNumber":9,"amount":12000000,"totalInstallments":12,
              "paidInstallments":2,"fee":300000,"startDate":"1403/04/01",
              "isEmergency":true,"splitMethod":2,
              "installmentsHistory":[
                {"installmentNumber":1,"amount":1050000,"date":"1403/05/01"},
                {"installmentNumber":2,"amount":1050000,"date":"1403/06/01"}
              ]}
           ]}
        ],
        "incomeExpenseTransactions": [
          {"id":"ie-1","type":"expense","amount":150000,"reason":"خرید لوازم",
           "description":null,"transaction_date":"1403/05/02","date":"1403/05/02",
           "created_by":"admin","createdBy":"admin"}
        ],
        "groups": [{"id":"g-1","leaderId":"m-1","memberIds":["m-1","m-101"]}],
        "resignedMembers": [
          {"id":"m-2","membershipNumber":7,"membership_number":7,"firstName":"علی",
           "first_name":"علی","lastName":"رنجبر","last_name":"رنجبر","username":"ali",
           "role":"resigned","joinDate":"1399/01/10","email":"","resignedDate":""}
        ],
        "settings": {"loanMultiplier":2.5,"loanMultiplierMode":"manual",
          "loanReservePercent":15,"currency":"toman","theme":"royal"},
        "profitReports": [
          {"id":"pr-1","version":2,"date":"1403/05/01","startDate":"1402/06/01",
           "endDate":"1403/05/01","description":"سود سال","totalProfit":9000000,
           "details":[{"balanceScore":3}]}
        ],
        "requests": [
          {"id":"r-1","type":"loan","memberId":"m-101","memberName":"زهرا محمدی",
           "amount":5000000,"request_date":"1403/05/20","status":"pending",
           "requested_by":"زهرا","from_month":6,"from_year":1403,"installments":10,
           "approved_by":"","approved_at":"","reject_reason":""}
        ],
        "requestsV2": [
          {"id":"rv-1","type":"installment_payment","member_id":"m-101",
           "member_name":"زهرا محمدی","amount":1050000,"request_date":"1403/06/10",
           "status":"approved","approved_by":"مدیر","approved_at":"1403/06/11"}
        ],
        "loanQueue": [
          {"id":"q-1","memberId":"m-101","memberName":"زهرا محمدی",
           "requestDate":"1403/06/15","status":"in_queue","priority":"high",
           "requestedAmount":6000000,"requestedInstallments":12,
           "adminEstimatedDate":"1403/09/01"}
        ],
        "notifications": [
          {"id":"n-1","title":"جلسه","message":"فردا","priority":"high",
           "date":"1403/06/20","target":"all","target_member_id":null,
           "replies":[{"memberId":"m-101","text":"حاضر هستم"}],
           "readBy":["m-1"],
           "files":[{"id":"f-1","name":"دعوت‌نامه.pdf","mime":"application/pdf","size":81234}]}
        ],
        "nextMemberNumber": 102, "nextLoanNumber": 10
      }
    }
    """.trimIndent()

    private fun snapshot() = AppDataJson
        .decodeFromString(LoadDataResponse.serializer(), responseJson)
        .appData!!
        .toSnapshot(dataVersion = "v77")

    @Test
    fun `json maps to full snapshot with counts`() {
        val snap = snapshot()

        assertEquals("v77", snap.dataVersion)
        assertEquals(2, snap.members.size)
        assertEquals(2, snap.transactions.size)
        assertEquals(1, snap.loans.size)
        assertEquals(2, snap.installments.size)
        assertEquals(1, snap.resignedMembers.size)
        assertEquals(1, snap.incomeExpenses.size)
        assertEquals(1, snap.groups.size)
        assertEquals(2, snap.groupMembers.size) // عضویت‌ها باز می‌شوند
        assertEquals(1, snap.profitReports.size)
        assertEquals(1, snap.requests.size)
        assertEquals(1, snap.requestsV2.size)
        assertEquals(1, snap.loanQueue.size)
        assertEquals(1, snap.notifications.size)
    }

    @Test
    fun `nested collections get owner ids stamped`() {
        val snap = snapshot()

        val tx = snap.transactions.first { it.id == "t-2" }
        assertEquals("m-101", tx.memberId)
        assertTrue(tx.isInstallment)
        assertEquals("l-9", tx.loanId)
        assertEquals(7, tx.toMonth)

        val loan = snap.loans.single()
        assertEquals("m-101", loan.memberId)
        assertTrue(loan.isEmergency)
        assertEquals(2, loan.splitMethod)

        val inst = snap.installments.first { it.installmentNumber == 2 }
        assertEquals("l-9", inst.loanId)
        assertEquals("1403/06/01", inst.paymentDate)
    }

    @Test
    fun `settings entity carries balances counters and theme fallback`() {
        val s = snapshot().settings!!

        assertEquals("صندوق خانوادگی", s.fundName)
        assertEquals(48300000L, s.mainAccount)
        assertEquals(-2100000L, s.incomeExpenseAccount)
        assertEquals(102, s.nextMemberNumber)
        assertEquals(10, s.nextLoanNumber)
        assertEquals(2.5, s.loanMultiplier!!, 0.0)
        assertEquals("manual", s.loanMultiplierMode)
        assertEquals(15, s.loanReservePercent)
        assertEquals("toman", s.currency)
        // theme از settings.theme می‌آید؛ غیبش بیفتد سراغ colorTheme قدیمی
        assertEquals("royal", s.theme)
        assertNull(s.fundLogo)
    }

    @Test
    fun `notification json blobs encode validly`() {
        val n = snapshot().notifications.single()

        assertEquals("high", n.priority)
        assertTrue(n.repliesJson.contains("حاضر هستم"))
        assertEquals("""["m-1"]""", n.readByJson)
        assertTrue(n.filesJson.contains("دعوت‌نامه.pdf"))
        assertTrue(n.filesJson.contains("81234"))
        // JSONهای خام باید قابل پارس برگشت باشند (روند خواندن در WP-1.5)
        assertTrue(n.repliesJson.startsWith("["))
    }

    @Test
    fun `mixed naming lands in shared entity columns`() {
        val snap = snapshot()

        val v1 = snap.requests.single()
        assertEquals("m-101", v1.memberId)        // camelCase در V1
        assertEquals("1403/05/20", v1.requestDate)
        assertEquals(6, v1.fromMonth)

        val v2 = snap.requestsV2.single()
        assertEquals("m-101", v2.memberId)        // snake_case در V2
        assertEquals("1403/06/11", v2.approvedAt)
    }

    @Test
    fun `group memberships expand to cross refs`() {
        val cross = snapshot().groupMembers

        assertTrue(cross.contains(com.shemim.core.db.entity.GroupMemberCrossRef("g-1", "m-1")))
        assertTrue(cross.contains(com.shemim.core.db.entity.GroupMemberCrossRef("g-1", "m-101")))
    }
}
