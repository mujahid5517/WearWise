package com.example.data.notification

import android.content.Context
import com.example.domain.model.WeeklyScheduleItem

class OutfitReminderScheduler(private val context: Context) {
    fun schedule(plan: WeeklyScheduleItem) {}
    fun cancel(dateIso: String) {}

    companion object {
        const val PREFERENCES_NAME = "wearwise_notifications"
        const val KEY_OUTFIT_REMINDERS = "outfit_reminders_enabled"

        fun notificationsEnabled(context: Context): Boolean {
            return context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_OUTFIT_REMINDERS, true)
        }

        fun schedule(context: Context) {}
        fun cancel(context: Context) {}
    }
}
