package com.gymtrack.app.logic

import com.gymtrack.app.data.Member
import com.gymtrack.app.data.MembershipStatus
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Pure date/money math for memberships. Kept out of the UI layer so the
 * rules are testable and shared by the dashboard, lists, and detail screen.
 *
 * All day boundaries are "midnight epoch-millis" in the device timezone.
 * (java.time is API 26+; minSdk is 24, so this uses Calendar instead of
 * adding the desugaring dependency.)
 */
object MembershipCalculator {

    /** Members expiring within this many days (inclusive) count as "expiring soon". */
    const val EXPIRING_SOON_DAYS = 7L

    private const val DAY_MILLIS = 24L * 60 * 60 * 1000

    private val dateFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
    private val moneyFormat: NumberFormat =
        NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
            maximumFractionDigits = 0 // gym fees are whole rupees
        }

    /** Midnight (start of day) for today, device timezone. */
    fun startOfToday(): Long = startOfDay(System.currentTimeMillis())

    /** Midnight (start of day) for any timestamp. */
    fun startOfDay(millis: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /** Midnight of the first day of the current month. */
    fun startOfMonth(): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = startOfToday()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        return cal.timeInMillis
    }

    /** [base] plus [months] calendar months, normalized back to midnight. */
    fun addMonths(base: Long, months: Int): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = startOfDay(base)
        cal.add(Calendar.MONTH, months)
        return cal.timeInMillis
    }

    /** Whole days from [fromStart] (inclusive) to [toStart] (inclusive). */
    fun daysUntil(fromStart: Long, toStart: Long): Long =
        TimeUnit.MILLISECONDS.toDays(toStart - fromStart)

    /**
     * The membership status shown as a badge everywhere in the app.
     * A membership is still valid ON its end date; it becomes FEE_DUE
     * the day after.
     */
    fun statusOf(member: Member, todayStart: Long = startOfToday()): MembershipStatus {
        val daysLeft = daysUntil(todayStart, member.planEndDate)
        return when {
            daysLeft < 0 -> MembershipStatus.FEE_DUE
            daysLeft <= EXPIRING_SOON_DAYS -> MembershipStatus.EXPIRING_SOON
            else -> MembershipStatus.OK
        }
    }

    /**
     * New paid-period end date after recording one plan renewal payment.
     * If the membership already lapsed, the new period starts today;
     * otherwise it extends from the current end date (no lost days).
     */
    fun renewedExpiry(member: Member, todayStart: Long = startOfToday()): Long {
        val base = maxOf(member.planEndDate, todayStart)
        return addMonths(base, member.planType.months)
    }

    /** Expiry date for a brand-new member joining today. */
    fun newMemberExpiry(planMonths: Int, joinDate: Long = startOfToday()): Long =
        addMonths(joinDate, planMonths)

    fun formatDate(millis: Long): String = dateFormat.format(Date(millis))

    fun formatMoney(amount: Double): String = moneyFormat.format(amount)

    /** "Valid till 18 Nov 2026" / "Expired on 11 Oct 2026" line for the UI. */
    fun validityLine(member: Member, todayStart: Long = startOfToday()): String {
        val end = formatDate(member.planEndDate)
        return if (daysUntil(todayStart, member.planEndDate) < 0) "Expired on $end"
        else "Valid till $end"
    }
}
