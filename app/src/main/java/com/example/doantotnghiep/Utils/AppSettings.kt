package com.example.doantotnghiep.Utils

import android.content.Context

object AppSettings {
    private const val PREF_NAME = "app_settings"
    private const val KEY_PUSH_ENABLED = "push_enabled"
    private const val KEY_PUSH_CHAT = "push_chat"
    private const val KEY_PUSH_APPOINTMENT = "push_appointment"
    private const val KEY_PUSH_SYSTEM = "push_system"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun isPushEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_PUSH_ENABLED, true)

    fun setPushEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_PUSH_ENABLED, enabled).apply()
    }

    fun isChatPushEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_PUSH_CHAT, true)

    fun setChatPushEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_PUSH_CHAT, enabled).apply()
    }

    fun isAppointmentPushEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_PUSH_APPOINTMENT, true)

    fun setAppointmentPushEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_PUSH_APPOINTMENT, enabled).apply()
    }

    fun isSystemPushEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_PUSH_SYSTEM, true)

    fun setSystemPushEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_PUSH_SYSTEM, enabled).apply()
    }

    fun shouldShowNotification(context: Context, type: String): Boolean {
        if (!isPushEnabled(context)) return false
        return when {
            type == "new_message" -> isChatPushEnabled(context)
            type.startsWith("appointment_") -> isAppointmentPushEnabled(context)
            else -> isSystemPushEnabled(context)
        }
    }
}
