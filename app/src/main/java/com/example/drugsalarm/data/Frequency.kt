package com.example.drugsalarm.data

enum class Frequency(val label: String) {
    HOURLY_1("Every 1 Hour"),
    HOURLY_2("Every 2 Hours"),
    HOURLY_4("Every 4 Hours"),
    HOURLY_8("Every 8 Hours"),
    HOURLY_12("Every 12 Hours"),
    DAILY("Daily"),
    WEEKLY("Weekly"),
    EVERY_2_WEEKS("Every 2 Weeks"),
    MONTHLY("Monthly"),
    ONCE("Once");

    companion object {
        fun fromLabel(label: String): Frequency {
            return entries.find { it.label == label || it.name.lowercase().contains(label.lowercase()) } ?: DAILY
        }
        
        fun getAllLabels(): List<String> = entries.map { it.label }
    }
}
