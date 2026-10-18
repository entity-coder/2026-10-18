package com.gymtrack.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MemberDao {

    @Insert
    suspend fun insert(member: Member): Long

    @Update
    suspend fun update(member: Member)

    @Delete
    suspend fun delete(member: Member)

    @Query("SELECT * FROM members ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<Member>>

    @Query("SELECT * FROM members WHERE id = :id")
    fun observeById(id: Long): Flow<Member?>

    @Query(
        "SELECT * FROM members " +
        "WHERE name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%' " +
        "ORDER BY name COLLATE NOCASE ASC"
    )
    fun search(query: String): Flow<List<Member>>

    /** Members whose paid period covers today — the "active" count on the dashboard. */
    @Query("SELECT COUNT(*) FROM members WHERE planEndDate >= :todayStart")
    fun countActive(todayStart: Long): Flow<Int>

    /** Members whose membership lapses within [soonEnd] (inclusive), still valid today. */
    @Query(
        "SELECT * FROM members " +
        "WHERE planEndDate BETWEEN :todayStart AND :soonEnd " +
        "ORDER BY planEndDate ASC"
    )
    fun expiringSoon(todayStart: Long, soonEnd: Long): Flow<List<Member>>

    /** Members whose membership already lapsed — the defaulter list. */
    @Query("SELECT * FROM members WHERE planEndDate < :todayStart ORDER BY planEndDate ASC")
    fun defaulters(todayStart: Long): Flow<List<Member>>
}
