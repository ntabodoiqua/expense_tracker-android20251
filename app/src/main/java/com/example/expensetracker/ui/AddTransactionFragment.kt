package com.example.expensetracker.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.PopupMenu
import android.widget.Toast
import com.example.expensetracker.R
import com.example.expensetracker.data.Transaction
import com.example.expensetracker.databinding.ActivityAddTransactionBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AddTransactionFragment : BottomSheetDialogFragment() {
    private lateinit var binding: ActivityAddTransactionBinding
    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private var selectedDate: Long = System.currentTimeMillis()
    private var isIncomeState = false
    var onSaveClick: ((Double, String, String, String, Long) -> Unit)? = null
    private val expenseCategories = listOf("Ăn uống", "Đi lại", "Mua sắm", "Giải trí", "Tiền nhà", "Hóa đơn", "Y tế", "Giáo dục", "Khác")
    private val incomeCategories = listOf("Lương", "Thưởng", "Lãi tiết kiệm", "Bán hàng", "Quà tặng", "Khác")

    // Khởi tạo giao diện từ XML binding
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = ActivityAddTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Cấu hình BottomSheet mở toàn màn hình
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as FrameLayout?
            bottomSheet?.let { sheet ->
                val behavior = BottomSheetBehavior.from(sheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }
        return dialog
    }

    // Thiết lập logic và sự kiện cho các thành phần UI
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val transactionArg = arguments?.getSerializable("transaction_data") as? Transaction
        if (transactionArg != null) {
            binding.tvTitle.text = "Sửa giao dịch"
            binding.btnSaveTransaction.text = "Cập nhật"
            binding.etAmount.setText(transactionArg.amount.toLong().toString())
            binding.etNote.setText(transactionArg.note)
            selectedDate = transactionArg.date
            val isIncome = (transactionArg.type == 1)
            setTransactionType(isIncome)
            binding.tvCategory.text = transactionArg.category
        } else {
            binding.tvTitle.text = "Thêm giao dịch"
            binding.btnSaveTransaction.text = "Lưu"
            setTransactionType(false)
            selectedDate = System.currentTimeMillis()
        }
        updateDateDisplay()
        binding.chipExpense.setOnClickListener { setTransactionType(false) }
        binding.chipIncome.setOnClickListener { setTransactionType(true) }
        binding.tvCategory.setOnClickListener { showCategoryMenu(it) }
        binding.tvDate.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Chọn ngày giao dịch")
                .setSelection(selectedDate)
                .setTheme(R.style.CustomMaterialDatePicker)
                .build()
            datePicker.addOnPositiveButtonClickListener { selection ->
                selectedDate = selection
                updateDateDisplay()
            }
            datePicker.show(parentFragmentManager, "DATE_PICKER")
        }
        binding.btnCancelTransaction.setOnClickListener { dismiss() }
        binding.btnSaveTransaction.setOnClickListener {
            val amountStr = binding.etAmount.text.toString().trim()
            val note = binding.etNote.text.toString().trim()
            if (amountStr.isEmpty()) {
                binding.etAmount.error = "Vui lòng nhập số tiền"
                binding.etAmount.requestFocus()
                return@setOnClickListener
            }
            val category = binding.tvCategory.text.toString()
            if (category.isEmpty() || category == "Loại giao dịch") {
                Toast.makeText(context, "Vui lòng chọn danh mục", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val amount = amountStr.toDoubleOrNull() ?: 0.0
            if (amount <= 0) {
                binding.etAmount.error = "Số tiền phải lớn hơn 0"
                return@setOnClickListener
            }
            val typeStr = if (isIncomeState) "Thu nhập" else "Chi tiêu"
            onSaveClick?.invoke(amount, typeStr, category, note, selectedDate)
            dismiss()
        }
    }

    // Cập nhật loại giao dịch (Thu/Chi) và danh mục tương ứng
    private fun setTransactionType(isIncome: Boolean) {
        isIncomeState = isIncome
        binding.chipIncome.isChecked = isIncome
        binding.chipExpense.isChecked = !isIncome
        val currentCategory = binding.tvCategory.text.toString()
        val targetList = if (isIncome) incomeCategories else expenseCategories
        if (!targetList.contains(currentCategory)) {
            binding.tvCategory.text = targetList[0]
        }
    }

    // Hiển thị menu chọn danh mục
    private fun showCategoryMenu(view: View) {
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

    // Cập nhật hiển thị ngày đã chọn
    private fun updateDateDisplay() {
        binding.tvDate.text = dateFormatter.format(Date(selectedDate))
    }

    // Áp dụng theme tùy chỉnh cho BottomSheet
    override fun getTheme(): Int {
        return R.style.CustomBottomSheetDialog
    }
}
