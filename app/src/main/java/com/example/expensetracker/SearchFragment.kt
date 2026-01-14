package com.example.expensetracker

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.PopupMenu
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.expensetracker.data.Transaction
import com.example.expensetracker.databinding.FragmentSearchBinding
import com.example.expensetracker.ui.TransactionAdapter
import com.example.expensetracker.viewmodel.TransactionViewModel
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.*

class SearchFragment : Fragment() {
    private lateinit var binding: FragmentSearchBinding
    private lateinit var viewModel: TransactionViewModel
    private lateinit var adapter: TransactionAdapter
    private var fullList: List<Transaction> = emptyList()

    // Biến lưu trạng thái bộ lọc (để lưu trữ tạm thời các giá trị ngày tháng)
    private var fromDate: Long? = null
    private var toDate: Long? = null

    // Các biến khác sẽ đọc trực tiếp từ UI khi cần lọc

    // Danh sách danh mục
    private val allCategories = listOf(
        "Tất cả",
        "Ăn uống", "Đi lại", "Mua sắm", "Giải trí", "Tiền nhà", "Hóa đơn", "Y tế", "Giáo dục",
        "Lương", "Thưởng", "Lãi tiết kiệm", "Bán hàng", "Quà tặng", "Khác"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupHideKeyboardOnOutsideTouch()
        viewModel = ViewModelProvider(this)[TransactionViewModel::class.java]

        adapter = TransactionAdapter()
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        // Setup các thành phần giao diện
        setupSearchView()
        setupCategoryFilter()
        setupTypeFilter()
        setupDatePickers()
        setupFilterButtons() // Nút Áp dụng & Xóa

        // Quan sát dữ liệu
        viewModel.allTransactions.observe(viewLifecycleOwner) { list ->
            list?.let {
                fullList = it
                // Chỉ lọc khi đã có dữ liệu
                executeFilter(showToast = false)
            }
        }
    }

