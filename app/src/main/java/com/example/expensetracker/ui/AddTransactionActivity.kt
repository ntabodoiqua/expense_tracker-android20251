package com.example.expensetracker.ui

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.expensetracker.data.Transaction
import com.example.expensetracker.databinding.ActivityAddTransactionBinding
import com.example.expensetracker.viewmodel.TransactionViewModel

class AddTransactionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddTransactionBinding
    private lateinit var viewModel: TransactionViewModel

    // Biến để lưu transaction đang sửa (nếu có)
    private var currentTransaction: Transaction? = null
    // Biến lưu thời gian, mặc định là thời điểm hiện tại
    private var selectedDate: Long = System.currentTimeMillis()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTransactionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[TransactionViewModel::class.java]

        setupSpinner()

        // --- CHECK XEM CÓ PHẢI ĐANG SỬA KHÔNG ---
        // Lấy dữ liệu gửi từ MainActivity sang (nếu có)
        // Lưu ý: Dùng getSerializableExtra cần ép kiểu
        currentTransaction = intent.getSerializableExtra("transaction_data") as? Transaction

        if (currentTransaction != null) {
            // Nếu có dữ liệu -> Đang là chế độ SỬA
            setupEditMode()
        }

        setupEvents()
        updateDateDisplay()
    }

    // Hàm điền dữ liệu cũ vào form
    private fun setupEditMode() {
        val t = currentTransaction!! // Chắc chắn không null

        binding.etAmount.setText(t.amount.toString().replace(".0", "")) // Xóa .0 nếu là số chẵn
        binding.etNote.setText(t.note)

        selectedDate = t.date
        updateDateDisplay()

        // Set Radio Button
        if (t.type == 1) binding.rbIncome.isChecked = true
        else binding.rbExpense.isChecked = true

        // Set Spinner (Hơi phức tạp chút vì phải tìm vị trí text trong list)
        val categories = listOf("Ăn uống", "Đi lại", "Mua sắm", "Giải trí", "Lương", "Thưởng", "Khác")
        val index = categories.indexOf(t.category)
        if (index >= 0) {
            binding.spinnerCategory.setSelection(index)
        }

        // Đổi tên nút thành "Cập nhật"
        binding.btnSave.text = "Cập nhật"
    }

    private fun setupSpinner() {
        val categories = listOf("Ăn uống", "Đi lại", "Mua sắm", "Giải trí", "Lương", "Thưởng", "Khác")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = adapter
    }

    private fun setupEvents() {
        binding.btnClose.setOnClickListener { finish() }

        binding.btnSave.setOnClickListener {
            saveTransaction()
        }

        binding.editDate.setOnClickListener {
            showDatePicker()
        }
    }

    private fun saveTransaction() {
        val amountStr = binding.etAmount.text.toString()
        val note = binding.etNote.text.toString()
        val category = binding.spinnerCategory.selectedItem.toString()

        if (amountStr.isEmpty()) {
            binding.etAmount.error = "Vui lòng nhập số tiền"
            return
        }

        val amount = amountStr.toDoubleOrNull() ?: 0.0
        val type = if (binding.rbIncome.isChecked) 1 else 0

        if (currentTransaction == null) {
            // --- TRƯỜNG HỢP 1: THÊM MỚI ---
            val newTransaction = Transaction(
                title = category,
                amount = amount,
                type = type,
                category = category,
                note = note,
                date = selectedDate
            )
            viewModel.addTransaction(newTransaction)
            Toast.makeText(this, "Đã thêm giao dịch!", Toast.LENGTH_SHORT).show()

        } else {
            // --- TRƯỜNG HỢP 2: CẬP NHẬT ---
            // Quan trọng: Phải copy lại cái ID cũ, nếu không Room sẽ tưởng là bản ghi mới
            val updatedTransaction = currentTransaction!!.copy(
                title = category,
                amount = amount,
                type = type,
                category = category,
                note = note,
                date = selectedDate
            )
            viewModel.updateTransaction(updatedTransaction)
            Toast.makeText(this, "Đã cập nhật!", Toast.LENGTH_SHORT).show()
        }

        finish()
    }
    private fun showDatePicker() {
        // Lấy ngày tháng hiện tại để hiển thị mặc định lên lịch
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = selectedDate

        val year = calendar.get(java.util.Calendar.YEAR)
        val month = calendar.get(java.util.Calendar.MONTH)
        val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)

        // Tạo hộp thoại chọn ngày
        val datePickerDialog = android.app.DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                // Khi người dùng chọn xong ngày
                val newCalendar = java.util.Calendar.getInstance()
                newCalendar.set(selectedYear, selectedMonth, selectedDay)

                // Lưu lại vào biến
                selectedDate = newCalendar.timeInMillis

                // Cập nhật text hiển thị trên màn hình
                updateDateDisplay()
            },
            year, month, day
        )
        datePickerDialog.show()
    }

    // Hàm phụ để hiển thị ngày đẹp (vd: 09/01/2026) lên ô nhập
    private fun updateDateDisplay() {
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
        binding.editDate.setText(sdf.format(java.util.Date(selectedDate)))
    }
}