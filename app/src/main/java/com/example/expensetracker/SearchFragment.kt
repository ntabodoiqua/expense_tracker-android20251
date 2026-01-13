package com.example.expensetracker

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.expensetracker.R
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
    
    // Bộ lọc
    private var searchQuery: String = ""
    private var selectedType: Int = -1 // -1: Tất cả, 0: Chi tiêu, 1: Thu nhập
    private var selectedCategory: String = ""
    private var fromDate: Long? = null
    private var toDate: Long? = null
    private var minAmount: Double? = null
    private var maxAmount: Double? = null

    private val allCategories = listOf(
        "Tất cả",
        "Ăn uống", "Mua sắm", "Giải trí", "Sức khỏe", "Giáo dục",
        "Di chuyển", "Hóa đơn", "Quà tặng", "Du lịch", "Khác",
        "Lương", "Thưởng", "Đầu tư", "Quà tặng nhận", "Kinh doanh", "Freelance", "Khác"
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
        
        // Setup hide keyboard on outside touch
        setupHideKeyboardOnOutsideTouch()
        
        // Setup ViewModel
        viewModel = ViewModelProvider(this)[TransactionViewModel::class.java]
        
        // Setup RecyclerView
        adapter = TransactionAdapter()
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        
        // Setup category dropdown
        setupCategoryDropdown()
        
        // Observe transactions
        viewModel.allTransactions.observe(viewLifecycleOwner) { list ->
            list?.let {
                fullList = it
                applyFilters()
            }
        }
        
        // Setup listeners
        setupSearchView()
        setupTypeFilter()
        setupDatePickers()
        setupFilterButtons()
    }

    private fun setupHideKeyboardOnOutsideTouch() {
        // Hide keyboard when touching outside on root
        binding.root.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                hideKeyboard()
            }
            false
        }
        
        // Hide keyboard when touching ScrollView
        binding.scrollView.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                hideKeyboard()
            }
            false
        }
        
        // Hide keyboard when clicking on ScrollView
        binding.scrollView.setOnClickListener {
            hideKeyboard()
        }
        
        // Also hide keyboard when scrolling
        binding.root.findViewById<View>(androidx.appcompat.R.id.search_src_text)?.setOnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
            }
        }
        
        // Hide keyboard when clicking apply or reset filter
        setupAmountFieldsFocusListener()
    }
    
    private fun setupAmountFieldsFocusListener() {
        // IME action done for min amount
        binding.etMinAmount.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard()
                v.clearFocus()
                true
            } else false
        }
        
        // IME action done for max amount
        binding.etMaxAmount.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard()
                v.clearFocus()
                true
            } else false
        }
        
        // Focus change listeners
        binding.etMinAmount.setOnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
            }
        }
        
        binding.etMaxAmount.setOnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
            }
        }
    }

    private fun setupCategoryDropdown() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, allCategories)
        binding.categoryDropdown.setAdapter(adapter)
        binding.categoryDropdown.setText("Tất cả", false)
        
        binding.categoryDropdown.setOnItemClickListener { _, _, position, _ ->
            selectedCategory = allCategories[position]
        }
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object :
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                searchQuery = newText ?: ""
                applyFilters()
                return true
            }
        })
    }

    private fun setupTypeFilter() {
        binding.chipGroupType.setOnCheckedStateChangeListener { _, checkedIds ->
            selectedType = when {
                binding.chipExpense.isChecked -> 0
                binding.chipIncome.isChecked -> 1
                else -> -1
            }
            applyFilters()
        }
    }

    private fun setupDatePickers() {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        
        binding.btnFromDate.setOnClickListener {
            val constraintsBuilder = CalendarConstraints.Builder()
                .setValidator(DateValidatorPointBackward.now())
            
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Chọn ngày bắt đầu")
                .setSelection(fromDate ?: MaterialDatePicker.todayInUtcMilliseconds())
                .setCalendarConstraints(constraintsBuilder.build())
                .setTheme(R.style.CustomMaterialDatePicker)
                .build()
            
            datePicker.addOnPositiveButtonClickListener { selection ->
                val utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                utcCalendar.timeInMillis = selection
                
                val localCalendar = Calendar.getInstance()
                localCalendar.set(
                    utcCalendar.get(Calendar.YEAR),
                    utcCalendar.get(Calendar.MONTH),
                    utcCalendar.get(Calendar.DAY_OF_MONTH)
                )
                
                fromDate = localCalendar.timeInMillis
                binding.btnFromDate.text = dateFormat.format(localCalendar.time)
            }
            
            datePicker.show(parentFragmentManager, "FROM_DATE_PICKER")
        }
        
        binding.btnToDate.setOnClickListener {
            val constraintsBuilder = CalendarConstraints.Builder()
                .setValidator(DateValidatorPointBackward.now())
            
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Chọn ngày kết thúc")
                .setSelection(toDate ?: MaterialDatePicker.todayInUtcMilliseconds())
                .setCalendarConstraints(constraintsBuilder.build())
                .setTheme(R.style.CustomMaterialDatePicker)
                .build()
            
            datePicker.addOnPositiveButtonClickListener { selection ->
                val utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                utcCalendar.timeInMillis = selection
                
                val localCalendar = Calendar.getInstance()
                localCalendar.set(
                    utcCalendar.get(Calendar.YEAR),
                    utcCalendar.get(Calendar.MONTH),
                    utcCalendar.get(Calendar.DAY_OF_MONTH)
                )
                
                toDate = localCalendar.timeInMillis
                binding.btnToDate.text = dateFormat.format(localCalendar.time)
            }
            
            datePicker.show(parentFragmentManager, "TO_DATE_PICKER")
        }
    }

    private fun setupFilterButtons() {
        binding.btnApplyFilter.setOnClickListener {
            // Hide keyboard first
            hideKeyboard()
            
            // Lấy giá trị số tiền
            val minText = binding.etMinAmount.text.toString()
            val maxText = binding.etMaxAmount.text.toString()
            
            minAmount = if (minText.isNotEmpty()) minText.toDoubleOrNull() else null
            maxAmount = if (maxText.isNotEmpty()) maxText.toDoubleOrNull() else null
            
            applyFilters()
        }
        
        binding.btnResetFilter.setOnClickListener {
            // Hide keyboard first
            hideKeyboard()
            resetFilters()
        }
    }
    
    private fun hideKeyboard() {
        val currentFocus = activity?.currentFocus
        if (currentFocus != null) {
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus.windowToken, 0)
            currentFocus.clearFocus()
        }
    }

    private fun resetFilters() {
        // Reset tất cả bộ lọc
        searchQuery = ""
        selectedType = -1
        selectedCategory = "Tất cả"
        fromDate = null
        toDate = null
        minAmount = null
        maxAmount = null
        
        // Reset UI
        binding.searchView.setQuery("", false)
        binding.chipAll.isChecked = true
        binding.categoryDropdown.setText("Tất cả", false)
        binding.btnFromDate.text = "Từ ngày"
        binding.btnToDate.text = "Đến ngày"
        binding.etMinAmount.text?.clear()
        binding.etMaxAmount.text?.clear()
        
        // Áp dụng lại
        applyFilters()
    }

    private fun applyFilters() {
        var filteredList = fullList
        
        // Lọc theo từ khóa tìm kiếm
        if (searchQuery.isNotEmpty()) {
            filteredList = filteredList.filter {
                it.title.lowercase().contains(searchQuery.lowercase()) ||
                it.note.lowercase().contains(searchQuery.lowercase())
            }
        }
        
        // Lọc theo loại giao dịch
        if (selectedType != -1) {
            filteredList = filteredList.filter { it.type == selectedType }
        }
        
        // Lọc theo danh mục
        if (selectedCategory.isNotEmpty() && selectedCategory != "Tất cả") {
            filteredList = filteredList.filter { it.category == selectedCategory }
        }
        
        // Lọc theo khoảng thời gian
        if (fromDate != null) {
            filteredList = filteredList.filter { it.date >= fromDate!! }
        }
        if (toDate != null) {
            // Thêm 1 ngày để bao gồm cả ngày kết thúc
            val endOfDay = toDate!! + (24 * 60 * 60 * 1000)
            filteredList = filteredList.filter { it.date < endOfDay }
        }
        
        // Lọc theo số tiền
        if (minAmount != null) {
            filteredList = filteredList.filter { it.amount >= minAmount!! }
        }
        if (maxAmount != null) {
            filteredList = filteredList.filter { it.amount <= maxAmount!! }
        }
        
        // Sắp xếp theo ngày mới nhất
        filteredList = filteredList.sortedByDescending { it.date }
        
        // Hiển thị kết quả
        if (filteredList.isEmpty()) {
            binding.layoutEmpty.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
        } else {
            binding.layoutEmpty.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
            adapter.setData(filteredList)
        }
    }
}