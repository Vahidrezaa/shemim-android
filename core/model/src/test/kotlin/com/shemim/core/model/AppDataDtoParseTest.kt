package com.shemim.core.model

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * تست پارس JSON→DTO — نگاشت قرارداد loadData (api/data-helpers.php).
 *
 * فیکسچر عمداً «کثیف» است: کلید ناشناخته (expand)، فیلد مشروط غایب
 * (fatherName برای بینندهٔ عادی)، null صریح برای فیلد رشته‌ای،
 * نام‌گذاری مخلوط requests و کلید snake_case اطلاعیه.
 */
class AppDataDtoParseTest {

    private val memberJson = """
    {
      "id": "m-101", "membershipNumber": 101, "firstName": "زهرا", "lastName": "محمدی",
      "username": "zahra", "role": "member", "isOwner": false,
      "joinDate": "1401/03/15", "birthDate": "1370/08/22", "birthDateLocked": true,
      "phone": "09120000000", "email": "", "colorTheme": "emerald",
      "transactions": [
        {"id":"t-1","type":"deposit","amount":2500000,"description":"واریز ماهانه",
         "date":"1403/05/12","createdAt":"2024-08-02 10:11:12","createdBy":"admin",
         "category":"monthly","isInstallment":false,"loanId":null},
        {"id":"t-2","type":"withdraw","amount":800000,"description":null,"date":"1403/06/01",
         "isInstallment":true,"loanId":"l-9",
         "fromMonth":5,"fromYear":1403,"toMonth":7,"toYear":1403}
      ],
      "loans": [
        {"id":"l-9","loanNumber":9,"amount":12000000,"totalInstallments":12,
         "paidInstallments":2,"fee":300000,"startDate":"1403/04/01","description":"وام تعمیرات",
         "isEmergency":true,"splitMethod":2,
         "installmentsHistory":[
           {"installmentNumber":1,"amount":1050000,"date":"1403/05/01"},
           {"installmentNumber":2,"amount":1050000,"date":"1403/06/01"}
         ]}
      ]
    }
    """.trimIndent()

    private val adminMemberJson = """
    {
      "id": "m-1", "membershipNumber": 1, "firstName": "مدیر", "lastName": "صندوق",
      "username": "admin", "role": "admin", "isOwner": true,
      "fatherName": "احمد", "nationalCode": "0012345678",
      "isSupervisor": false, "supervisorPerms": null,
      "joinDate": "1400/01/01", "birthDate": "1365/02/03", "birthDateLocked": false,
      "phone": null, "email": "zahra@x.ir", "colorTheme": "royal",
      "transactions": [], "loans": []
    }
    """.trimIndent()

