package com.gymtrack.app.data

import androidx.room.TypeConverter

/**
 * Room type converters. PlanType is stored as its enum name string
 * so the database stays readable and robust to enum reordering.
 */
class Converters {
    @TypeConverter
    fun planTypeToString(planType: PlanType): String = planType.name

    @TypeConverter
    fun stringToPlanType(value: String?): PlanType = PlanType.fromString(value)
}
