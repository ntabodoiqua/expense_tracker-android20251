package com.example.expensetracker

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.expensetracker.data.Transaction
import com.example.expensetracker.data.UserPreferences
import com.example.expensetracker.databinding.FragmentChartBinding
import com.example.expensetracker.ui.TransactionAdapter
import com.example.expensetracker.viewmodel.TransactionViewModel
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class ChartFragment : Fragment() {
    private var _binding: FragmentChartBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: TransactionViewModel
    private lateinit var userPreferences: UserPreferences
    private lateinit var adapter: TransactionAdapter

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
        setupRecyclerView()
        setupPeriodSelector()
        
        viewModel.allTransactions.observe(viewLifecycleOwner) { list ->
            fullList = list ?: emptyList()
            updateStatistics()
        }
    }
    private fun setupRecyclerView() {
        adapter = TransactionAdapter()
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        binding.recyclerView.isNestedScrollingEnabled = false
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
        updateExpensePieChart(filteredList)
        updateIncomePieChart(filteredList)
        updateTrendBarChart(filteredList)
        updateTransactionList(filteredList)
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
    
    private fun updateExpensePieChart(list: List<Transaction>) {
        val expenseList = list.filter { it.type == 0 }

        // 1. Kiểm tra dữ liệu trống
        if (expenseList.isEmpty()) {
            binding.pieChartExpense.visibility = View.GONE
            binding.tvNoExpenseData.visibility = View.VISIBLE
            binding.layoutLegendExpense.removeAllViews()
            return
        }

        binding.pieChartExpense.visibility = View.VISIBLE
        binding.tvNoExpenseData.visibility = View.GONE

        // 2. Gom nhóm theo danh mục (Group by Category)
        val categoryMap = HashMap<String, Float>()
        var totalAmount = 0f

        expenseList.forEach { transaction ->
            val category = transaction.category
            val amount = transaction.amount.toFloat()
            categoryMap[category] = (categoryMap[category] ?: 0f) + amount
            totalAmount += amount
        }

        // 3. Chuẩn bị biến
        val entries = ArrayList<PieEntry>()
        val colors = ArrayList<Int>()
        val colorPalette = listOf(
            Color.parseColor("#fb923c"),
            Color.parseColor("#60a5fa"),
            Color.parseColor("#f472b6"),
            Color.parseColor("#c084fc"),
            Color.parseColor("#818cf8"),
            Color.parseColor("#22d3ee"),
            Color.parseColor("#f87171"),
            Color.parseColor("#a8a29e"),
            Color.parseColor("#9ca3af"),
        )
        // Xóa Legend cũ trước khi thêm mới
        binding.layoutLegendExpense.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())
        val formatter = DecimalFormat("#,### đ")

        // 4. Sắp xếp giảm dần (Tiền nhiều nhất lên đầu)
        val sortedList = categoryMap.entries.sortedByDescending { it.value }

        // 5. VÒNG LẶP CHÍNH (Vừa nạp Chart, vừa tạo Legend)
        sortedList.forEachIndexed { index, entry ->
            val categoryName = entry.key
            val amount = entry.value

            // --- A. Xử lý cho Chart ---
            entries.add(PieEntry(amount, categoryName))
            val color = colorPalette[index % colorPalette.size]
            colors.add(color)

            // --- B. Xử lý Custom Legend (Chú thích bên dưới) ---
            val itemView = inflater.inflate(R.layout.item_legend, binding.layoutLegendExpense, false)
            val viewColor = itemView.findViewById<View>(R.id.viewColor)
            val tvName = itemView.findViewById<TextView>(R.id.tvCategoryName)
            val tvAmount = itemView.findViewById<TextView>(R.id.tvAmount)

            try {
                val drawable = viewColor.background as android.graphics.drawable.GradientDrawable
                drawable.setColor(color)
            } catch (e: Exception) {
                viewColor.setBackgroundColor(color) // Fallback nếu không phải shape
            }

            val percent = if (totalAmount > 0) (amount / totalAmount * 100).toInt() else 0

            tvName.text = "$categoryName ($percent%)"
            tvAmount.text = formatter.format(amount)

            binding.layoutLegendExpense.addView(itemView)
        }

        // 6. Cấu hình DataSet
        val dataSet = PieDataSet(entries, "")
        dataSet.colors = colors

        dataSet.valueTextSize = 0f
        dataSet.valueTextColor = Color.TRANSPARENT
        dataSet.sliceSpace = 2f

        // 7. Cấu hình Chart
        val data = PieData(dataSet)
        binding.pieChartExpense.data = data
        binding.pieChartExpense.description.isEnabled = false
        binding.pieChartExpense.setDrawEntryLabels(false)
        binding.pieChartExpense.legend.isEnabled = false
        binding.pieChartExpense.isDrawHoleEnabled = true
        binding.pieChartExpense.setHoleColor(Color.TRANSPARENT)
        binding.pieChartExpense.holeRadius = 50f
        binding.pieChartExpense.transparentCircleRadius = 55f
        binding.pieChartExpense.setExtraOffsets(0f, 0f, 0f, 0f)
        binding.pieChartExpense.animateY(1500, Easing.EaseInOutQuad)
        binding.pieChartExpense.invalidate()
    }

    private fun updateIncomePieChart(list: List<Transaction>) {
        val incomeList = list.filter { it.type == 1 }

        // 1. Kiểm tra dữ liệu trống
        if (incomeList.isEmpty()) {
            binding.pieChartIncome.visibility = View.GONE
            binding.tvNoIncomeData.visibility = View.VISIBLE
            binding.layoutLegendIncome.removeAllViews()
            return
        }

        binding.pieChartIncome.visibility = View.VISIBLE
        binding.tvNoIncomeData.visibility = View.GONE

        // 2. Gom nhóm theo danh mục (Group by Category)
        val categoryMap = HashMap<String, Float>()
        var totalAmount = 0f

        incomeList.forEach { transaction ->
            val category = transaction.category
            val amount = transaction.amount.toFloat()
            categoryMap[category] = (categoryMap[category] ?: 0f) + amount
            totalAmount += amount
        }

        // 3. Chuẩn bị biến
        val entries = ArrayList<PieEntry>()
        val colors = ArrayList<Int>()
        val colorPalette = listOf(
            Color.parseColor("#4ade80"),
            Color.parseColor("#facc15"),
            Color.parseColor("#a3e635"),
            Color.parseColor("#38bdf8"),
            Color.parseColor("#fbbf24"),
            Color.parseColor("#9ca3af")
        )

        // Xóa Legend cũ trước khi thêm mới
        binding.layoutLegendIncome.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())
        val formatter = DecimalFormat("#,### đ")

        // 4. Sắp xếp giảm dần (Tiền nhiều nhất lên đầu)
        val sortedList = categoryMap.entries.sortedByDescending { it.value }

        // 5. VÒNG LẶP CHÍNH (Vừa nạp Chart, vừa tạo Legend)
        sortedList.forEachIndexed { index, entry ->
            val categoryName = entry.key
            val amount = entry.value

            // --- A. Xử lý cho Chart ---
            entries.add(PieEntry(amount, categoryName))
            val color = colorPalette[index % colorPalette.size]
            colors.add(color)

            // --- B. Xử lý Custom Legend (Chú thích bên dưới) ---
            val itemView = inflater.inflate(R.layout.item_legend, binding.layoutLegendIncome, false)
            val viewColor = itemView.findViewById<View>(R.id.viewColor)
            val tvName = itemView.findViewById<TextView>(R.id.tvCategoryName)
            val tvAmount = itemView.findViewById<TextView>(R.id.tvAmount)

            try {
                val drawable = viewColor.background as android.graphics.drawable.GradientDrawable
                drawable.setColor(color)
            } catch (e: Exception) {
                viewColor.setBackgroundColor(color) // Fallback nếu không phải shape
            }

            val percent = if (totalAmount > 0) (amount / totalAmount * 100).toInt() else 0

            tvName.text = "$categoryName ($percent%)"
            tvAmount.text = formatter.format(amount)

            binding.layoutLegendIncome.addView(itemView)
        }

        // 6. Cấu hình DataSet
        val dataSet = PieDataSet(entries, "")
        dataSet.colors = colors

        dataSet.valueTextSize = 0f
        dataSet.valueTextColor = Color.TRANSPARENT
        dataSet.sliceSpace = 2f

        // 7. Cấu hình Chart
        val data = PieData(dataSet)
        binding.pieChartIncome.data = data
        binding.pieChartIncome.description.isEnabled = false
        binding.pieChartIncome.setDrawEntryLabels(false)
        binding.pieChartIncome.legend.isEnabled = false
        binding.pieChartIncome.isDrawHoleEnabled = true
        binding.pieChartIncome.setHoleColor(Color.TRANSPARENT)
        binding.pieChartIncome.holeRadius = 50f
        binding.pieChartIncome.transparentCircleRadius = 55f
        binding.pieChartIncome.setExtraOffsets(0f, 0f, 0f, 0f)
        binding.pieChartIncome.animateY(1500, Easing.EaseInOutQuad)
        binding.pieChartIncome.invalidate()
    }

    private fun updateTrendBarChart(list: List<Transaction>) {
        // 1. Kiểm tra dữ liệu
        if (list.isEmpty()) {
            binding.barChartTrend.visibility = View.GONE
            binding.tvNoTrendData.visibility = View.VISIBLE
            return
        }

        binding.barChartTrend.visibility = View.VISIBLE
        binding.tvNoTrendData.visibility = View.GONE

        // 2. Định dạng ngày tháng cho trục X
        val dateFormat = when (selectedPeriod) {
            Period.WEEK -> SimpleDateFormat("EEE", Locale("vi", "VN"))
            Period.MONTH -> SimpleDateFormat("dd/MM", Locale.getDefault())
            Period.YEAR -> SimpleDateFormat("MM/yy", Locale.getDefault())
            Period.ALL -> SimpleDateFormat("MM/yy", Locale.getDefault())
        }

        // 3. Xử lý dữ liệu (Group Data)
        val groupedData = HashMap<String, Pair<Float, Float>>()

        list.forEach { transaction ->
            val label = dateFormat.format(Date(transaction.date))
            val current = groupedData[label] ?: Pair(0f, 0f)

            if (transaction.type == 1) { // Thu nhập
                groupedData[label] = Pair(current.first + transaction.amount.toFloat(), current.second)
            } else { // Chi tiêu
                groupedData[label] = Pair(current.first, current.second + transaction.amount.toFloat())
            }
        }

        // Sắp xếp theo thời gian
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

        // 4. Cấu hình DataSet (Màu sắc, Font chữ)
        val incomeDataSet = BarDataSet(incomeEntries, "Thu nhập")
        incomeDataSet.color = Color.parseColor("#22c55e") // Xanh lá
        incomeDataSet.valueTextColor = Color.parseColor("#22c55e")
        incomeDataSet.valueTextSize = 14f

        val expenseDataSet = BarDataSet(expenseEntries, "Chi tiêu")
        expenseDataSet.color = Color.parseColor("#ef4444") // Đỏ
        expenseDataSet.valueTextColor = Color.parseColor("#ef4444")
        expenseDataSet.valueTextSize = 14f

        // 5. Tính toán khoảng cách
        val groupSpace = 0.2f // Khoảng cách giữa các ngày
        val barSpace = 0.05f  // Khoảng cách giữa cột Thu và Chi
        val barWidth = 0.35f  // Độ rộng của cột

        val barData = BarData(incomeDataSet, expenseDataSet)
        barData.barWidth = barWidth

        // Custom hiển thị số trên đầu cột
        barData.setValueFormatter(object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                if (value == 0f) return ""

                return if (value >= 1_000_000) {
                    DecimalFormat("#.#M").format(value / 1_000_000)
                } else if (value >= 1000) {
                    DecimalFormat("#.#K").format(value / 1000)
                } else {
                    DecimalFormat("#").format(value)
                }
            }
        })

        binding.barChartTrend.data = barData

        // 6. Cấu hình trục X (Trục ngang)
        val xAxis = binding.barChartTrend.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.setCenterAxisLabels(true)
        xAxis.granularity = 1f
        xAxis.setDrawGridLines(false)
        xAxis.textColor = Color.DKGRAY
        xAxis.textSize = 14f
        xAxis.axisMinimum = 0f
        xAxis.axisMaximum = labels.size.toFloat()

        // 7. Cấu hình trục Y (Trục dọc bên trái)
        val leftAxis = binding.barChartTrend.axisLeft
        leftAxis.setDrawGridLines(true)
        leftAxis.gridColor = Color.parseColor("#E0E0E0")
        leftAxis.axisMinimum = 0f
        leftAxis.textColor = Color.GRAY

        // 8. Tắt trục Y bên phải & Description
        binding.barChartTrend.axisRight.isEnabled = false
        binding.barChartTrend.description.isEnabled = false

        // 9. Cấu hình Legend (Chú thích)
        val legend = binding.barChartTrend.legend
        legend.verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.TOP
        legend.horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.RIGHT
        legend.orientation = com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL
        legend.setDrawInside(false)
        legend.yOffset = 10f
        legend.xOffset = 10f
        legend.textSize = 14f

        // 10. Kích hoạt Group Bars và Animation
        binding.barChartTrend.groupBars(0f, groupSpace, barSpace)

        // Zoom/Scroll
        binding.barChartTrend.setVisibleXRangeMaximum(7f)
        binding.barChartTrend.isDragEnabled = true
        binding.barChartTrend.setScaleEnabled(false)

        binding.barChartTrend.animateY(1000)
        binding.barChartTrend.invalidate()
    }

    private fun updateTransactionList(list: List<Transaction>) {
        // Sắp xếp giảm dần theo ngày (mới nhất lên đầu)
        val sortedList = list.sortedByDescending { it.date }

        adapter.setData(sortedList)

        if (sortedList.isEmpty()) {
            binding.recyclerView.visibility = View.GONE
            binding.tvNoTransaction.visibility = View.VISIBLE
        } else {
            binding.recyclerView.visibility = View.VISIBLE
            binding.tvNoTransaction.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}