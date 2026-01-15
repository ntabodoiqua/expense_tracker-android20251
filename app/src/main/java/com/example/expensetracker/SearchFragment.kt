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
    private var fromDate: Long? = null
    private var toDate: Long? = null
    private val allCategories = listOf(
        "Tất cả", "Ăn uống", "Đi lại", "Mua sắm", "Giải trí", "Tiền nhà", "Hóa đơn", "Y tế", "Giáo dục",
        "Lương", "Thưởng", "Lãi tiết kiệm", "Bán hàng", "Quà tặng", "Khác"
    )

    // Khởi tạo giao diện từ XML binding
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Thiết lập ViewModel, RecyclerView và các bộ lọc
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupHideKeyboardOnOutsideTouch()
        viewModel = ViewModelProvider(this)[TransactionViewModel::class.java]
        adapter = TransactionAdapter()
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        setupSearchView()
        setupCategoryFilter()
        setupTypeFilter()
        setupDatePickers()
        setupFilterButtons()
        viewModel.allTransactions.observe(viewLifecycleOwner) { list ->
            list?.let {
                fullList = it
                executeFilter(showToast = false)
            }
        }
    }

    // Thiết lập thanh tìm kiếm
    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                hideKeyboard()
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                executeFilter(showToast = false)
                return true
            }
        })
    }

    // Thiết lập bộ lọc danh mục
    private fun setupCategoryFilter() {
        binding.tvCategoryFilter.text = "Tất cả danh mục"
        binding.tvCategoryFilter.setOnClickListener { view ->
            val popup = PopupMenu(requireContext(), view)
            allCategories.forEach { popup.menu.add(it) }
            popup.setOnMenuItemClickListener { item ->
                binding.tvCategoryFilter.text = if (item.title == "Tất cả") "Tất cả danh mục" else item.title
                executeFilter(showToast = true)
                true
            }
            popup.show()
        }
    }

    // Thiết lập bộ lọc loại giao dịch
    private fun setupTypeFilter() {
        binding.chipGroupType.setOnCheckedStateChangeListener { _, _ ->
            executeFilter(showToast = true)
        }
    }

    // Thiết lập bộ chọn ngày
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
                val calendar = Calendar.getInstance().apply {
                    timeInMillis = selection
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }
                fromDate = calendar.timeInMillis
                binding.tvFromDate.text = dateFormat.format(Date(fromDate!!))
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
                val calendar = Calendar.getInstance().apply {
                    timeInMillis = selection
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                }
                toDate = calendar.timeInMillis
                binding.tvToDate.text = dateFormat.format(Date(selection))
                executeFilter(showToast = true)
            }
            datePicker.show(parentFragmentManager, "TO")
        }
    }

    // Thiết lập nút áp dụng và xóa bộ lọc
    private fun setupFilterButtons() {
        binding.btnApplyFilter.setOnClickListener {
            hideKeyboard()
            executeFilter(showToast = true)
        }
        binding.btnResetFilter.setOnClickListener {
            hideKeyboard()
            resetAllFilters()
        }
        binding.etMaxAmount.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard()
                executeFilter(showToast = true)
                true
            } else false
        }
    }

    // Thực hiện lọc dữ liệu theo các điều kiện
    private fun executeFilter(showToast: Boolean) {
        var filteredList = fullList
        val query = binding.searchView.query.toString().lowercase()
        val typeFilter = when {
            binding.chipExpense.isChecked -> 0
            binding.chipIncome.isChecked -> 1
            else -> -1
        }
        val categoryText = binding.tvCategoryFilter.text.toString()
        val categoryFilter = if (categoryText == "Tất cả danh mục") "Tất cả" else categoryText
        val minText = binding.etMinAmount.text.toString().replace(",", "").replace(".", "")
        val maxText = binding.etMaxAmount.text.toString().replace(",", "").replace(".", "")
        val minVal = minText.toDoubleOrNull()
        val maxVal = maxText.toDoubleOrNull()
        if (query.isNotEmpty()) {
            filteredList = filteredList.filter {
                it.title.lowercase().contains(query) || it.note.lowercase().contains(query)
            }
        }
        if (typeFilter != -1) {
            filteredList = filteredList.filter { it.type == typeFilter }
        }
        if (categoryFilter != "Tất cả") {
            filteredList = filteredList.filter { it.category == categoryFilter }
        }
        if (fromDate != null) {
            filteredList = filteredList.filter { it.date >= fromDate!! }
        }
        if (toDate != null) {
            filteredList = filteredList.filter { it.date <= toDate!! }
        }
        if (minVal != null) {
            filteredList = filteredList.filter { it.amount >= minVal }
        }
        if (maxVal != null) {
            filteredList = filteredList.filter { it.amount <= maxVal }
        }
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

    // Đặt lại tất cả bộ lọc về mặc định
    private fun resetAllFilters() {
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
        executeFilter(showToast = true)
    }

    // Ẩn bàn phím
    private fun hideKeyboard() {
        val currentFocus = activity?.currentFocus
        if (currentFocus != null) {
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus.windowToken, 0)
            currentFocus.clearFocus()
        }
    }

    // Ẩn bàn phím khi chạm bên ngoài
    private fun setupHideKeyboardOnOutsideTouch() {
        binding.root.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) hideKeyboard()
            false
        }
    }
}
