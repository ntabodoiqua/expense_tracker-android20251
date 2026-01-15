package com.example.expensetracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val amount: Double,
    val type: Int,
    val category: String,
    val note: String = "",
    val date: Long = System.currentTimeMillis()
) : Serializable
