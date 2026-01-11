package com.example.expensetracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val title: String,      // Ví dụ: Ăn sáng, Tiền nhà
    val amount: Double,     // Số tiền: 30000.0
    val type: Int,          // 0: Chi tiêu (Expense), 1: Thu nhập (Income)
    val category: String,   // Ăn uống, Giải trí...
    val note: String = "",  // Ghi chú thêm
    val date: Long = System.currentTimeMillis() // Lưu thời gian dạng mili giây
) : Serializable // Serializable để sau này truyền object giữa các màn hình