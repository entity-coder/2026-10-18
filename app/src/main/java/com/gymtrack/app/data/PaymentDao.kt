package com.gymtrack.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {

    @Insert
    suspend fun insert(payment: Payment): Long

    @Query("SELECT * FROM payments ORDER BY date DESC")
    fun observeAll(): Flow<List<Payment>>

    @Query("SELECT * FROM payments WHERE memberId = :memberId ORDER BY date DESC")
    fun observeForMember(memberId: Long): Flow<List<Payment>>

    /** Total fees collected since [from] (epoch-millis) — dashboard "this month" figure. */
    @Query("SELECT COALESCE(SUM(amount), 0) FROM payments WHERE date >= :from")
    fun sumSince(from: Long): Flow<Double>
}
