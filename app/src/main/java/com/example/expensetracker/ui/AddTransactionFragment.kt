package com.example.expensetracker.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.expensetracker.R
import com.example.expensetracker.data.Transaction
import com.example.expensetracker.databinding.ActivityAddTransactionBinding // Hoặc LayoutAddTransactionBottomSheetBinding tùy tên file xml của bạn
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AddTransactionFragment : BottomSheetDialogFragment() {

    private lateinit var binding: ActivityAddTransactionBinding

    var onSaveClick: ((Double, String, String, String, Long) -> Unit)? = null
    private var isIncomeState = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ActivityAddTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. DỮ LIỆU DANH MỤC
        val expenseCategories = listOf("Ăn uống", "Đi lại", "Mua sắm", "Giải trí", "Tiền nhà", "Khác")
        val incomeCategories = listOf("Lương", "Thưởng", "Lãi tiết kiệm", "Đầu tư", "Khác")
        val dateFormatter = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())

        // Biến cờ để tránh reset danh mục khi vừa mở form Sửa (Update)
        var isFirstLoad = true

        // 2. LOGIC TỰ ĐỘNG ĐỔI DANH MỤC KHI CHỌN CHIP
        binding.chipGroupType.setOnCheckedChangeListener { _, checkedId ->
            val isIncome = (checkedId == R.id.chipIncome)

            // Lấy list tương ứng
            val list = if (isIncome) incomeCategories else expenseCategories

            // Nếu không phải là lần load đầu tiên (người dùng tự bấm đổi) thì mới reset về mục đầu
            if (!isFirstLoad) {
                binding.tvCategory.text = list[0]
            }
            // Sau khi chạy xong lần đầu thì tắt cờ đi
            isFirstLoad = false
        }

        // --- HÀM HỖ TRỢ: Hiện Menu ---
        fun showCategoryMenu(view: View) {
            val popup = android.widget.PopupMenu(requireContext(), view)

            // DÙNG BIẾN isIncomeState ĐỂ QUYẾT ĐỊNH LIST NÀO (Không hỏi Chip nữa)
            val list = if (isIncomeState) incomeCategories else expenseCategories

            for (item in list) popup.menu.add(item)

            popup.setOnMenuItemClickListener { item ->
                binding.tvCategory.text = item.title
                true
            }
            popup.show()
        }

        // 2. CÀI ĐẶT SỰ KIỆN CLICK
        binding.tvCategory.setOnClickListener { showCategoryMenu(it) }
        binding.btnCancelTransaction.setOnClickListener { dismiss() }

        // 3. LOGIC LOAD DỮ LIỆU CŨ (SỬA / THÊM)
        val transactionArg = arguments?.getSerializable("transaction_data") as? Transaction

        if (transactionArg != null) {
            // --- UPDATE MODE ---
            binding.tvTitle.text = "Chỉnh sửa giao dịch"
            binding.btnSaveTransaction.text = "Cập nhật"

            binding.etAmount.setText(transactionArg.amount.toString().replace(".0", ""))
            binding.etNote.setText(transactionArg.note)
            binding.tvCategory.text = transactionArg.category
            binding.tvDate.text = dateFormatter.format(java.util.Date(transactionArg.date))

            // Set đúng Chip (Sẽ kích hoạt listener ở trên)
            if (transactionArg.type == 1) {
                binding.chipGroupType.check(R.id.chipIncome)
            } else {
                binding.chipGroupType.check(R.id.chipExpense)
            }

            // Set lại danh mục cũ sau khi gọi hàm đó
            binding.tvCategory.text = transactionArg.category

        } else {
            // --- ADD MODE ---
            binding.tvTitle.text = "Thêm giao dịch mới"
            binding.btnSaveTransaction.text = "Thêm mới"
            binding.tvDate.text = dateFormatter.format(java.util.Date())

            isFirstLoad = false // Add mode thì không cần giữ giá trị cũ, cứ reset thoải mái
            binding.chipGroupType.check(R.id.chipExpense)
            binding.tvCategory.text = expenseCategories[0]
        }

        // 4. XỬ LÝ NÚT LƯU
        binding.btnSaveTransaction.setOnClickListener {
            val amountStr = binding.etAmount.text.toString()
            if (amountStr.isNotEmpty()) {
                val amount = amountStr.toDouble()
                val note = binding.etNote.text.toString()
                val category = binding.tvCategory.text.toString()
                val dateStr = binding.tvDate.text.toString()
                val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())

                // Chuyển String -> Long (Nếu lỗi thì lấy giờ hiện tại)
                val dateLong = try {
                    dateFormat.parse(dateStr)?.time ?: System.currentTimeMillis()
                } catch (e: Exception) {
                    System.currentTimeMillis()
                }
                val isIncome = (binding.chipGroupType.checkedChipId == R.id.chipIncome)
                val type = if (isIncome) "Thu nhập" else "Chi tiêu"

                onSaveClick?.invoke(amount, type, category, note, dateLong)
                dismiss()
            } else {
                binding.etAmount.error = "Vui lòng nhập số tiền"
                binding.etAmount.requestFocus()
            }
        }

        // 5. XỬ LÝ CHỌN NGÀY
        binding.tvDate.setOnClickListener {
            val calendar = java.util.Calendar.getInstance()
            try {
                val currentDate = dateFormatter.parse(binding.tvDate.text.toString())
                if (currentDate != null) calendar.time = currentDate
            } catch (e: Exception) { }

            val datePickerDialog = android.app.DatePickerDialog(
                requireContext(),
                { _, y, m, d ->
                    val selCal = java.util.Calendar.getInstance()
                    selCal.set(y, m, d)
                    binding.tvDate.text = dateFormatter.format(selCal.time)
                },
                calendar.get(java.util.Calendar.YEAR),
                calendar.get(java.util.Calendar.MONTH),
                calendar.get(java.util.Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }
    }

    override fun getTheme(): Int {
        return R.style.CustomBottomSheetDialog
    }
}