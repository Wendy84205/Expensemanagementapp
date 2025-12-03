package com.example.financeapp.data.models

import java.util.*

enum class RecurringFrequency(
    val displayName: String,
    val days: Int,
    val calendarField: Int = Calendar.DATE
) {
    DAILY("Hàng ngày", 1, Calendar.DATE),
    WEEKLY("Hàng tuần", 7, Calendar.WEEK_OF_YEAR),
    MONTHLY("Hàng tháng", 30, Calendar.MONTH),
    QUARTERLY("Hàng quý", 90, Calendar.MONTH),
    YEARLY("Hàng năm", 365, Calendar.YEAR);

    companion object {
        fun fromDisplayName(displayName: String): RecurringFrequency? {
            return values().find { it.displayName == displayName }
        }

        fun fromName(name: String): RecurringFrequency? {
            return values().find { it.name == name }
        }

        fun getAllDisplayNames(): List<String> {
            return values().map { it.displayName }
        }
    }
}