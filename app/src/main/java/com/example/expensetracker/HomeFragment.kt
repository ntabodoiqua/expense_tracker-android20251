package com.example.expensetracker

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.expensetracker.databinding.FragmentHomeBinding
import com.example.expensetracker.ui.TransactionAdapter
import com.example.expensetracker.viewmodel.TransactionViewModel
import com.bumptech.glide.Glide
import com.example.expensetracker.data.Transaction
import com.example.expensetracker.data.UserPreferences
import com.example.expensetracker.ui.AddTransactionFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.Calendar

class HomeFragment : Fragment() {
    private lateinit var binding: FragmentHomeBinding
    private lateinit var viewModel: TransactionViewModel
    private lateinit var adapter: TransactionAdapter
    private lateinit var userPreferences: UserPreferences

    private var fullList: List<Transaction> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userPreferences = UserPreferences(requireContext())
        viewModel = ViewModelProvider(this)[TransactionViewModel::class.java]

        setupRecyclerView()
        setupListeners()
        setupObservers()
        loadUserInfo()
    }

    override fun onResume() {
        super.onResume()
        loadUserInfo()
        if (fullList.isNotEmpty()) {
            updateNotificationBadge()
        }
    }

    private fun setupRecyclerView() {
        adapter = TransactionAdapter()
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        setupSwipeToDelete()

        adapter.onItemClick = { transaction ->
            showAddEditTransactionDialog(transaction)
        }
    }

    private fun setupListeners() {
        binding.btnNoti.setOnClickListener {
            openNotificationPage()
        }

        binding.btnViewMore.setOnClickListener {
            try {
                requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigation)
                    .selectedItemId = R.id.nav_chart
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        binding.fabAdd.setOnClickListener {
            showAddEditTransactionDialog(null)
        }
    }

    private fun setupObservers() {
        viewModel.allTransactions.observe(viewLifecycleOwner) { list ->
            list?.let {
                fullList = it
                val recentTransactions = it.sortedByDescending { t -> t.date }.take(5)

                adapter.setData(recentTransactions)
                updateDashboard(it)
                updateNotificationBadge()

                if (recentTransactions.isEmpty()) {
                    binding.layoutEmpty.visibility = View.VISIBLE
                    binding.recyclerView.visibility = View.GONE
                } else {
                    binding.layoutEmpty.visibility = View.GONE
                    binding.recyclerView.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun showAddEditTransactionDialog(transaction: Transaction?) {
        val bottomSheet = AddTransactionFragment()
        if (transaction != null) {
            val bundle = Bundle()
            bundle.putSerializable("transaction_data", transaction)
            bottomSheet.arguments = bundle
        }

        bottomSheet.onSaveClick = { amount, typeStr, category, note, date ->
            val typeInt = if (typeStr == "Thu nhập") 1 else 0
            if (transaction == null) {
                // Thêm mới
                val newTransaction = Transaction(0, category, amount, typeInt, category, note, date)
                viewModel.addTransaction(newTransaction)
                Toast.makeText(context, "Đã thêm giao dịch!", Toast.LENGTH_SHORT).show()
            } else {
                // Cập nhật
                val updatedTransaction = transaction.copy(
                    amount = amount, type = typeInt, category = category, note = note, date = date
                )
                viewModel.updateTransaction(updatedTransaction)
                Toast.makeText(context, "Đã cập nhật!", Toast.LENGTH_SHORT).show()
            }
        }
        bottomSheet.show(parentFragmentManager, if (transaction == null) "AddTag" else "EditTag")
    }

    private fun updateDashboard(list: List<Transaction>) {
        var totalIncome = 0.0
        var totalExpense = 0.0
        var todayExpense = 0.0
        var monthExpense = 0.0

        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_YEAR)
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)

        for (transaction in list) {
            if (transaction.type == 1) {
                totalIncome += transaction.amount
            } else {
                totalExpense += transaction.amount
                val tCal = Calendar.getInstance()
                tCal.timeInMillis = transaction.date

                if (tCal.get(Calendar.YEAR) == currentYear) {
                    if (tCal.get(Calendar.DAY_OF_YEAR) == today) todayExpense += transaction.amount
                    if (tCal.get(Calendar.MONTH) == currentMonth) monthExpense += transaction.amount
                }
            }
        }

        val totalBalance = totalIncome - totalExpense
        val formatter = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("vi", "VN"))

        binding.currentBalanceValue.text = formatter.format(totalBalance)
        binding.tvIncome.text = formatter.format(totalIncome)
        binding.tvExpense.text = formatter.format(totalExpense)

        checkSpendingLimits(todayExpense, monthExpense, formatter)
    }

    private fun checkSpendingLimits(todayExpense: Double, monthExpense: Double, formatter: java.text.NumberFormat) {
        val warnings = mutableListOf<String>()

        if (userPreferences.isDailyLimitEnabled && userPreferences.dailyLimit > 0) {
            val dailyLimit = userPreferences.dailyLimit
            val dailyPercent = (todayExpense / dailyLimit * 100).toInt()
            if (todayExpense >= dailyLimit) {
                warnings.add("🚨 Đã vượt giới hạn ngày!\nĐã chi: ${formatter.format(todayExpense)}")
            } else if (dailyPercent >= 80) {
                warnings.add("⚠️ Sắp đạt giới hạn ngày (${dailyPercent}%)\nĐã chi: ${formatter.format(todayExpense)}")
            }
        }

        if (userPreferences.isMonthlyLimitEnabled && userPreferences.monthlyLimit > 0) {
            val monthlyLimit = userPreferences.monthlyLimit
            val monthlyPercent = (monthExpense / monthlyLimit * 100).toInt()
            if (monthExpense >= monthlyLimit) {
                warnings.add("🚨 Đã vượt giới hạn tháng!\nĐã chi: ${formatter.format(monthExpense)}")
            } else if (monthlyPercent >= 80) {
                warnings.add("⚠️ Sắp đạt giới hạn tháng (${monthlyPercent}%)\nĐã chi: ${formatter.format(monthExpense)}")
            }
        }

        if (warnings.isNotEmpty()) {
            binding.layoutWarning.visibility = View.VISIBLE
            binding.tvWarning.text = warnings.joinToString("\n\n")
        } else {
            binding.layoutWarning.visibility = View.GONE
        }
    }

    private fun updateNotificationBadge() {
        val count = countUnreadNotifications()
        if (count > 0) {
            binding.tvNotificationBadge.visibility = View.VISIBLE
            binding.tvNotificationBadge.text = if (count > 9) "9+" else count.toString()
        } else {
            binding.tvNotificationBadge.visibility = View.GONE
        }
    }

    private fun countUnreadNotifications(): Int {
        var count = 0
        val lastReadTime = userPreferences.lastNotificationReadTime

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startOfToday = calendar.timeInMillis

        if (userPreferences.isDailyLimitEnabled) {
            val todayExpense = calculateTodayExpense()
            if (todayExpense >= userPreferences.dailyLimit * 0.8 && lastReadTime < startOfToday) {
                count++
            }
        }

        if (userPreferences.isMonthlyLimitEnabled) {
            val monthExpense = calculateMonthExpense()
            if (monthExpense >= userPreferences.monthlyLimit * 0.8 && lastReadTime < startOfToday) {
                count++
            }
        }

        val todayTransactions = fullList.filter { isToday(it.date) }
        if (todayTransactions.isNotEmpty()) {
            val latestTransactionTime = todayTransactions.maxOf { it.date }
            if (latestTransactionTime > lastReadTime) {
                count++
            }
        }

        return count
    }

    private fun setupSwipeToDelete() {
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(r: RecyclerView, v: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val transactionToDelete = adapter.getTransactionAt(position)
                viewModel.deleteTransaction(transactionToDelete)
                Toast.makeText(requireContext(), "Đã xóa!", Toast.LENGTH_SHORT).show()
            }
        }
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(binding.recyclerView)
    }

    private fun loadUserInfo() {
        binding.Username.text = userPreferences.userName
        val avatarUri = userPreferences.userAvatar
        if (avatarUri.isNotEmpty()) {
            try {
                Glide.with(this).load(Uri.parse(avatarUri)).circleCrop()
                    .placeholder(R.drawable.avatar).error(R.drawable.avatar).into(binding.imgAvatar)
            } catch (e: Exception) { loadDefaultAvatar() }
        } else { loadDefaultAvatar() }
    }

    private fun loadDefaultAvatar() {
        Glide.with(this).load(R.drawable.avatar).circleCrop().into(binding.imgAvatar)
    }

    private fun openNotificationPage() {
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, NotificationFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun calculateTodayExpense(): Double {
        return fullList.filter { it.type == 0 && isToday(it.date) }.sumOf { it.amount }
    }

    private fun calculateMonthExpense(): Double {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)
        return fullList.filter {
            it.type == 0 && isSameMonth(it.date, currentMonth, currentYear)
        }.sumOf { it.amount }
    }

    private fun isToday(date: Long): Boolean {
        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_YEAR)
        val currentYear = calendar.get(Calendar.YEAR)
        val tCal = Calendar.getInstance()
        tCal.timeInMillis = date
        return tCal.get(Calendar.DAY_OF_YEAR) == today && tCal.get(Calendar.YEAR) == currentYear
    }

    private fun isSameMonth(date: Long, month: Int, year: Int): Boolean {
        val tCal = Calendar.getInstance()
        tCal.timeInMillis = date
        return tCal.get(Calendar.MONTH) == month && tCal.get(Calendar.YEAR) == year
    }
}