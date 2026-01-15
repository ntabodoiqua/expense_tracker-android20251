package com.example.expensetracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.AppDatabase
import com.example.expensetracker.data.Transaction
import kotlinx.coroutines.launch

class TransactionViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).transactionDao()
    val allTransactions: LiveData<List<Transaction>> = dao.getAllTransactions().asLiveData()

    // Thêm giao dịch mới
    fun addTransaction(transaction: Transaction) {
        viewModelScope.launch {
            dao.insertTransaction(transaction)
        }
    }

    // Xóa giao dịch
    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            dao.deleteTransaction(transaction)
        }
    }

    // Cập nhật giao dịch
    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            dao.updateTransaction(transaction)
        }
    }

    // Xóa tất cả giao dịch
    fun deleteAllTransactions() {
        viewModelScope.launch {
            dao.deleteAllTransactions()
        }
    }
}
