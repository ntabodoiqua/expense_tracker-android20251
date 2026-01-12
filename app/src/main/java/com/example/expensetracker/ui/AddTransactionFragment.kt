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
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class AddTransactionFragment : BottomSheetDialogFragment() {

    private lateinit var binding: ActivityAddTransactionBinding
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

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

        // 1. Cài đặt Dropdown Danh mục với layout đẹp hơn
        val categories = listOf("Ăn uống", "Đi lại", "Mua sắm", "Giải trí", "Lương", "Thưởng", "Khác")
        val adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, categories)
        binding.autoCompleteCategory.setAdapter(adapter)

        // 2. Set ngày mặc định là hôm nay
        binding.etDate.setText(dateFormat.format(calendar.time))

        // 3. Xử lý dữ liệu truyền vào (nếu có - trường hợp sửa)
        val transactionArg = arguments?.getSerializable("transaction_data") as? Transaction

        if (transactionArg != null) {
            // --- TRƯỜNG HỢP: SỬA (UPDATE) ---
            binding.tvTitle.text = "✏️ Chỉnh sửa giao dịch"
            binding.btnSaveTransaction.text = "💾 Cập nhật"

            // Điền dữ liệu cũ vào ô
            binding.etAmount.setText(transactionArg.amount.toLong().toString())
            binding.etNote.setText(transactionArg.note)
            binding.autoCompleteCategory.setText(transactionArg.category, false)

            // Set ngày từ dữ liệu cũ
            binding.etDate.setText(dateFormat.format(transactionArg.date))

            if (transactionArg.type == 1) binding.chipIncome.isChecked = true
            else binding.chipExpense.isChecked = true
        } else {
            // --- TRƯỜNG HỢP: THÊM MỚI (ADD) ---
            binding.tvTitle.text = "➕ Thêm giao dịch mới"
            binding.btnSaveTransaction.text = "💾 Lưu"
        }

        // =========================================================
        // 4. XỬ LÝ SỰ KIỆN NÚT BẤM
        // =========================================================

        // Nút HỦY -> Đóng luôn
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        // Nút LƯU / CẬP NHẬT
        binding.btnSaveTransaction.setOnClickListener {
            val amountStr = binding.etAmount.text.toString().trim()
            val category = binding.autoCompleteCategory.text.toString().trim()

            // Validate
            var isValid = true

            if (amountStr.isEmpty() || amountStr.toDoubleOrNull() == null || amountStr.toDouble() <= 0) {
                binding.layoutAmount.error = "Vui lòng nhập số tiền hợp lệ"
                isValid = false
            } else {
                binding.layoutAmount.error = null
            }

            if (category.isEmpty()) {
                binding.layoutCategory.error = "Vui lòng chọn danh mục"
                isValid = false
            } else {
                binding.layoutCategory.error = null
            }

            if (isValid) {
                val amount = amountStr.toDouble()
                val note = binding.etNote.text.toString().trim()
                val type = if (binding.chipIncome.isChecked) "Thu nhập" else "Chi tiêu"

                // Gửi dữ liệu về HomeFragment
                onSaveClick?.invoke(amount, type, category, note)
                dismiss()
            }
        }

        // 5. Xử lý chọn ngày
        binding.etDate.setOnClickListener {
            showDatePicker()
        }

        // Click vào icon cũng mở DatePicker
        binding.layoutDate.setEndIconOnClickListener {
            showDatePicker()
        }
    }

    private fun showDatePicker() {
        // Tạo constraint để không cho chọn ngày tương lai
        val constraintsBuilder = CalendarConstraints.Builder()
            .setValidator(DateValidatorPointBackward.now())

        // Tạo Material DatePicker
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Chọn ngày giao dịch")
            .setSelection(calendar.timeInMillis)
            .setCalendarConstraints(constraintsBuilder.build())
            .setTheme(R.style.CustomMaterialDatePicker)
            .build()

        // Xử lý khi chọn ngày
        datePicker.addOnPositiveButtonClickListener { selection ->
            // selection là UTC timestamp, cần convert về local
            val utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            utcCalendar.timeInMillis = selection
            
            calendar.set(
                utcCalendar.get(Calendar.YEAR),
                utcCalendar.get(Calendar.MONTH),
                utcCalendar.get(Calendar.DAY_OF_MONTH)
            )
            binding.etDate.setText(dateFormat.format(calendar.time))
        }

        datePicker.show(parentFragmentManager, "DATE_PICKER")
    }

    // Làm cho nền trong suốt để thấy được bo góc
    override fun getTheme(): Int {
        return R.style.CustomBottomSheetDialog
    }
}