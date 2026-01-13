package com.example.expensetracker

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.expensetracker.data.Transaction
import com.example.expensetracker.data.UserPreferences
import com.example.expensetracker.databinding.FragmentChartBinding
import com.example.expensetracker.viewmodel.TransactionViewModel
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

class ChartFragment : Fragment() {
    private var _binding: FragmentChartBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: TransactionViewModel
    private lateinit var userPreferences: UserPreferences
    
    private var selectedPeriod = Period.MONTH
    private var fullList: List<Transaction> = emptyList()
    
    enum class Period {
        WEEK, MONTH, YEAR, ALL
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        userPreferences = UserPreferences(requireContext())
        viewModel = ViewModelProvider(this)[TransactionViewModel::class.java]
        
        loadUserInfo()
        setupPeriodSelector()
        
        viewModel.allTransactions.observe(viewLifecycleOwner) { list ->
            fullList = list ?: emptyList()
            updateStatistics()
        }
    }
    
    private fun loadUserInfo() {
        val avatarUri = userPreferences.userAvatar
        if (avatarUri.isNotEmpty()) {
            Glide.with(this)
                .load(android.net.Uri.parse(avatarUri))
                .circleCrop()
                .into(binding.imgAvatar)
        }
    }
    
    private fun setupPeriodSelector() {
        binding.chipGroupPeriod.setOnCheckedStateChangeListener { _, checkedIds ->
            selectedPeriod = when (checkedIds.firstOrNull()) {
                R.id.chipWeek -> Period.WEEK
                R.id.chipMonth -> Period.MONTH
                R.id.chipYear -> Period.YEAR
                R.id.chipAll -> Period.ALL
                else -> Period.MONTH
            }
            updatePeriodSubtitle()
            updateStatistics()
        }
    }
    
    private fun updatePeriodSubtitle() {
        binding.tvPeriodSubtitle.text = when (selectedPeriod) {
            Period.WEEK -> "7 ngày gần đây"
            Period.MONTH -> "Tháng này"
            Period.YEAR -> "Năm nay"
            Period.ALL -> "Toàn bộ thời gian"
        }
    }
    
