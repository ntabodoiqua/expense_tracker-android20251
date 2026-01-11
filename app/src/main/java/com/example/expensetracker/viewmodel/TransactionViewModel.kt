package com.example.expensetracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.AppDatabase
import com.example.expensetracker.data.Transaction
import kotlinx.coroutines.launch

// Dùng AndroidViewModel để dễ dàng lấy Context khởi tạo Database
class TransactionViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getDatabase(application).transactionDao()

    // Chuyển đổi Flow từ Room sang LiveData để Activity dễ quan sát
    val allTransactions: LiveData<List<Transaction>> = dao.getAllTransactions().asLiveData()

    // Hàm thêm giao dịch (chạy trên background thread nhờ coroutines)
    fun addTransaction(transaction: Transaction) {
        viewModelScope.launch {
            dao.insertTransaction(transaction)
        }
    }
    // Hàm xóa giao dịch
    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            dao.deleteTransaction(transaction)
        }
    }
    // Hàm sửa giao dịch
    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            dao.updateTransaction(transaction)
        }
    }

}