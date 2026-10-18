package com.gymtrack.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One fee payment against a member. Deleting a member cascades and
 * removes their payment history too.
 */
@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(
            entity = Member::class,
            parentColumns = ["id"],
            childColumns = ["memberId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("memberId")]
)
data class Payment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val memberId: Long,
    val amount: Double,
    /** Epoch-millis when the payment was recorded. */
    val date: Long,
    val note: String = ""
)