    private fun getFilteredTransactions(): List<Transaction> {
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis
        
        return when (selectedPeriod) {
            Period.WEEK -> {
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                fullList.filter { it.date >= calendar.timeInMillis }
            }
            Period.MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                fullList.filter { it.date >= calendar.timeInMillis }
            }
            Period.YEAR -> {
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                fullList.filter { it.date >= calendar.timeInMillis }
            }
            Period.ALL -> fullList
        }
    }
    
    private fun updateStatistics() {
        val filteredList = getFilteredTransactions()
        
        updateSummaryCards(filteredList)
        updateCategoryPieChart(filteredList)
        updateTrendBarChart(filteredList)
        updateTopCategories(filteredList)
    }
    
    private fun updateSummaryCards(list: List<Transaction>) {
        var totalIncome = 0.0
        var totalExpense = 0.0
        
        list.forEach { transaction ->
            if (transaction.type == 1) {
                totalIncome += transaction.amount
            } else {
                totalExpense += transaction.amount
            }
        }
        
        val netBalance = totalIncome - totalExpense
        val formatter = DecimalFormat("#,### đ")
        
        binding.tvTotalIncome.text = formatter.format(totalIncome)
        binding.tvTotalExpense.text = formatter.format(totalExpense)
        binding.tvNetBalance.text = formatter.format(netBalance)
        binding.tvTransactionCount.text = "${list.size} giao dịch"
    }
    
    private fun updateCategoryPieChart(list: List<Transaction>) {
        val expenseList = list.filter { it.type == 0 }
        
        if (expenseList.isEmpty()) {
            binding.pieChartCategory.visibility = View.GONE
            binding.tvNoCategoryData.visibility = View.VISIBLE
            return
        }
        
        binding.pieChartCategory.visibility = View.VISIBLE
        binding.tvNoCategoryData.visibility = View.GONE
        
        // Group by category
        val categoryMap = HashMap<String, Float>()
        expenseList.forEach { transaction ->
            val category = transaction.category
            categoryMap[category] = (categoryMap[category] ?: 0f) + transaction.amount.toFloat()
        }
        
        // Create pie entries
        val entries = ArrayList<PieEntry>()
        val colors = ArrayList<Int>()
        val colorPalette = listOf(
            Color.parseColor("#F44336"), // Red
            Color.parseColor("#E91E63"), // Pink
            Color.parseColor("#9C27B0"), // Purple
            Color.parseColor("#673AB7"), // Deep Purple
            Color.parseColor("#3F51B5"), // Indigo
            Color.parseColor("#2196F3"), // Blue
            Color.parseColor("#00BCD4"), // Cyan
            Color.parseColor("#009688"), // Teal
            Color.parseColor("#4CAF50"), // Green
            Color.parseColor("#FF9800"), // Orange
        )
        
        categoryMap.entries.sortedByDescending { it.value }.forEachIndexed { index, entry ->
            entries.add(PieEntry(entry.value, entry.key))
            colors.add(colorPalette[index % colorPalette.size])
        }
        
        val dataSet = PieDataSet(entries, "")
        dataSet.colors = colors
        dataSet.valueTextSize = 12f
        dataSet.valueTextColor = Color.WHITE
        dataSet.sliceSpace = 2f
        dataSet.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return if (value > 1000) {
                    DecimalFormat("#.#k").format(value / 1000)
                } else {
                    DecimalFormat("#").format(value)
                }
            }
        }
        
        val data = PieData(dataSet)
        binding.pieChartCategory.data = data
        binding.pieChartCategory.description.isEnabled = false
        binding.pieChartCategory.setDrawEntryLabels(false)
        
        // Configure legend to avoid overlapping
        val legend = binding.pieChartCategory.legend
        legend.isEnabled = true
        legend.verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM
        legend.horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER
        legend.orientation = com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL
        legend.setDrawInside(false)
        legend.textSize = 11f
        legend.xEntrySpace = 8f
        legend.yEntrySpace = 4f
        legend.formSize = 10f
        
        binding.pieChartCategory.setEntryLabelTextSize(11f)
        binding.pieChartCategory.setHoleColor(Color.TRANSPARENT)
        binding.pieChartCategory.holeRadius = 40f
        binding.pieChartCategory.transparentCircleRadius = 45f
        binding.pieChartCategory.setExtraOffsets(10f, 10f, 10f, 20f) // Add bottom offset for legend
        binding.pieChartCategory.animateY(1000)
        binding.pieChartCategory.invalidate()
    }
    
    private fun updateTrendBarChart(list: List<Transaction>) {
        if (list.isEmpty()) {
            binding.barChartTrend.visibility = View.GONE
            binding.tvNoTrendData.visibility = View.VISIBLE
            return
        }
        
        binding.barChartTrend.visibility = View.VISIBLE
        binding.tvNoTrendData.visibility = View.GONE
        
        val dateFormat = when (selectedPeriod) {
            Period.WEEK -> SimpleDateFormat("EEE", Locale("vi", "VN"))
            Period.MONTH -> SimpleDateFormat("dd/MM", Locale.getDefault())
            Period.YEAR -> SimpleDateFormat("MMM", Locale("vi", "VN"))
            Period.ALL -> SimpleDateFormat("MM/yy", Locale.getDefault())
        }
        
        // Group transactions by date/period
        val groupedData = HashMap<String, Pair<Float, Float>>() // label -> (income, expense)
        
        list.forEach { transaction ->
            val label = dateFormat.format(Date(transaction.date))
            val current = groupedData[label] ?: Pair(0f, 0f)
            
            if (transaction.type == 1) {
                groupedData[label] = Pair(current.first + transaction.amount.toFloat(), current.second)
            } else {
                groupedData[label] = Pair(current.first, current.second + transaction.amount.toFloat())
            }
        }
        
        val sortedEntries = groupedData.entries.sortedBy { entry ->
            list.first { dateFormat.format(Date(it.date)) == entry.key }.date
        }
        
        val incomeEntries = ArrayList<BarEntry>()
        val expenseEntries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()
        
        sortedEntries.forEachIndexed { index, entry ->
            labels.add(entry.key)
            incomeEntries.add(BarEntry(index.toFloat(), entry.value.first))
            expenseEntries.add(BarEntry(index.toFloat(), entry.value.second))
        }
        
        val incomeDataSet = BarDataSet(incomeEntries, "Thu nhập")
        incomeDataSet.color = Color.parseColor("#4CAF50")
        incomeDataSet.valueTextSize = 10f
        
        val expenseDataSet = BarDataSet(expenseEntries, "Chi tiêu")
        expenseDataSet.color = Color.parseColor("#F44336")
        expenseDataSet.valueTextSize = 10f
        
        val barData = BarData(incomeDataSet, expenseDataSet)
        barData.barWidth = 0.35f
        barData.setValueFormatter(object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return if (value > 1000) {
                    DecimalFormat("#.#k").format(value / 1000)
                } else if (value > 0) {
                    DecimalFormat("#").format(value)
                } else {
                    ""
                }
            }
        })
        
        binding.barChartTrend.data = barData
        binding.barChartTrend.groupBars(0f, 0.3f, 0f)
        binding.barChartTrend.description.isEnabled = false
        
        // Configure legend
        val legend = binding.barChartTrend.legend
        legend.isEnabled = true
        legend.verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.TOP
        legend.horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.RIGHT
        legend.orientation = com.github.mikephil.charting.components.Legend.LegendOrientation.VERTICAL
        legend.setDrawInside(false)
        legend.textSize = 11f
        legend.xOffset = 10f
        
        binding.barChartTrend.setFitBars(true)
        binding.barChartTrend.setExtraOffsets(10f, 10f, 10f, 10f)
        
        val xAxis = binding.barChartTrend.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.granularity = 1f
        xAxis.setDrawGridLines(false)
        xAxis.labelRotationAngle = -45f
        xAxis.textSize = 10f
        
        binding.barChartTrend.axisLeft.setDrawGridLines(false)
        binding.barChartTrend.axisLeft.textSize = 10f
        binding.barChartTrend.axisRight.isEnabled = false
        binding.barChartTrend.animateY(1000)
        binding.barChartTrend.invalidate()
    }
    
    private fun updateTopCategories(list: List<Transaction>) {
        val expenseList = list.filter { it.type == 0 }
        
        if (expenseList.isEmpty()) {
            binding.layoutTopCategories.visibility = View.GONE
            binding.tvNoTopCategories.visibility = View.VISIBLE
            return
        }
        
        binding.layoutTopCategories.visibility = View.VISIBLE
        binding.tvNoTopCategories.visibility = View.GONE
        binding.layoutTopCategories.removeAllViews()
        
        // Group by category and calculate totals
        val categoryMap = HashMap<String, Float>()
        expenseList.forEach { transaction ->
            categoryMap[transaction.category] = (categoryMap[transaction.category] ?: 0f) + transaction.amount.toFloat()
        }
        
        val totalExpense = categoryMap.values.sum()
        val sortedCategories = categoryMap.entries.sortedByDescending { it.value }.take(5)
        
        sortedCategories.forEach { entry ->
            val categoryView = layoutInflater.inflate(
                android.R.layout.simple_list_item_2,
                binding.layoutTopCategories,
                false
            )
            
            val percentage = (entry.value / totalExpense * 100).toInt()
            val formatter = DecimalFormat("#,### đ")
            
            val text1 = categoryView.findViewById<TextView>(android.R.id.text1)
            val text2 = categoryView.findViewById<TextView>(android.R.id.text2)
            
            text1.text = "${entry.key} ($percentage%)"
            text1.textSize = 16f
            text1.setTextColor(Color.parseColor("#1E293B"))
            
            text2.text = formatter.format(entry.value)
            text2.textSize = 14f
            text2.setTextColor(Color.parseColor("#64748B"))
            
            categoryView.setPadding(0, 16, 0, 16)
            binding.layoutTopCategories.addView(categoryView)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}