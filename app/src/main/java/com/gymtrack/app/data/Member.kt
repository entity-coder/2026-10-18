package com.gymtrack.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A gym member.
 *
 * Fee state is derived from [planStartDate]/[planEndDate]: the member's
 * membership is paid up through [planEndDate] (midnight at the start of
 * that day — i.e. the membership is still valid on the end date itself).
 * Recording a payment extends [planEndDate] by the plan's month count,
 * starting from today if the membership already lapsed.
 */
@Entity(tableName = "members")
data class Member(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val phone: String,
    /** Content URI string of the member's photo, picked from the gallery. Null if none. */
    val photoUri: String? = null,
    val planType: PlanType,
    /** Fee charged per plan period (e.g. per month for MONTHLY). */
    val planFee: Double,
    /** Midnight epoch-millis when the current paid period started. */
    val planStartDate: Long,
    /** Midnight epoch-millis of the last day the membership is valid. */
    val planEndDate: Long
)
