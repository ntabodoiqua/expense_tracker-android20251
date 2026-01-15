package com.example.expensetracker

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
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

    // Khởi tạo giao diện từ XML binding
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNotificationBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Thiết lập ViewModel và quan sát dữ liệu
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        userPreferences = UserPreferences(requireContext())
        viewModel = ViewModelProvider(this)[TransactionViewModel::class.java]
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        binding.btnMarkAllRead.setOnClickListener {
            markAllAsRead()
        }
        viewModel.allTransactions.observe(viewLifecycleOwner) { list ->
            fullList = list ?: emptyList()
            loadNotifications()
        }
    }

    // Tải và hiển thị danh sách thông báo
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
                addNotificationItem(notification)
            }
        }
    }

    // Tạo danh sách thông báo dựa trên dữ liệu chi tiêu
    private fun generateNotifications(): List<NotificationItem> {
        val notifications = mutableListOf<NotificationItem>()
        val formatter = DecimalFormat("#,### đ")
        val lastReadTime = userPreferences.lastNotificationReadTime
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startOfToday = calendar.timeInMillis
        if (userPreferences.isDailyLimitEnabled) {
            val dailyLimit = userPreferences.dailyLimit
            val todayExpense = calculateTodayExpense()
            val isUnread = lastReadTime < startOfToday
            if (todayExpense >= dailyLimit) {
                notifications.add(
                    NotificationItem(
                        "Vượt giới hạn ngày!",
                        "Bạn đã chi ${formatter.format(todayExpense)} (Vượt mức ${formatter.format(dailyLimit)})",
                        "Hôm nay", isUnread, R.drawable.triangle_alert,
                        themeColor = "#ef4444", backgroundColor = "#fee2e2"
                    )
                )
            } else if (todayExpense >= dailyLimit * 0.8) {
                notifications.add(
                    NotificationItem(
                        "Sắp đạt giới hạn ngày",
                        "Đã chi ${(todayExpense / dailyLimit * 100).toInt()}% giới hạn cho phép.",
                        "Hôm nay", isUnread, R.drawable.siren,
                        themeColor = "#f97316", backgroundColor = "#ffedd5"
                    )
                )
            }
        }
        if (userPreferences.isMonthlyLimitEnabled) {
            val monthlyLimit = userPreferences.monthlyLimit
            val monthExpense = calculateMonthExpense()
            val isUnread = lastReadTime < startOfToday
            if (monthExpense >= monthlyLimit) {
                notifications.add(
                    NotificationItem(
                        "Vượt giới hạn tháng!",
                        "Bạn đã chi ${formatter.format(monthExpense)} (Vượt mức ${formatter.format(monthlyLimit)})",
                        "Tháng này", isUnread, R.drawable.triangle_alert,
                        themeColor = "#ef4444", backgroundColor = "#fee2e2"
                    )
                )
            } else if (monthExpense >= monthlyLimit * 0.8) {
                notifications.add(
                    NotificationItem(
                        "Sắp đạt giới hạn tháng",
                        "Đã chi ${(monthExpense / monthlyLimit * 100).toInt()}% giới hạn cho phép.",
                        "Tháng này", isUnread, R.drawable.siren,
                        themeColor = "#f97316", backgroundColor = "#ffedd5"
                    )
                )
            }
        }
        val todayTransactions = getTodayTransactions()
        if (todayTransactions.isNotEmpty()) {
            val latestTxTime = todayTransactions.maxOf { it.date }
            val isTxUnread = latestTxTime > lastReadTime
            val totalSpent = todayTransactions.filter { it.type == 0 }.sumOf { it.amount }
            notifications.add(
                NotificationItem(
                    "Thống kê hôm nay",
                    "Có ${todayTransactions.size} giao dịch mới. Tổng chi: ${formatter.format(totalSpent)}",
                    "Hôm nay", isTxUnread, R.drawable.chart_pie,
                    themeColor = "#3b82f6", backgroundColor = "#dbeafe"
                )
            )
        }
        return notifications
    }

    // Thêm item thông báo vào layout
    private fun addNotificationItem(item: NotificationItem) {
        val itemView = layoutInflater.inflate(R.layout.item_notification, binding.layoutNotifications, false)
        val imgIcon = itemView.findViewById<ImageView>(R.id.imgNotificationIcon)
        val cardIcon = itemView.findViewById<CardView>(R.id.cardIcon)
        val tvTitle = itemView.findViewById<TextView>(R.id.tvNotificationTitle)
        val tvMessage = itemView.findViewById<TextView>(R.id.tvNotificationMessage)
        val tvTime = itemView.findViewById<TextView>(R.id.tvNotificationTime)
        val viewUnread = itemView.findViewById<View>(R.id.viewUnreadIndicator)
        tvTitle.text = item.title
        tvMessage.text = item.message
        tvTime.text = item.time
        imgIcon.setImageResource(item.icon)
        try {
            imgIcon.setColorFilter(Color.parseColor(item.themeColor))
            cardIcon.setCardBackgroundColor(Color.parseColor(item.backgroundColor))
        } catch (e: Exception) {
            e.printStackTrace()
        }
        viewUnread.visibility = if (item.isUnread) View.VISIBLE else View.GONE
        binding.layoutNotifications.addView(itemView)
    }

    // Lấy danh sách giao dịch hôm nay
    private fun getTodayTransactions(): List<com.example.expensetracker.data.Transaction> {
        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_YEAR)
        val currentYear = calendar.get(Calendar.YEAR)
        return fullList.filter { transaction ->
            val tCal = Calendar.getInstance()
            tCal.timeInMillis = transaction.date
            tCal.get(Calendar.DAY_OF_YEAR) == today && tCal.get(Calendar.YEAR) == currentYear
        }
    }

    // Tính tổng chi tiêu hôm nay
    private fun calculateTodayExpense(): Double {
        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_YEAR)
        val currentYear = calendar.get(Calendar.YEAR)
        return fullList.filter {
            it.type == 0 && isSameDay(it.date, today, currentYear)
        }.sumOf { it.amount }
    }

    // Tính tổng chi tiêu tháng này
    private fun calculateMonthExpense(): Double {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)
        return fullList.filter {
            it.type == 0 && isSameMonth(it.date, currentMonth, currentYear)
        }.sumOf { it.amount }
    }

    // Kiểm tra xem ngày có trùng không
    private fun isSameDay(date: Long, day: Int, year: Int): Boolean {
        val cal = Calendar.getInstance()
        cal.timeInMillis = date
        return cal.get(Calendar.DAY_OF_YEAR) == day && cal.get(Calendar.YEAR) == year
    }

    // Kiểm tra xem tháng có trùng không
    private fun isSameMonth(date: Long, month: Int, year: Int): Boolean {
        val cal = Calendar.getInstance()
        cal.timeInMillis = date
        return cal.get(Calendar.MONTH) == month && cal.get(Calendar.YEAR) == year
    }

    // Đánh dấu tất cả thông báo đã đọc
    private fun markAllAsRead() {
        userPreferences.lastNotificationReadTime = System.currentTimeMillis()
        loadNotifications()
    }

    // Giải phóng binding và đánh dấu đã đọc khi view bị hủy
    override fun onDestroyView() {
        super.onDestroyView()
        markAllAsRead()
        _binding = null
    }

    data class NotificationItem(
        val title: String,
        val message: String,
        val time: String,
        val isUnread: Boolean,
        val icon: Int,
        val themeColor: String,
        val backgroundColor: String
    )
}
