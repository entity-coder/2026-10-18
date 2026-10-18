package com.gymtrack.app.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One daily gym visit. The unique index on (memberId, day) enforces
 * one check-in per member per day at the database level — the UI's
 * "already checked in" state is just a friendly reflection of this.
 */
@Entity(
    tableName = "check_ins",
    foreignKeys = [
        ForeignKey(
            entity = Member::class,
            parentColumns = ["id"],
            childColumns = ["memberId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("memberId"),
        Index(value = ["memberId", "day"], unique = true)
    ]
)
data class CheckIn(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val memberId: Long,
    /** Midnight epoch-millis of the visit day. */
    val day: Long
)

/**
 * A check-in row joined with the member's name for display lists.
 * Room fills [checkIn] from the c.* columns and [memberName] from
 * the aliased column.
 */
data class CheckInWithMember(
    @Embedded val checkIn: CheckIn,
    val memberName: String
)