    private fun fullAppDataJson(): String {
        return """
        {
          "success": true,
          "dataVersion": "9f2c8a77e1",
          "appData": {
            "fundName": "صندوق خانوادگی",
            "ownerPower": null,
            "quizPoints": {"score": 12},
            "mainAccount": 48300000,
            "incomeExpenseAccount": -2100000,
            "members": [$adminMemberJson, $memberJson],
            "incomeExpenseTransactions": [
              {"id":"ie-1","type":"income","amount":500000,"reason":"کارمزد وام",
               "description":null,"transaction_date":"1403/05/02","date":"1403/05/02",
               "created_by":"admin","createdBy":"admin","created_at":null,"createdAt":null}
            ],
            "groups": [{"id":"g-1","leaderId":"m-1","memberIds":["m-101"]}],
            "resignedMembers": [
              {"id":"m-2","membership_number":7,"membershipNumber":7,"first_name":"علی",
               "firstName":"علی","last_name":"رنجبر","lastName":"رنجبر","username":"ali",
               "role":"resigned","join_date":"1399/01/10","joinDate":"1399/01/10",
               "birth_date":null,"birthDate":null,"phone":"09350000000","email":"",
               "resignedDate":""}
            ],
            "settings": {"loanMultiplier":2.5,"loanMultiplierMode":"manual",
              "loanReservePercent":15,"loanMonthlyDivisor":3,"loanCooldownMonths":2,
              "loanDelayCooldownFactor":1.5,"loanPersonCapFactor":1.2,
              "latePenaltyPerDayPerMillion":8000,"birthdayGiftAmount":200000,
              "currency":"toman","displayMode":null,"fundLogo":"","theme":"royal"},
            "profitReports": [
              {"id":"pr-1","version":2,"date":"1403/05/01","startDate":"1402/06/01",
               "endDate":"1403/05/01","description":"سود سال","totalProfit":9000000,
               "details":[{"balanceScore":3,"loanInRangeScore":2,"amount":1000}]}
            ],
            "requests": [
              {"id":"r-1","type":"loan","memberId":"m-101","memberName":"زهرا محمدی",
               "amount":5000000,"description":"وام ضروری","request_date":"1403/05/20",
               "status":"pending","requested_by":"زهرا","category":null,
               "from_month":6,"from_year":1403,"to_month":null,"to_year":null,
               "installments":10,"preferred_date":"1403/07/01","loan_id":null,
               "installment_number":null,"due_date":null,"installment_date":null,
               "approved_by":"","approved_at":"","reject_reason":""}
            ],
            "requestsV2": [
              {"id":"rv-1","type":"installment_payment","member_id":"m-101",
               "member_name":"زهرا محمدی","amount":1050000,"description":"قسط دوم",
               "request_date":"1403/06/10","status":"approved","requested_by":"مدیر",
               "approved_by":"مدیر","approved_at":"1403/06/11","reject_reason":""}
            ],
            "loanQueue": [
              {"id":"q-1","memberId":"m-101","memberName":"زهرا محمدی",
               "requestDate":"1403/06/15","status":"in_queue","priority":"high",
               "requestedAmount":6000000,"requestedInstallments":12,
               "adminEstimatedDate":"1403/09/01","loanId":null,"loanNumber":null,
               "grantedAt":null}
            ],
            "notifications": [
              {"id":"n-1","title":"جلسه ماهانه","message":"جلسه فردا ساعت ۱۸",
               "priority":"high","date":"1403/06/20","target":"all",
               "target_member_id":null,
               "replies":[{"memberId":"m-101","text":"حاضر هستم"}],
               "readBy":["m-1","m-101"],
               "files":[{"id":"f-1","name":"دعوت‌نامه.pdf","mime":"application/pdf","size":81234}]}
            ],
            "nextMemberNumber": 102,
            "nextLoanNumber": 10,
            "expand": "کلید آیندهٔ سرور — باید نادیده گرفته شود"
          },
          "rfWatch": null
        }
        """.trimIndent()
    }

    @Test
    fun `full appData parses with all fields mapped`() {
        val res = AppDataJson.decodeFromString(LoadDataResponse.serializer(), fullAppDataJson())

        assertTrue(res.success)
        assertFalse(res.unchanged)
        assertTrue(res.hasFreshData)
        assertEquals("9f2c8a77e1", res.dataVersion)
        assertNull(res.rfWatch)

        val app = res.appData!!
        assertEquals("صندوق خانوادگی", app.fundName)
        assertEquals(48300000L, app.mainAccount)
        assertEquals(-2100000L, app.incomeExpenseAccount)
        assertEquals(102, app.nextMemberNumber)
        assertEquals(10, app.nextLoanNumber)
        assertNull(app.ownerPower)
        assertEquals(12, app.quizPoints!!["score"]!!.toString().toInt())

        // اعضا: عضو عادی بدون فیلدهای مشروط
        val viewer = app.members.first { it.id == "m-101" }
        assertEquals(101, viewer.membershipNumber)
        assertTrue(viewer.birthDateLocked)
        assertEquals("", viewer.email)
        assertNull(viewer.fatherName)
        assertNull(viewer.nationalCode)
        assertNull(viewer.isSupervisor)

        // تراکنش‌ها + بازهٔ مقرری
        val t2 = viewer.transactions[1]
        assertTrue(t2.isInstallment)
        assertEquals("l-9", t2.loanId)
        assertEquals(5, t2.fromMonth)
        assertEquals(1403, t2.toYear)

        // وام + تاریخچهٔ اقساط
        val loan = viewer.loans[0]
        assertEquals(2, loan.paidInstallments)
        assertTrue(loan.isEmergency)
        assertEquals(2, loan.splitMethod)
        assertEquals(1050000L, loan.installmentsHistory[0].amount)

        // عضو مدیر: فیلدهای مشروط حاضرند
        val admin = app.members.first { it.role == "admin" }
        assertTrue(admin.isOwner)
        assertEquals("احمد", admin.fatherName)
        assertEquals("0012345678", admin.nationalCode)
        assertEquals(false, admin.isSupervisor)
        assertNull(admin.supervisorPerms)
    }

