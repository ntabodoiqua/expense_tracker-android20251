package com.example.expensetracker.ui

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import com.example.expensetracker.R
import com.example.expensetracker.data.Transaction
import com.example.expensetracker.databinding.ActivityAddTransactionBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class AddTransactionFragment : BottomSheetDialogFragment() {

    private lateinit var binding: ActivityAddTransactionBinding
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val amountFormatter = DecimalFormat("#,###")
    private var isFormattingAmount = false

    // Danh mục theo loại giao dịch
    private val expenseCategories = listOf(
        "Ăn uống",
        "Đi lại", 
        "Mua sắm",
        "Giải trí",
        "Học tập",
        "Y tế",
        "Nhà ở",
        "Điện nước",
        "Internet",
        "Khác"
    )
    
    private val incomeCategories = listOf(
        "Lương",
        "Thưởng",
        "Đầu tư",
        "Làm thêm",
        "Kinh doanh",
        "Quà tặng",
        "Khác"
    )

    // Callback để gửi dữ liệu về HomeFragment sau khi lưu
    var onSaveClick: ((Double, String, String, String, Long) -> Unit)? = null

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

        // 1. Setup ẩn bàn phím khi touch ra ngoài
        setupHideKeyboardOnOutsideTouch()

        // 2. Setup format số tiền với dấu phân cách
        setupAmountFormatting()

        // 3. Set ngày mặc định là hôm nay
        binding.etDate.setText(dateFormat.format(calendar.time))

        // 4. Xử lý logic chọn chip (chỉ cho phép 1 chip được chọn)
        setupChipSelection()

        // 5. Xử lý dữ liệu truyền vào (nếu có - trường hợp sửa)
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

            if (transactionArg.type == 1) {
                binding.chipIncome.isChecked = true
                binding.chipExpense.isChecked = false
                updateCategoryDropdown(true)
            } else {
                binding.chipExpense.isChecked = true
                binding.chipIncome.isChecked = false
                updateCategoryDropdown(false)
            }
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

            if (amountStr.isEmpty()) {
                binding.layoutAmount.error = "Vui lòng nhập số tiền"
                isValid = false
            } else {
                // Xóa dấu phân cách trước khi parse
                val cleanAmount = amountStr.replace(".", "").replace(",", "")
                val amount = cleanAmount.toDoubleOrNull()
                if (amount == null || amount <= 0) {
                    binding.layoutAmount.error = "Số tiền không hợp lệ"
                    isValid = false
                } else {
                    binding.layoutAmount.error = null
                }
            }

            if (category.isEmpty()) {
                binding.layoutCategory.error = "Vui lòng chọn danh mục"
                isValid = false
            } else {
                binding.layoutCategory.error = null
            }

            if (isValid) {
                // Xóa dấu phân cách trước khi parse
                val cleanAmount = amountStr.replace(".", "").replace(",", "")
                val amount = cleanAmount.toDouble()
                val note = binding.etNote.text.toString().trim()
                val type = if (binding.chipIncome.isChecked) "Thu nhập" else "Chi tiêu"

                // Gửi dữ liệu về HomeFragment (bao gồm timestamp của ngày đã chọn)
                onSaveClick?.invoke(amount, type, category, note, calendar.timeInMillis)
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

    // Xử lý logic chọn chip Thu nhập / Chi tiêu
    private fun setupChipSelection() {
        // Mặc định chọn Chi tiêu
        binding.chipExpense.isChecked = true
        updateCategoryDropdown(false)
        
        binding.chipExpense.setOnClickListener {
            binding.chipExpense.isChecked = true
            binding.chipIncome.isChecked = false
            updateCategoryDropdown(false) // Chi tiêu
        }

        binding.chipIncome.setOnClickListener {
            binding.chipIncome.isChecked = true
            binding.chipExpense.isChecked = false
            updateCategoryDropdown(true) // Thu nhập
        }
    }
    
    // Cập nhật danh sách danh mục theo loại giao dịch
    private fun updateCategoryDropdown(isIncome: Boolean) {
        val categories = if (isIncome) incomeCategories else expenseCategories
        val adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, categories)
        binding.autoCompleteCategory.setAdapter(adapter)
        
        // Clear selection cũ nếu không hợp lệ với loại mới
        val currentCategory = binding.autoCompleteCategory.text.toString()
        if (currentCategory.isNotEmpty() && !categories.contains(currentCategory)) {
            binding.autoCompleteCategory.setText("", false)
        }
    }

    // Ẩn bàn phím khi touch ra ngoài các input
    private fun setupHideKeyboardOnOutsideTouch() {
        binding.root.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val currentFocus = dialog?.currentFocus
                if (currentFocus != null) {
                    val inputMethodManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    inputMethodManager.hideSoftInputFromWindow(currentFocus.windowToken, 0)
                    currentFocus.clearFocus()
                }
            }
            false
        }
    }

    // Format số tiền với dấu phân cách hàng nghìn
    private fun setupAmountFormatting() {
        binding.etAmount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isFormattingAmount) return

                isFormattingAmount = true
                
                val originalString = s.toString()
                
                // Xóa tất cả dấu phân cách cũ
                val cleanString = originalString.replace(".", "").replace(",", "")
                
                if (cleanString.isNotEmpty()) {
                    try {
                        val parsed = cleanString.toLong()
                        val formatted = amountFormatter.format(parsed)
                        
                        binding.etAmount.removeTextChangedListener(this)
                        binding.etAmount.setText(formatted)
                        binding.etAmount.setSelection(formatted.length)
                        binding.etAmount.addTextChangedListener(this)
                    } catch (e: NumberFormatException) {
                        // Nếu số quá lớn, giữ nguyên
                    }
                }
                
                isFormattingAmount = false
            }
        })
    }

    // Làm cho nền trong suốt để thấy được bo góc
    override fun getTheme(): Int {
        return R.style.CustomBottomSheetDialog
    }
}