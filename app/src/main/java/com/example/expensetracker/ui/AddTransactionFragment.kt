package com.example.expensetracker.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import com.example.expensetracker.R
import com.example.expensetracker.data.Transaction
import com.example.expensetracker.databinding.ActivityAddTransactionBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddTransactionFragment : BottomSheetDialogFragment() {
    private lateinit var binding: ActivityAddTransactionBinding
    private val decimalFormat = DecimalFormat("#,###", DecimalFormatSymbols(Locale.US).apply {
        groupingSeparator = '.'
    })
    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private var selectedDateCalendar = Calendar.getInstance()
    private var isIncomeState = false
    var onSaveClick: ((Double, String, String, String, Long) -> Unit)? = null

    // Khởi tạo view binding cho fragment
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ActivityAddTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Thiết lập giao diện và xử lý sự kiện sau khi view được tạo
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val expenseCategories = listOf("Ăn uống", "Đi lại", "Mua sắm", "Giải trí", "Tiền nhà", "Hóa đơn", "Y tế", "Giáo dục", "Khác")
        val incomeCategories = listOf("Lương", "Thưởng", "Lãi tiết kiệm", "Bán hàng", "Quà tặng", "Khác")
        binding.etAmount.inputType = InputType.TYPE_CLASS_NUMBER

        // Chuyển đổi loại giao dịch giữa Thu nhập và Chi tiêu
        fun setTransactionType(isIncome: Boolean) {
            isIncomeState = isIncome
            binding.chipIncome.isChecked = isIncome
            binding.chipExpense.isChecked = !isIncome
            val currentList = if (isIncome) incomeCategories else expenseCategories
            binding.tvCategory.text = currentList[0]
        }

        // Hiển thị popup menu chọn danh mục
        fun showCategoryMenu(view: View) {
            val popup = PopupMenu(requireContext(), view)
            val list = if (isIncomeState) incomeCategories else expenseCategories
            for (item in list) {
                popup.menu.add(item)
            }
            popup.setOnMenuItemClickListener { item ->
                binding.tvCategory.text = item.title
                true
            }
            popup.show()
        }

        binding.chipExpense.setOnClickListener { setTransactionType(false) }
        binding.chipIncome.setOnClickListener { setTransactionType(true) }
        binding.tvCategory.setOnClickListener { showCategoryMenu(it) }
        binding.tvDate.setOnClickListener {
            val year = selectedDateCalendar.get(Calendar.YEAR)
            val month = selectedDateCalendar.get(Calendar.MONTH)
            val day = selectedDateCalendar.get(Calendar.DAY_OF_MONTH)
            val datePickerDialog = DatePickerDialog(requireContext(), { _, y, m, d ->
                selectedDateCalendar.set(y, m, d)
                binding.tvDate.text = dateFormatter.format(selectedDateCalendar.time)
            }, year, month, day)
            datePickerDialog.show()
        }
        binding.btnCancelTransaction.setOnClickListener { dismiss() }

        // TextWatcher để format số tiền với dấu chấm phân cách hàng nghìn
        binding.etAmount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                binding.etAmount.removeTextChangedListener(this)
                try {
                    var originalString = s.toString()
                    if (originalString.isEmpty()) {
                        binding.etAmount.addTextChangedListener(this)
                        return
                    }
                    val cursorPosition = binding.etAmount.selectionStart
                    val dotsBeforeCursor = originalString.substring(0, cursorPosition).count { it == '.' }
                    val cleanString = originalString.replace(".", "")
                    if (cleanString.isNotEmpty()) {
                        val longval: Long = cleanString.toLong()
                        val formattedString = decimalFormat.format(longval)
                        binding.etAmount.setText(formattedString)
                        val newDotsBeforeCursor = formattedString.substring(0,
                            minOf(cursorPosition, formattedString.length)).count { it == '.' }
                        val newCursorPosition = cursorPosition + (newDotsBeforeCursor - dotsBeforeCursor)
                        binding.etAmount.setSelection(minOf(maxOf(newCursorPosition, 0), formattedString.length))
                    }
                } catch (nfe: NumberFormatException) {}
                binding.etAmount.addTextChangedListener(this)
            }
        })

        val transactionArg = arguments?.getSerializable("transaction_data") as? Transaction
        if (transactionArg != null) {
            binding.tvTitle.text = "Chỉnh sửa giao dịch"
            binding.btnSaveTransaction.text = "Cập nhật"
            binding.etAmount.setText(decimalFormat.format(transactionArg.amount))
            binding.etNote.setText(transactionArg.note)
            selectedDateCalendar.timeInMillis = transactionArg.date
            binding.tvDate.text = dateFormatter.format(selectedDateCalendar.time)
            val isIncome = (transactionArg.type == 1)
            setTransactionType(isIncome)
            binding.tvCategory.text = transactionArg.category
        } else {
            binding.tvTitle.text = "Thêm giao dịch mới"
            binding.btnSaveTransaction.text = "Lưu"
            setTransactionType(false)
            binding.tvDate.text = dateFormatter.format(selectedDateCalendar.time)
        }

        // Xử lý lưu giao dịch khi nhấn nút Save
        binding.btnSaveTransaction.setOnClickListener {
            val rawAmount = binding.etAmount.text.toString()
                .replace(",", "")
                .replace(".", "")
                .replace(" ", "")
            if (rawAmount.isNotEmpty()) {
                val amount = rawAmount.toDouble()
                val note = binding.etNote.text.toString()
                val category = binding.tvCategory.text.toString()
                val typeStr = if (isIncomeState) "Thu nhập" else "Chi tiêu"
                val dateLong = selectedDateCalendar.timeInMillis
                onSaveClick?.invoke(amount, typeStr, category, note, dateLong)
                dismiss()
            } else {
                binding.etAmount.error = "Vui lòng nhập số tiền"
                binding.etAmount.requestFocus()
            }
        }
    }

    // Áp dụng style bo góc cho BottomSheet
    override fun getTheme(): Int {
        return R.style.CustomBottomSheetDialog
    }
}
