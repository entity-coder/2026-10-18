package com.gymtrack.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Room database for GymTrack. Three tables: members, payments, check_ins.
 * Schema export is disabled ([exportSchema] = false) since this is a
 * single-version demo with no migrations to track.
 */
@Database(
    entities = [Member::class, Payment::class, CheckIn::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class GymDatabase : RoomDatabase() {

    abstract fun memberDao(): MemberDao
    abstract fun paymentDao(): PaymentDao
    abstract fun checkInDao(): CheckInDao

    companion object {
        private const val DB_NAME = "gymtrack_db"

        @Volatile
        private var instance: GymDatabase? = null

        fun getInstance(context: Context): GymDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    GymDatabase::class.java,
                    DB_NAME
                ).build().also { instance = it }
            }
    }
}
