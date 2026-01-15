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

    // Format tiền tệ và ngày tháng - Dùng dấu chấm phân cách hàng nghìn kiểu VN
    private val decimalFormat = DecimalFormat("#,###", DecimalFormatSymbols(Locale.US).apply {
        groupingSeparator = '.'
    })
    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private var selectedDateCalendar = Calendar.getInstance()

    // Biến trạng thái quan trọng: Xác định đang là Thu hay Chi
    private var isIncomeState = false

    // Callback gửi dữ liệu về HomeFragment (Amount, Type, Category, Note, Date)
    var onSaveClick: ((Double, String, String, String, Long) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Đảm bảo tên class Binding khớp với tên file layout XML của bạn
        // Nếu file xml tên là layout_add_transaction.xml thì đổi thành LayoutAddTransactionBinding
        binding = ActivityAddTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. CẤU HÌNH DỮ LIỆU & GIAO DIỆN BAN ĐẦU
        val expenseCategories = listOf("Ăn uống", "Đi lại", "Mua sắm", "Giải trí", "Tiền nhà", "Hóa đơn", "Y tế", "Giáo dục", "Khác")
        val incomeCategories = listOf("Lương", "Thưởng", "Lãi tiết kiệm", "Bán hàng", "Quà tặng", "Khác")

        // Cấu hình bàn phím số cho ô nhập tiền - chỉ cho phép số nguyên
        binding.etAmount.inputType = InputType.TYPE_CLASS_NUMBER

        // --- HÀM LOGIC: Đổi loại giao dịch (Chi tiêu <-> Thu nhập) ---
        fun setTransactionType(isIncome: Boolean) {
            isIncomeState = isIncome

            // Cập nhật giao diện Chip
            // Lưu ý: Vì dùng LinearLayout, ta set trạng thái checked thủ công
            binding.chipIncome.isChecked = isIncome
            binding.chipExpense.isChecked = !isIncome

            // Reset danh mục về mục đầu tiên của loại tương ứng
            val currentList = if (isIncome) incomeCategories else expenseCategories
            binding.tvCategory.text = currentList[0]
        }

        // --- HÀM LOGIC: Hiện Menu chọn danh mục ---
        fun showCategoryMenu(view: View) {
            val popup = PopupMenu(requireContext(), view)
            // Lấy list dựa trên biến trạng thái isIncomeState
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

        // 2. XỬ LÝ SỰ KIỆN CLICK (LISTENER)

        // Click chọn loại (Chip)
        binding.chipExpense.setOnClickListener { setTransactionType(false) }
        binding.chipIncome.setOnClickListener { setTransactionType(true) }

        // Click chọn Danh mục -> Hiện Menu
        binding.tvCategory.setOnClickListener { showCategoryMenu(it) }

        // Click chọn Ngày -> Hiện Lịch
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

        // Click Hủy -> Đóng
        binding.btnCancelTransaction.setOnClickListener { dismiss() }

        // 3. XỬ LÝ FORMAT SỐ TIỀN (Thêm dấu chấm phân cách hàng nghìn khi nhập)
        binding.etAmount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                binding.etAmount.removeTextChangedListener(this)

                try {
                    var originalString = s.toString()
                    
                    // Nếu chuỗi rỗng, không làm gì
                    if (originalString.isEmpty()) {
                        binding.etAmount.addTextChangedListener(this)
                        return
                    }
                    
                    // Lưu lại vị trí con trỏ hiện tại
                    val cursorPosition = binding.etAmount.selectionStart
                    
                    // Đếm số dấu chấm trước vị trí con trỏ (để tính toán lại vị trí sau khi format)
                    val dotsBeforeCursor = originalString.substring(0, cursorPosition).count { it == '.' }
                    
                    // Loại bỏ tất cả dấu chấm để lấy số thuần
                    val cleanString = originalString.replace(".", "")
                    
                    // Nếu có số, format nó
                    if (cleanString.isNotEmpty()) {
                        val longval: Long = cleanString.toLong()
                        val formattedString = decimalFormat.format(longval)
                        
                        // Set text mới
                        binding.etAmount.setText(formattedString)
                        
                        // Tính toán lại vị trí con trỏ sau khi format
                        val newDotsBeforeCursor = formattedString.substring(0, 
                            minOf(cursorPosition, formattedString.length)).count { it == '.' }
                        val newCursorPosition = cursorPosition + (newDotsBeforeCursor - dotsBeforeCursor)
                        
                        // Đặt con trỏ về đúng vị trí
                        binding.etAmount.setSelection(minOf(maxOf(newCursorPosition, 0), formattedString.length))
                    }
                    
                } catch (nfe: NumberFormatException) {
                    // Nếu lỗi, giữ nguyên text
                }

                binding.etAmount.addTextChangedListener(this)
            }
        })

        // 4. KIỂM TRA: CHẾ ĐỘ SỬA (UPDATE) HAY THÊM MỚI (ADD)?
        val transactionArg = arguments?.getSerializable("transaction_data") as? Transaction

        if (transactionArg != null) {
            // --- MODE: SỬA ---
            binding.tvTitle.text = "Chỉnh sửa giao dịch"
            binding.btnSaveTransaction.text = "Cập nhật"

            // Điền dữ liệu cũ
            // Format số tiền có dấu phẩy
            binding.etAmount.setText(decimalFormat.format(transactionArg.amount))
            binding.etNote.setText(transactionArg.note)

            // Set ngày cũ
            selectedDateCalendar.timeInMillis = transactionArg.date
            binding.tvDate.text = dateFormatter.format(selectedDateCalendar.time)

            // Set loại và danh mục
            val isIncome = (transactionArg.type == 1)
            setTransactionType(isIncome)

            // Ghi đè lại danh mục cũ
            binding.tvCategory.text = transactionArg.category

        } else {
            // --- MODE: THÊM MỚI ---
            binding.tvTitle.text = "Thêm giao dịch mới"
            binding.btnSaveTransaction.text = "Lưu"

            // Mặc định: Chi tiêu, Ngày hôm nay
            setTransactionType(false)
            binding.tvDate.text = dateFormatter.format(selectedDateCalendar.time)
        }

        // 5. XỬ LÝ NÚT LƯU
        binding.btnSaveTransaction.setOnClickListener {
            // Lấy số tiền và loại bỏ dấu phẩy để parse thành Double
            val rawAmount = binding.etAmount.text.toString()
                .replace(",", "")
                .replace(".", "")
                .replace(" ", "")

            if (rawAmount.isNotEmpty()) {
                val amount = rawAmount.toDouble()
                val note = binding.etNote.text.toString()
                val category = binding.tvCategory.text.toString()

                // Quyết định loại dựa trên biến trạng thái
                val typeStr = if (isIncomeState) "Thu nhập" else "Chi tiêu"

                // Lấy thời gian từ biến Calendar đã chọn
                val dateLong = selectedDateCalendar.timeInMillis

                // Gửi dữ liệu về (Callback)
                onSaveClick?.invoke(amount, typeStr, category, note, dateLong)
                dismiss()
            } else {
                binding.etAmount.error = "Vui lòng nhập số tiền"
                binding.etAmount.requestFocus()
            }
        }
    }

    // Áp dụng Style bo góc cho BottomSheet
    override fun getTheme(): Int {
        return R.style.CustomBottomSheetDialog
    }
}