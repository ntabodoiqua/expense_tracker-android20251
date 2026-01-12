package com.example.expensetracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    // Lấy toàn bộ danh sách, sắp xếp mới nhất lên đầu
    // Flow là dạng dữ liệu realtime của Kotlin (giống Stream), UI sẽ tự cập nhật khi DB thay đổi
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    // Tính tổng thu/chi (để hiển thị dashboard)
    @Query("SELECT SUM(amount) FROM transactions WHERE type = :type")
    fun getTotalAmountByType(type: Int): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()
}