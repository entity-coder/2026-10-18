package com.gymtrack.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CheckInDao {

    /**
     * Records a check-in. IGNORE keeps this idempotent: a second tap for the
     * same member/day is silently dropped by the unique index instead of
     * crashing — the UI greys the button out anyway.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun checkIn(checkIn: CheckIn): Long

    @Query("SELECT COUNT(*) FROM check_ins WHERE day = :day")
    fun countForDay(day: Long): Flow<Int>

    /** Today's visitors with names, newest first. */
    @Query(
        "SELECT c.*, m.name AS memberName FROM check_ins c " +
        "JOIN members m ON m.id = c.memberId " +
        "WHERE c.day = :day ORDER BY c.id DESC"
    )
    fun observeForDay(day: Long): Flow<List<CheckInWithMember>>

    /** The member ids that already checked in on [day] — drives the button state. */
    @Query("SELECT memberId FROM check_ins WHERE day = :day")
    fun checkedInMemberIds(day: Long): Flow<List<Long>>
}
