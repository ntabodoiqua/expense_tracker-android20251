package com.example.expensetracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    // Lấy toàn bộ danh sách giao dịch sắp xếp theo ngày
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    // Tính tổng tiền theo loại giao dịch
    @Query("SELECT SUM(amount) FROM transactions WHERE type = :type")
    fun getTotalAmountByType(type: Int): Flow<Double?>

    // Thêm giao dịch mới
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    // Cập nhật giao dịch
    @Update
    suspend fun updateTransaction(transaction: Transaction)

    // Xóa giao dịch
    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    // Xóa tất cả giao dịch
    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()
}
