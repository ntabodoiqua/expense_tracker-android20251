package com.example.expensetracker.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.example.expensetracker.R
import com.example.expensetracker.data.Transaction
import com.example.expensetracker.databinding.ActivityAddTransactionBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AddTransactionFragment : BottomSheetDialogFragment() {

    private lateinit var binding: ActivityAddTransactionBinding

    // Callback để gửi dữ liệu về HomeFragment sau khi lưu
    var onSaveClick: ((Double, String, String, String) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ActivityAddTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Cài đặt Dropdown Danh mục
        val categories = listOf("Ăn uống", "Đi lại", "Mua sắm", "Giải trí", "Lương", "Thưởng", "Khác")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, categories)
        binding.autoCompleteCategory.setAdapter(adapter)

        // 2. Xử lý nút Lưu
        val transactionArg = arguments?.getSerializable("transaction_data") as? Transaction

        if (transactionArg != null) {
            // --- TRƯỜNG HỢP: SỬA (UPDATE) ---
            binding.tvTitle.text = "Chỉnh sửa giao dịch"

            // Điền dữ liệu cũ vào ô
            binding.etAmount.setText(transactionArg.amount.toString().replace(".0", ""))
            binding.etNote.setText(transactionArg.note)
            binding.autoCompleteCategory.setText(transactionArg.category, false)

            if (transactionArg.type == 1) binding.chipIncome.isChecked = true
            else binding.chipExpense.isChecked = true
        } else {
            // --- TRƯỜNG HỢP: THÊM MỚI (ADD) ---
            binding.tvTitle.text = "Thêm Giao dịch mới"
        }

        // =========================================================
        // 3. XỬ LÝ SỰ KIỆN NÚT BẤM
        // =========================================================

        // Nút HỦY -> Đóng luôn
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        // Nút LƯU / CẬP NHẬT
        binding.btnSaveTransaction.setOnClickListener {
            val amountStr = binding.etAmount.text.toString()

            if (amountStr.isNotEmpty()) {
                val amount = amountStr.toDouble()
                val note = binding.etNote.text.toString()
                val category = binding.autoCompleteCategory.text.toString()
                val type = if (binding.chipIncome.isChecked) "Thu nhập" else "Chi tiêu"

                // Gửi dữ liệu về HomeFragment
                onSaveClick?.invoke(amount, type, category, note)
                dismiss()
            } else {
                binding.layoutAmount.error = "Vui lòng nhập số tiền"
            }
        }

        // 3. Xử lý chọn ngày
        binding.etDate.setOnClickListener {
            // 1. Lấy ngày tháng năm hiện tại để hiển thị mặc định trong lịch
            val calendar = java.util.Calendar.getInstance()
            val year = calendar.get(java.util.Calendar.YEAR)
            val month = calendar.get(java.util.Calendar.MONTH)
            val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)

            // 2. Tạo DatePickerDialog
            val datePickerDialog = android.app.DatePickerDialog(
                requireContext(),
                { _, selectedYear, selectedMonth, selectedDay ->
                    // 3. Xử lý khi người dùng chọn xong ngày
                    // Tạo một Calendar mới để set ngày vừa chọn (vì selectedMonth chạy từ 0-11)
                    val selectedCalendar = java.util.Calendar.getInstance()
                    selectedCalendar.set(selectedYear, selectedMonth, selectedDay)

                    // 4. Định dạng thành chuỗi "dd/MM/yyyy" (Ví dụ: 09/01/2026)
                    val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                    val formattedDate = dateFormat.format(selectedCalendar.time)

                    // 5. Gán vào ô nhập liệu
                    binding.etDate.setText(formattedDate)
                },
                year, month, day
            )

            // 6. Hiển thị lên
            datePickerDialog.show()
        }
    }

    // Làm cho nền trong suốt để thấy được bo góc
    override fun getTheme(): Int {
        return R.style.CustomBottomSheetDialog
    }
}