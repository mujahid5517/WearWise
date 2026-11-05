package com.example.data.notification

import android.content.Context
import com.example.domain.model.WeeklyScheduleItem

class OutfitReminderScheduler(private val context: Context) {
    fun schedule(item: WeeklyScheduleItem) {}
    fun schedule(scheduleId: Long, timeMinutes: Int, dateIso: String) {}
    fun cancel(scheduleId: Long) {}
    fun cancel(dateIso: String) {}

    companion object {
        const val PREFERENCES_NAME = "outfit_reminders_prefs"
        const val KEY_OUTFIT_REMINDERS = "outfit_reminders_enabled"

        fun notificationsEnabled(context: Context): Boolean {
            return context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_OUTFIT_REMINDERS, true)
        }
    }
}
