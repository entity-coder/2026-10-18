package com.gymtrack.app.data

/**
 * Fee/expiry status of a member's current paid period.
 * Computed by [com.gymtrack.app.logic.MembershipCalculator.statusOf], never stored.
 */
enum class MembershipStatus {
    /** Membership valid for more than [EXPIRING_SOON_DAYS] days. */
    OK,

    /** Still valid today, but lapses within the warning window. */
    EXPIRING_SOON,

    /** Paid period ended — fee is due before the next visit. */
    FEE_DUE
}
