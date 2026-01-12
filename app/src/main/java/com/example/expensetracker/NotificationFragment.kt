package com.example.expensetracker

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import com.example.expensetracker.data.UserPreferences
import com.example.expensetracker.databinding.FragmentNotificationBinding
import com.example.expensetracker.viewmodel.TransactionViewModel
import java.text.DecimalFormat
import java.util.*

class NotificationFragment : Fragment() {
    private var _binding: FragmentNotificationBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: TransactionViewModel
    private lateinit var userPreferences: UserPreferences
    private var fullList: List<com.example.expensetracker.data.Transaction> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userPreferences = UserPreferences(requireContext())
        viewModel = ViewModelProvider(this)[TransactionViewModel::class.java]

        // Back button
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // Mark all as read
        binding.tvMarkAllRead.setOnClickListener {
            markAllAsRead()
        }

        // Observe transactions
        viewModel.allTransactions.observe(viewLifecycleOwner) { list ->
            fullList = list ?: emptyList()
            loadNotifications()
        }
    }

    private fun loadNotifications() {
        binding.layoutNotifications.removeAllViews()
        val notifications = generateNotifications()

        if (notifications.isEmpty()) {
            binding.layoutEmpty.visibility = View.VISIBLE
            binding.layoutNotifications.visibility = View.GONE
        } else {
            binding.layoutEmpty.visibility = View.GONE
            binding.layoutNotifications.visibility = View.VISIBLE

            notifications.forEach { notification ->
                addNotificationItem(
                    notification.title,
                    notification.message,
                    notification.time,
                    notification.isUnread,
                    notification.icon
                )
            }
        }
    }

    private fun generateNotifications(): List<NotificationItem> {
        val notifications = mutableListOf<NotificationItem>()
        val calendar = Calendar.getInstance()
        val formatter = DecimalFormat("#,### đ")

        // Check spending limits
        if (userPreferences.isDailyLimitEnabled) {
            val dailyLimit = userPreferences.dailyLimit
            val todayExpense = calculateTodayExpense()

            if (todayExpense >= dailyLimit) {
                notifications.add(
                    NotificationItem(
                        "Vượt giới hạn chi tiêu hàng ngày",
                        "Bạn đã chi ${formatter.format(todayExpense)} vượt mức ${formatter.format(dailyLimit)} hôm nay",
                        "Hôm nay",
                        true,
                        R.drawable.alert_triangle
                    )
                )
            } else if (todayExpense >= dailyLimit * 0.8) {
                notifications.add(
                    NotificationItem(
                        "Sắp đạt giới hạn chi tiêu",
                        "Bạn đã chi ${(todayExpense / dailyLimit * 100).toInt()}% giới hạn hôm nay",
                        "Hôm nay",
                        true,
                        R.drawable.alert_triangle
                    )
                )
            }
        }

        if (userPreferences.isMonthlyLimitEnabled) {
            val monthlyLimit = userPreferences.monthlyLimit
            val monthExpense = calculateMonthExpense()

            if (monthExpense >= monthlyLimit) {
                notifications.add(
                    NotificationItem(
                        "Vượt giới hạn chi tiêu tháng này",
                        "Bạn đã chi ${formatter.format(monthExpense)} vượt mức ${formatter.format(monthlyLimit)} trong tháng",
                        "Tháng này",
                        true,
                        R.drawable.alert_triangle
                    )
                )
            } else if (monthExpense >= monthlyLimit * 0.8) {
                notifications.add(
                    NotificationItem(
                        "Cảnh báo chi tiêu tháng",
                        "Bạn đã chi ${(monthExpense / monthlyLimit * 100).toInt()}% giới hạn tháng này",
                        "Tháng này",
                        true,
                        R.drawable.alert_triangle
                    )
                )
            }
        }

        // Transaction statistics
        val today = calendar.get(Calendar.DAY_OF_YEAR)
        val currentYear = calendar.get(Calendar.YEAR)

        val todayTransactions = fullList.filter { transaction ->
            val transactionCalendar = Calendar.getInstance()
            transactionCalendar.timeInMillis = transaction.date
            val transactionDay = transactionCalendar.get(Calendar.DAY_OF_YEAR)
            val transactionYear = transactionCalendar.get(Calendar.YEAR)

            transactionDay == today && transactionYear == currentYear
        }

        if (todayTransactions.isNotEmpty()) {
            notifications.add(
                NotificationItem(
                    "Thống kê hôm nay",
                    "${todayTransactions.size} giao dịch - Tổng: ${formatter.format(todayTransactions.filter { it.type == 0 }.sumOf { it.amount })}",
                    "Hôm nay",
                    false,
                    R.drawable.ic_bar_chart
                )
            )
        }

        return notifications
    }

    private fun addNotificationItem(
        title: String,
        message: String,
        time: String,
        isUnread: Boolean,
        iconRes: Int
    ) {
        val itemView = layoutInflater.inflate(R.layout.item_notification, binding.layoutNotifications, false)

        itemView.findViewById<ImageView>(R.id.imgNotificationIcon).setImageResource(iconRes)
        itemView.findViewById<TextView>(R.id.tvNotificationTitle).text = title
        itemView.findViewById<TextView>(R.id.tvNotificationMessage).text = message
        itemView.findViewById<TextView>(R.id.tvNotificationTime).text = time
        itemView.findViewById<View>(R.id.viewUnreadIndicator).visibility = if (isUnread) View.VISIBLE else View.GONE

        binding.layoutNotifications.addView(itemView)
    }

    private fun calculateTodayExpense(): Double {
        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_YEAR)
        val currentYear = calendar.get(Calendar.YEAR)

        return fullList.filter { transaction ->
            if (transaction.type != 0) return@filter false

            val transactionCalendar = Calendar.getInstance()
            transactionCalendar.timeInMillis = transaction.date
            val transactionDay = transactionCalendar.get(Calendar.DAY_OF_YEAR)
            val transactionYear = transactionCalendar.get(Calendar.YEAR)

            transactionDay == today && transactionYear == currentYear
        }.sumOf { it.amount }
    }

    private fun calculateMonthExpense(): Double {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        return fullList.filter { transaction ->
            if (transaction.type != 0) return@filter false

            val transactionCalendar = Calendar.getInstance()
            transactionCalendar.timeInMillis = transaction.date
            val transactionMonth = transactionCalendar.get(Calendar.MONTH)
            val transactionYear = transactionCalendar.get(Calendar.YEAR)

            transactionMonth == currentMonth && transactionYear == currentYear
        }.sumOf { it.amount }
    }

    private fun markAllAsRead() {
        userPreferences.lastNotificationReadTime = System.currentTimeMillis()
        loadNotifications()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Mark as read when leaving
        markAllAsRead()
        _binding = null
    }

    data class NotificationItem(
        val title: String,
        val message: String,
        val time: String,
        val isUnread: Boolean,
        val icon: Int
    )
}
