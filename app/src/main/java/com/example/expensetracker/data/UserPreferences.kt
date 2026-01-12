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
    }

    // Kiểm tra có phải lần đầu mở app không
    var isFirstTime: Boolean
        get() = prefs.getBoolean(KEY_IS_FIRST_TIME, true)
        set(value) = prefs.edit().putBoolean(KEY_IS_FIRST_TIME, value).apply()

    // Tên người dùng
    var userName: String
        get() = prefs.getString(KEY_USER_NAME, "Username") ?: "Username"
        set(value) = prefs.edit().putString(KEY_USER_NAME, value).apply()

    // Avatar URI (nếu người dùng chọn ảnh từ gallery)
    var userAvatar: String
        get() = prefs.getString(KEY_USER_AVATAR, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USER_AVATAR, value).apply()

    // Số dư ban đầu
    var initialBalance: Double
        get() = prefs.getFloat(KEY_INITIAL_BALANCE, 0f).toDouble()
        set(value) = prefs.edit().putFloat(KEY_INITIAL_BALANCE, value.toFloat()).apply()

    // Xóa tất cả dữ liệu (nếu cần reset)
    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