    @Test
    fun `mixed-case request keys and raw v2 rows map correctly`() {
        val app = AppDataJson.decodeFromString(LoadDataResponse.serializer(), fullAppDataJson()).appData!!

        val v1 = app.requests.single()
        assertEquals("m-101", v1.memberId)          // camelCase
        assertEquals("1403/05/20", v1.requestDate)  // snake_case
        assertEquals(6, v1.fromMonth)
        assertEquals(10, v1.installments)
        assertEquals("", v1.rejectReason)

        val v2 = app.requestsV2.single()
        assertEquals("m-101", v2.memberId)          // snake_case در V2
        assertEquals("زهرا محمدی", v2.memberName)
        assertEquals("1403/06/11", v2.approvedAt)
    }

    @Test
    fun `notification readBy replies and files map correctly`() {
        val n = AppDataJson.decodeFromString(LoadDataResponse.serializer(), fullAppDataJson())
            .appData!!.notifications.single()

        assertEquals("high", n.priority)
        assertNull(n.targetMemberId)
        assertEquals(listOf("m-1", "m-101"), n.readBy)
        assertEquals(1, n.replies.size)
        val file = n.files.single()
        assertEquals("دعوت‌نامه.pdf", file.name)
        assertEquals(81234L, file.size)
    }

    @Test
    fun `income expense raw and added keys map correctly`() {
        val ie = AppDataJson.decodeFromString(LoadDataResponse.serializer(), fullAppDataJson())
            .appData!!.incomeExpenseTransactions.single()

        assertEquals("income", ie.type)
        assertEquals("1403/05/02", ie.date)      // کلید اضافه‌شدهٔ سرور
        assertEquals("admin", ie.createdBy)     // کلید اضافه‌شدهٔ سرور
        assertNull(ie.createdAt)                // coerce: null → default null
        assertEquals("کارمزد وام", ie.reason)
    }

    @Test
    fun `resigned member keeps camel keys and ignores raw snake keys`() {
        val rm = AppDataJson.decodeFromString(LoadDataResponse.serializer(), fullAppDataJson())
            .appData!!.resignedMembers.single()

        assertEquals(7, rm.membershipNumber)
        assertEquals("علی", rm.firstName)
        assertEquals("resigned", rm.role)
        assertEquals("", rm.resignedDate)
        assertNull(rm.birthDate)
    }

    @Test
    fun `unchanged response has no appData and flags correctly`() {
        val json = """{"success":true,"unchanged":true,"dataVersion":"abc","rfWatch":{"live":true}}"""
        val res = AppDataJson.decodeFromString(LoadDataResponse.serializer(), json)

        assertTrue(res.unchanged)
        assertFalse(res.hasFreshData)
        assertNull(res.appData)
        assertEquals("""{"live":true}""", res.rfWatch.toString())
    }

    @Test
    fun `settings fractional factors parse as doubles`() {
        val s = AppDataJson.decodeFromString(LoadDataResponse.serializer(), fullAppDataJson())
            .appData!!.settings

        assertEquals(2.5, s.loanMultiplier!!, 0.0)
        assertEquals("manual", s.loanMultiplierMode)
        assertEquals(15, s.loanReservePercent)
        assertEquals(1.5, s.loanDelayCooldownFactor!!, 0.0)
        assertEquals(1.2, s.loanPersonCapFactor!!, 0.0)
        assertEquals(8000.0, s.latePenaltyPerDayPerMillion!!, 0.0)
        assertEquals(200000L, s.birthdayGiftAmount)
        assertEquals("toman", s.currency)
        assertEquals("royal", s.theme)
        assertNull(s.displayMode)
        assertNull(s.colorTheme)
    }

    @Test
    fun `empty and missing collections default safely`() {
        val json = """{"success":true,"dataVersion":"v","appData":{"fundName":"x"}}"""
        val app = AppDataJson.decodeFromString(LoadDataResponse.serializer(), json).appData!!

        assertEquals("x", app.fundName)
        assertTrue(app.members.isEmpty())
        assertTrue(app.notifications.isEmpty())
        assertEquals(1, app.nextMemberNumber)
        assertEquals(SettingsDto(), app.settings)
    }

    @Test
    fun `member list roundtrips through serializer`() {
        val members = listOf(
            AppDataJson.decodeFromString(MemberDto.serializer(), memberJson),
        )
        val encoded = AppDataJson.encodeToString(
            ListSerializer(MemberDto.serializer()), members,
        )
        val back = AppDataJson.decodeFromString(ListSerializer(MemberDto.serializer()), encoded)
        assertEquals(members, back)
    }
}
