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
        binding.btnMarkAllRead.setOnClickListener {
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
                addNotificationItem(notification)
            }
        }
    }

    private fun generateNotifications(): List<NotificationItem> {
        val notifications = mutableListOf<NotificationItem>()
        val formatter = DecimalFormat("#,### đ")
        // Lấy thời gian đọc lần cuối
        val lastReadTime = userPreferences.lastNotificationReadTime

        fun isNew(timestamp: Long): Boolean {
            return timestamp > lastReadTime
        }

        // Thời gian đầu ngày hôm nay (00:00:00)
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startOfToday = calendar.timeInMillis

        // 1. KIỂM TRA GIỚI HẠN NGÀY
        if (userPreferences.isDailyLimitEnabled) {
            val dailyLimit = userPreferences.dailyLimit
            val todayExpense = calculateTodayExpense()
            val isUnread = lastReadTime < startOfToday
            if (todayExpense >= dailyLimit) {
                // MỨC ĐỘ: NGUY HIỂM (Màu Đỏ)
                notifications.add(
                    NotificationItem(
                        "Vượt giới hạn ngày!",
                        "Bạn đã chi ${formatter.format(todayExpense)} (Vượt mức ${formatter.format(dailyLimit)})",
                        "Hôm nay",
                        isUnread,
                        R.drawable.triangle_alert,
                        themeColor = "#ef4444",
                        backgroundColor = "#fee2e2"
                    )
                )
            } else if (todayExpense >= dailyLimit * 0.8) {
                // MỨC ĐỘ: CẢNH BÁO (Màu Cam)
                notifications.add(
                    NotificationItem(
                        "Sắp đạt giới hạn ngày",
                        "Đã chi ${(todayExpense / dailyLimit * 100).toInt()}% giới hạn cho phép.",
                        "Hôm nay",
                        isUnread,
                        R.drawable.siren,
                        themeColor = "#f97316",
                        backgroundColor = "#ffedd5"
                    )
                )
            }
        }

        // 2. KIỂM TRA GIỚI HẠN THÁNG
        if (userPreferences.isMonthlyLimitEnabled) {
            val monthlyLimit = userPreferences.monthlyLimit
            val monthExpense = calculateMonthExpense()
            val isUnread = lastReadTime < startOfToday
            if (monthExpense >= monthlyLimit) {
                // MỨC ĐỘ: NGUY HIỂM (Màu Đỏ)
                notifications.add(
                    NotificationItem(
                        "Vượt giới hạn tháng!",
                        "Bạn đã chi ${formatter.format(monthExpense)} (Vượt mức ${formatter.format(monthlyLimit)})",
                        "Tháng này",
                        isUnread,
                        R.drawable.triangle_alert,
                        themeColor = "#ef4444",
                        backgroundColor = "#fee2e2"
                    )
                )
            } else if (monthExpense >= monthlyLimit * 0.8) {
                // MỨC ĐỘ: CẢNH BÁO (Màu Cam)
                notifications.add(
                    NotificationItem(
                        "Sắp đạt giới hạn tháng",
                        "Đã chi ${(monthExpense / monthlyLimit * 100).toInt()}% giới hạn cho phép.",
                        "Tháng này",
                        isUnread,
                        R.drawable.siren,
                        themeColor = "#f97316",
                        backgroundColor = "#ffedd5"
                    )
                )
            }
        }

        // 3. THỐNG KÊ (Màu Xanh dương)
        val todayTransactions = getTodayTransactions()
        if (todayTransactions.isNotEmpty()) {
            // Lấy thời gian giao dịch mới nhất
            val latestTxTime = todayTransactions.maxOf { it.date }
            // Nếu giao dịch mới nhất diễn ra SAU lần cuối bấm đọc -> Chưa đọc
            val isTxUnread = latestTxTime > lastReadTime
            val totalSpent = todayTransactions.filter { it.type == 0 }.sumOf { it.amount }
            notifications.add(
                NotificationItem(
                    "Thống kê hôm nay",
                    "Có ${todayTransactions.size} giao dịch mới. Tổng chi: ${formatter.format(totalSpent)}",
                    "Hôm nay",
                    isTxUnread,
                    R.drawable.chart_pie,
                    themeColor = "#3b82f6",
                    backgroundColor = "#dbeafe"
                )
            )
        }

        return notifications
    }

    private fun addNotificationItem(item: NotificationItem) {
        val itemView = layoutInflater.inflate(R.layout.item_notification, binding.layoutNotifications, false)

        val imgIcon = itemView.findViewById<ImageView>(R.id.imgNotificationIcon)
        val cardIcon = itemView.findViewById<CardView>(R.id.cardIcon)
        val tvTitle = itemView.findViewById<TextView>(R.id.tvNotificationTitle)
        val tvMessage = itemView.findViewById<TextView>(R.id.tvNotificationMessage)
        val tvTime = itemView.findViewById<TextView>(R.id.tvNotificationTime)
        val viewUnread = itemView.findViewById<View>(R.id.viewUnreadIndicator)

        // Set Data
        tvTitle.text = item.title
        tvMessage.text = item.message
        tvTime.text = item.time
        imgIcon.setImageResource(item.icon)

        try {
            // 1. Đổi màu icon (Tint)
            imgIcon.setColorFilter(Color.parseColor(item.themeColor))

            // 2. Đổi màu nền CardView
            cardIcon.setCardBackgroundColor(Color.parseColor(item.backgroundColor))
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Ẩn hiện chấm đỏ chưa đọc
        viewUnread.visibility = if (item.isUnread) View.VISIBLE else View.GONE

        binding.layoutNotifications.addView(itemView)
    }

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

    private fun calculateTodayExpense(): Double {
        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_YEAR)
        val currentYear = calendar.get(Calendar.YEAR)
        return fullList.filter {
            it.type == 0 && isSameDay(it.date, today, currentYear)
        }.sumOf { it.amount }
    }

    private fun calculateMonthExpense(): Double {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)
        return fullList.filter {
            it.type == 0 && isSameMonth(it.date, currentMonth, currentYear)
        }.sumOf { it.amount }
    }

    // Helper function check ngày
    private fun isSameDay(date: Long, day: Int, year: Int): Boolean {
        val cal = Calendar.getInstance()
        cal.timeInMillis = date
        return cal.get(Calendar.DAY_OF_YEAR) == day && cal.get(Calendar.YEAR) == year
    }

    // Helper function check tháng
    private fun isSameMonth(date: Long, month: Int, year: Int): Boolean {
        val cal = Calendar.getInstance()
        cal.timeInMillis = date
        return cal.get(Calendar.MONTH) == month && cal.get(Calendar.YEAR) == year
    }

    private fun markAllAsRead() {
        userPreferences.lastNotificationReadTime = System.currentTimeMillis()
        loadNotifications()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        markAllAsRead()
        _binding = null
    }

    // Data Class cập nhật thêm màu sắc
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