    // --- 1. SEARCH VIEW (Lọc ngay khi gõ) ---
    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                hideKeyboard()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Gọi hàm lọc tổng hợp ngay lập tức
                executeFilter(showToast = false)
                return true
            }
        })
    }

    // --- 2. CATEGORY FILTER (Lọc ngay khi chọn) ---
    private fun setupCategoryFilter() {
        binding.tvCategoryFilter.text = "Tất cả danh mục"

        binding.tvCategoryFilter.setOnClickListener { view ->
            val popup = PopupMenu(requireContext(), view)
            allCategories.forEach { popup.menu.add(it) }

            popup.setOnMenuItemClickListener { item ->
                binding.tvCategoryFilter.text = if (item.title == "Tất cả") "Tất cả danh mục" else item.title
                // Lọc ngay lập tức
                executeFilter(showToast = true)
                true
            }
            popup.show()
        }
    }

    // --- 3. TYPE FILTER (Lọc ngay khi bấm Chip) ---
    private fun setupTypeFilter() {
        binding.chipGroupType.setOnCheckedStateChangeListener { _, _ ->
            // Lọc ngay lập tức
            executeFilter(showToast = true)
        }
    }

    // --- 4. DATE PICKERS (Lọc ngay khi chọn ngày xong) ---
    private fun setupDatePickers() {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        binding.tvFromDate.setOnClickListener {
            val constraints = CalendarConstraints.Builder().setValidator(DateValidatorPointBackward.now()).build()
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Chọn ngày bắt đầu")
                .setSelection(fromDate ?: MaterialDatePicker.todayInUtcMilliseconds())
                .setCalendarConstraints(constraints)
                .setTheme(R.style.CustomMaterialDatePicker)
                .build()

            datePicker.addOnPositiveButtonClickListener { selection ->
                val calendar = Calendar.getInstance().apply { timeInMillis = selection; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }
                fromDate = calendar.timeInMillis
                binding.tvFromDate.text = dateFormat.format(Date(fromDate!!))

                // Lọc ngay lập tức
                executeFilter(showToast = true)
            }
            datePicker.show(parentFragmentManager, "FROM")
        }

        binding.tvToDate.setOnClickListener {
            val constraints = CalendarConstraints.Builder().setValidator(DateValidatorPointBackward.now()).build()
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Chọn ngày kết thúc")
                .setSelection(toDate ?: MaterialDatePicker.todayInUtcMilliseconds())
                .setCalendarConstraints(constraints)
                .setTheme(R.style.CustomMaterialDatePicker)
                .build()

            datePicker.addOnPositiveButtonClickListener { selection ->
                val calendar = Calendar.getInstance().apply { timeInMillis = selection; set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59) }
                toDate = calendar.timeInMillis
                binding.tvToDate.text = dateFormat.format(Date(selection))

                // Lọc ngay lập tức
                executeFilter(showToast = true)
            }
            datePicker.show(parentFragmentManager, "TO")
        }
    }

    // --- 5. BUTTONS (Áp dụng & Xóa) ---
    private fun setupFilterButtons() {
        // Nút Áp dụng: Chủ yếu dùng để xác nhận khoảng giá (nếu người dùng nhập xong mà chưa làm gì khác)
        // Hoặc đơn giản là người dùng thích bấm nút để chắc ăn.
        binding.btnApplyFilter.setOnClickListener {
            hideKeyboard()
            executeFilter(showToast = true)
        }

        // Nút Xóa bộ lọc: Reset toàn bộ form
        binding.btnResetFilter.setOnClickListener {
            hideKeyboard()
            resetAllFilters()
        }

        // Bonus: Lắng nghe nút "Done" trên bàn phím ở ô nhập giá tối đa
        binding.etMaxAmount.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard()
                executeFilter(showToast = true)
                true
            } else false
        }
    }

    // --- LOGIC: HÀM LỌC TỔNG HỢP (QUAN TRỌNG NHẤT) ---
    private fun executeFilter(showToast: Boolean) {
        var filteredList = fullList

        // === BƯỚC 1: ĐỌC DỮ LIỆU TỪ UI NGAY TẠI THỜI ĐIỂM LỌC ===

        // A. Đọc Search Query
        val query = binding.searchView.query.toString().lowercase()

        // B. Đọc Loại (Type) từ Chip
        val typeFilter = when {
            binding.chipExpense.isChecked -> 0
            binding.chipIncome.isChecked -> 1
            else -> -1
        }

        // C. Đọc Danh mục từ TextView
        val categoryText = binding.tvCategoryFilter.text.toString()
        val categoryFilter = if (categoryText == "Tất cả danh mục") "Tất cả" else categoryText

        // D. Đọc Giá tiền từ EditText (Parse trực tiếp)
        val minText = binding.etMinAmount.text.toString().replace(",", "").replace(".", "")
        val maxText = binding.etMaxAmount.text.toString().replace(",", "").replace(".", "")
        val minVal = minText.toDoubleOrNull()
        val maxVal = maxText.toDoubleOrNull()

        // === BƯỚC 2: ÁP DỤNG CÁC ĐIỀU KIỆN ===

        // 1. Tìm kiếm
        if (query.isNotEmpty()) {
            filteredList = filteredList.filter {
                it.title.lowercase().contains(query) || it.note.lowercase().contains(query)
            }
        }

        // 2. Loại
        if (typeFilter != -1) {
            filteredList = filteredList.filter { it.type == typeFilter }
        }

        // 3. Danh mục
        if (categoryFilter != "Tất cả") {
            filteredList = filteredList.filter { it.category == categoryFilter }
        }

        // 4. Ngày tháng (Dùng biến đã lưu từ DatePicker)
        if (fromDate != null) {
            filteredList = filteredList.filter { it.date >= fromDate!! }
        }
        if (toDate != null) {
            filteredList = filteredList.filter { it.date <= toDate!! }
        }

        // 5. Giá tiền
        if (minVal != null) {
            filteredList = filteredList.filter { it.amount >= minVal }
        }
        if (maxVal != null) {
            filteredList = filteredList.filter { it.amount <= maxVal }
        }

        // === BƯỚC 3: HIỂN THỊ KẾT QUẢ ===

        // Sắp xếp
        filteredList = filteredList.sortedByDescending { it.date }

        if (filteredList.isEmpty()) {
            binding.layoutEmpty.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
        } else {
            binding.layoutEmpty.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
            adapter.setData(filteredList)
        }

        if (showToast) {
            Toast.makeText(context, "Tìm thấy ${filteredList.size} giao dịch", Toast.LENGTH_SHORT).show()
        }
    }

    private fun resetAllFilters() {
        // Reset UI về mặc định
        binding.searchView.setQuery("", false)
        binding.searchView.clearFocus()

        binding.chipAll.isChecked = true

        binding.tvCategoryFilter.text = "Tất cả danh mục"

        binding.tvFromDate.text = "Từ ngày"
        binding.tvToDate.text = "Đến ngày"
        fromDate = null
        toDate = null

        binding.etMinAmount.text?.clear()
        binding.etMaxAmount.text?.clear()

        // Gọi lọc lại (sẽ hiển thị full list)
        executeFilter(showToast = true)
    }

    private fun hideKeyboard() {
        val currentFocus = activity?.currentFocus
        if (currentFocus != null) {
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus.windowToken, 0)
            currentFocus.clearFocus()
        }
    }

    private fun setupHideKeyboardOnOutsideTouch() {
        binding.root.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) hideKeyboard()
            false
        }
    }
}