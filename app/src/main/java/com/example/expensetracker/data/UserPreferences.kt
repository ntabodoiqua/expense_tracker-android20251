package com.example.expensetracker.data

import android.content.Context
import android.content.SharedPreferences

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "expense_tracker_prefs"
        private const val KEY_IS_FIRST_TIME = "is_first_time"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_AVATAR = "user_avatar"
        private const val KEY_INITIAL_BALANCE = "initial_balance"
        private const val KEY_DAILY_LIMIT = "daily_limit"
        private const val KEY_MONTHLY_LIMIT = "monthly_limit"
        private const val KEY_DAILY_LIMIT_ENABLED = "daily_limit_enabled"
        private const val KEY_MONTHLY_LIMIT_ENABLED = "monthly_limit_enabled"
        private const val KEY_LAST_NOTIFICATION_READ_TIME = "last_notification_read_time"
    }

    var isFirstTime: Boolean
        get() = prefs.getBoolean(KEY_IS_FIRST_TIME, true)
        set(value) = prefs.edit().putBoolean(KEY_IS_FIRST_TIME, value).apply()

    var userName: String
        get() = prefs.getString(KEY_USER_NAME, "Username") ?: "Username"
        set(value) = prefs.edit().putString(KEY_USER_NAME, value).apply()

    var userAvatar: String
        get() = prefs.getString(KEY_USER_AVATAR, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USER_AVATAR, value).apply()

    var initialBalance: Double
        get() = prefs.getFloat(KEY_INITIAL_BALANCE, 0f).toDouble()
        set(value) = prefs.edit().putFloat(KEY_INITIAL_BALANCE, value.toFloat()).apply()

    var dailyLimit: Double
        get() = prefs.getFloat(KEY_DAILY_LIMIT, 0f).toDouble()
        set(value) = prefs.edit().putFloat(KEY_DAILY_LIMIT, value.toFloat()).apply()

    var monthlyLimit: Double
        get() = prefs.getFloat(KEY_MONTHLY_LIMIT, 0f).toDouble()
        set(value) = prefs.edit().putFloat(KEY_MONTHLY_LIMIT, value.toFloat()).apply()

    var isDailyLimitEnabled: Boolean
        get() = prefs.getBoolean(KEY_DAILY_LIMIT_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_DAILY_LIMIT_ENABLED, value).apply()

    var isMonthlyLimitEnabled: Boolean
        get() = prefs.getBoolean(KEY_MONTHLY_LIMIT_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_MONTHLY_LIMIT_ENABLED, value).apply()

    var lastNotificationReadTime: Long
        get() = prefs.getLong(KEY_LAST_NOTIFICATION_READ_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_NOTIFICATION_READ_TIME, value).apply()

    // Xóa tất cả dữ liệu preferences
    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
