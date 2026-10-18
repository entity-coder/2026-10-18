package com.gymtrack.app.data

/**
 * Membership plan durations offered by the gym.
 * Each renewal extends the member's paid period by [months].
 */
enum class PlanType(val months: Int, val displayName: String) {
    MONTHLY(1, "Monthly"),
    QUARTERLY(3, "Quarterly"),
    YEARLY(12, "Yearly");

    companion object {
        /** Lenient parse: unknown stored values fall back to MONTHLY. */
        fun fromString(value: String?): PlanType =
            values().firstOrNull { it.name == value } ?: MONTHLY
    }
}
