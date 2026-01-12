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

class HomeFragment : Fragment() {
    private lateinit var binding: FragmentHomeBinding
    private lateinit var viewModel: TransactionViewModel
    private lateinit var adapter: TransactionAdapter
    private lateinit var userPreferences: UserPreferences

    private var fullList: List<com.example.expensetracker.data.Transaction> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Khởi tạo UserPreferences
        userPreferences = UserPreferences(requireContext())

        // 1. Setup RecyclerView
        adapter = TransactionAdapter()
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        setupSwipeToDelete()

        // 2. Setup ViewModel
        viewModel = ViewModelProvider(this)[TransactionViewModel::class.java]

        // 3. Quan sát dữ liệu
        viewModel.allTransactions.observe(viewLifecycleOwner) { list ->
            list?.let {
                fullList = it
                // Lọc chỉ hiển thị giao dịch 2 ngày gần đây
                val recentTransactions = filterRecentTransactions(it)
                adapter.setData(recentTransactions)
                updateDashboard(it)
                updateNotificationBadge() // Update badge when data changes
                if (recentTransactions.isEmpty()) {
                    binding.layoutEmpty.visibility = View.VISIBLE
                    binding.recyclerView.visibility = View.GONE
                } else {
                    binding.layoutEmpty.visibility = View.GONE
                    binding.recyclerView.visibility = View.VISIBLE
                }
            }
        }

        // 4. Load thông tin người dùng
        loadUserInfo()

        // 5. Xử lý sự kiện nút thông báo
        binding.imageView3.setOnClickListener {
            openNotificationPage()
        }

        // 6. Xử lý nút "Xem thêm" - chuyển sang trang tìm kiếm
        binding.btnViewMore.setOnClickListener {
            openSearchPage()
        }

        // Cập nhật badge thông báo
        updateNotificationBadge()

        // ==============================================================
        // 6. XỬ LÝ SỰ KIỆN SỬA (BẤM VÀO ITEM) - Đã sửa lỗi Intent
        // ==============================================================
        adapter.onItemClick = { transaction ->
            // Tạo BottomSheet
            val bottomSheet = AddTransactionFragment()

            // Đóng gói dữ liệu cũ gửi sang
            val bundle = Bundle()
            bundle.putSerializable("transaction_data", transaction)
            bottomSheet.arguments = bundle

            // Xử lý khi bấm nút "Cập nhật" ở bên kia
            bottomSheet.onSaveClick = { amount, typeStr, category, note, date ->
                // Convert chuỗi "Thu nhập" -> số 1, "Chi tiêu" -> số 0
                val typeInt = if (typeStr == "Thu nhập") 1 else 0

                // Tạo đối tượng mới dựa trên cái cũ (giữ nguyên ID, cập nhật ngày)
                val updatedTransaction = transaction.copy(
                    amount = amount,
                    type = typeInt,
                    category = category,
                    note = note,
                    date = date
                )

                // Gọi ViewModel Update
                viewModel.updateTransaction(updatedTransaction)
                Toast.makeText(context, "Đã cập nhật!", Toast.LENGTH_SHORT).show()
            }

            bottomSheet.show(parentFragmentManager, "EditTransactionTag")
        }

        // ==============================================================
        // 6. XỬ LÝ SỰ KIỆN THÊM MỚI (BẤM NÚT FAB)
        // ==============================================================
        binding.fabAdd.setOnClickListener {
            val bottomSheet = AddTransactionFragment()

            // Xử lý khi bấm nút "Lưu" ở bên kia
            bottomSheet.onSaveClick = { amount, typeStr, category, note, date ->
                val typeInt = if (typeStr == "Thu nhập") 1 else 0

                // Tạo giao dịch mới (ID = 0 để Room tự tăng)
                val newTransaction = Transaction(
                    id = 0,
                    title = category,
                    amount = amount,
                    type = typeInt,
                    category = category,
                    note = note,
                    date = date // Sử dụng ngày đã chọn từ DatePicker
                )

                // Gọi ViewModel Insert
                viewModel.addTransaction(newTransaction)
                Toast.makeText(context, "Đã thêm giao dịch!", Toast.LENGTH_SHORT).show()
            }

            bottomSheet.show(parentFragmentManager, "AddTransactionTag")
        }
    }

    // Hàm tính toán và cập nhật giao diện số dư
    private fun updateDashboard(list: List<Transaction>) {
        var totalIncome = 0.0
        var totalExpense = 0.0
        var todayExpense = 0.0
        var monthExpense = 0.0

        // Lấy ngày hiện tại
        val calendar = java.util.Calendar.getInstance()
        val today = calendar.get(java.util.Calendar.DAY_OF_YEAR)
        val currentYear = calendar.get(java.util.Calendar.YEAR)
        val currentMonth = calendar.get(java.util.Calendar.MONTH)

        // 1. Duyệt qua danh sách để cộng dồn
        for (transaction in list) {
            if (transaction.type == 1) {
                // Nếu là Thu nhập (Type = 1)
                totalIncome += transaction.amount
            } else {
                // Nếu là Chi tiêu (Type = 0 hoặc khác 1)
                totalExpense += transaction.amount

                // Tính chi tiêu theo ngày và tháng
                val transactionCalendar = java.util.Calendar.getInstance()
                transactionCalendar.timeInMillis = transaction.date

                val transactionDay = transactionCalendar.get(java.util.Calendar.DAY_OF_YEAR)
                val transactionYear = transactionCalendar.get(java.util.Calendar.YEAR)
                val transactionMonth = transactionCalendar.get(java.util.Calendar.MONTH)

                // Chi tiêu trong ngày
                if (transactionDay == today && transactionYear == currentYear) {
                    todayExpense += transaction.amount
                }

                // Chi tiêu trong tháng
                if (transactionMonth == currentMonth && transactionYear == currentYear) {
                    monthExpense += transaction.amount
                }
            }
        }

        // 2. Tính số dư hiện tại
        val totalBalance = totalIncome - totalExpense

        // 3. Định dạng số tiền cho đẹp (VD: 5000000 -> 5.000.000 đ)
        val formatter = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("vi", "VN"))

        // 4. Gán vào TextView
        binding.currentBalanceValue.text = formatter.format(totalBalance)
        binding.tvIncome.text = formatter.format(totalIncome)
        binding.tvExpense.text = formatter.format(totalExpense)

        // 5. Kiểm tra giới hạn chi tiêu và hiển thị cảnh báo
        checkSpendingLimits(todayExpense, monthExpense, formatter)
    }

    private fun filterRecentTransactions(list: List<Transaction>): List<Transaction> {
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.DAY_OF_YEAR, -2) // Lùi lại 2 ngày
        val twoDaysAgo = calendar.timeInMillis
        
        return list.filter { transaction ->
            transaction.date >= twoDaysAgo
        }.sortedByDescending { it.date } // Sắp xếp mới nhất trước
    }

    private fun openSearchPage() {
        val searchFragment = SearchFragment()
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, searchFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun checkSpendingLimits(todayExpense: Double, monthExpense: Double, formatter: java.text.NumberFormat) {
        val warnings = mutableListOf<String>()

        // Kiểm tra giới hạn ngày
        if (userPreferences.isDailyLimitEnabled && userPreferences.dailyLimit > 0) {
            val dailyLimit = userPreferences.dailyLimit
            val dailyPercent = (todayExpense / dailyLimit * 100).toInt()

            if (todayExpense >= dailyLimit) {
                warnings.add("🚨 Đã vượt giới hạn ngày!\nĐã chi: ${formatter.format(todayExpense)} / ${formatter.format(dailyLimit)}")
            } else if (dailyPercent >= 80) {
                warnings.add("⚠️ Sắp đạt giới hạn ngày (${dailyPercent}%)\nĐã chi: ${formatter.format(todayExpense)} / ${formatter.format(dailyLimit)}")
            }
        }

        // Kiểm tra giới hạn tháng
        if (userPreferences.isMonthlyLimitEnabled && userPreferences.monthlyLimit > 0) {
            val monthlyLimit = userPreferences.monthlyLimit
            val monthlyPercent = (monthExpense / monthlyLimit * 100).toInt()

            if (monthExpense >= monthlyLimit) {
                warnings.add("🚨 Đã vượt giới hạn tháng!\nĐã chi: ${formatter.format(monthExpense)} / ${formatter.format(monthlyLimit)}")
            } else if (monthlyPercent >= 80) {
                warnings.add("⚠️ Sắp đạt giới hạn tháng (${monthlyPercent}%)\nĐã chi: ${formatter.format(monthExpense)} / ${formatter.format(monthlyLimit)}")
            }
        }

        // Hiển thị cảnh báo nếu có
        if (warnings.isNotEmpty()) {
            showSpendingWarning(warnings)
        } else {
            hideSpendingWarning()
        }
    }

    private fun showSpendingWarning(warnings: List<String>) {
        binding.layoutWarning.visibility = View.VISIBLE
        binding.tvWarning.text = warnings.joinToString("\n\n")
    }

    private fun hideSpendingWarning() {
        binding.layoutWarning.visibility = View.GONE
    }

    private fun setupSwipeToDelete() {
        // Tạo callback xử lý sự kiện vuốt
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false // Chúng ta không làm tính năng kéo thả (Drag & Drop) nên return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // 1. Lấy vị trí item vừa vuốt
                val position = viewHolder.adapterPosition

                // 2. Lấy đối tượng Transaction từ Adapter
                val transactionToDelete = adapter.getTransactionAt(position)

                // 3. Gọi ViewModel để xóa khỏi Database
                viewModel.deleteTransaction(transactionToDelete)

                // 4. Thông báo cho người dùng (có thể thêm nút Hoàn tác/Undo nếu muốn)
                Toast.makeText(requireContext(), "Đã xóa!", Toast.LENGTH_SHORT).show()
            }
        }

        // Gắn helper vào RecyclerView
        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)
    }

    // Load thông tin người dùng từ SharedPreferences
    private fun loadUserInfo() {
        // Hiển thị tên người dùng
        binding.Username.text = userPreferences.userName

        // Hiển thị avatar
        val avatarUri = userPreferences.userAvatar
        if (avatarUri.isNotEmpty()) {
            try {
                Glide.with(this)
                    .load(Uri.parse(avatarUri))
                    .circleCrop()
                    .placeholder(R.drawable.avatar)
                    .error(R.drawable.avatar)
                    .into(binding.imgAvatar)
            } catch (e: Exception) {
                loadDefaultAvatar()
            }
        } else {
            loadDefaultAvatar()
        }
    }

    private fun loadDefaultAvatar() {
        Glide.with(this)
            .load(R.drawable.avatar)
            .circleCrop()
            .into(binding.imgAvatar)
    }

    private fun openNotificationPage() {
        val notificationFragment = NotificationFragment()
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, notificationFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun updateNotificationBadge() {
        // Count unread notifications
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
        
        // Check spending limit notifications
        if (userPreferences.isDailyLimitEnabled) {
            val todayExpense = calculateTodayExpense()
            val dailyLimit = userPreferences.dailyLimit
            if (todayExpense >= dailyLimit * 0.8) {
                count++
            }
        }
        
        if (userPreferences.isMonthlyLimitEnabled) {
            val monthExpense = calculateMonthExpense()
            val monthlyLimit = userPreferences.monthlyLimit
            if (monthExpense >= monthlyLimit * 0.8) {
                count++
            }
        }
        
        // Check for today's transactions
        val todayTransactions = fullList.filter { transaction ->
            val calendar = java.util.Calendar.getInstance()
            val today = calendar.get(java.util.Calendar.DAY_OF_YEAR)
            val currentYear = calendar.get(java.util.Calendar.YEAR)
            
            val transactionCalendar = java.util.Calendar.getInstance()
            transactionCalendar.timeInMillis = transaction.date
            val transactionDay = transactionCalendar.get(java.util.Calendar.DAY_OF_YEAR)
            val transactionYear = transactionCalendar.get(java.util.Calendar.YEAR)
            
            transactionDay == today && transactionYear == currentYear
        }
        
        if (todayTransactions.isNotEmpty()) {
            count++
        }
        
        return count
    }

    private fun hasUnreadNotifications(): Boolean {
        val lastReadTime = userPreferences.lastNotificationReadTime
        
        // Check if there are new spending limit warnings since last read
        if (userPreferences.isDailyLimitEnabled) {
            val todayExpense = calculateTodayExpense()
            val dailyLimit = userPreferences.dailyLimit
            if (todayExpense >= dailyLimit * 0.8) {
                return true
            }
        }
        
        if (userPreferences.isMonthlyLimitEnabled) {
            val monthExpense = calculateMonthExpense()
            val monthlyLimit = userPreferences.monthlyLimit
            if (monthExpense >= monthlyLimit * 0.8) {
                return true
            }
        }
        
        return false
    }

    private fun showNotifications() {
        // Tính toán thông báo dựa trên chi tiêu
        val notifications = mutableListOf<String>()
        
        // Kiểm tra giới hạn chi tiêu
        if (userPreferences.isDailyLimitEnabled) {
            val dailyLimit = userPreferences.dailyLimit
            val todayExpense = calculateTodayExpense()
            
            if (todayExpense >= dailyLimit) {
                notifications.add("⚠️ Bạn đã vượt giới hạn chi tiêu hàng ngày!")
            } else if (todayExpense >= dailyLimit * 0.8) {
                notifications.add("⚡ Bạn đã chi ${(todayExpense / dailyLimit * 100).toInt()}% giới hạn ngày hôm nay")
            }
        }
        
        if (userPreferences.isMonthlyLimitEnabled) {
            val monthlyLimit = userPreferences.monthlyLimit
            val monthExpense = calculateMonthExpense()
            
            if (monthExpense >= monthlyLimit) {
                notifications.add("⚠️ Bạn đã vượt giới hạn chi tiêu tháng này!")
            } else if (monthExpense >= monthlyLimit * 0.8) {
                notifications.add("⚡ Bạn đã chi ${(monthExpense / monthlyLimit * 100).toInt()}% giới hạn tháng này")
            }
        }
        
        // Thêm thông tin thống kê
        val transactionCount = fullList.size
        val todayTransactions = fullList.filter { transaction ->
            val calendar = java.util.Calendar.getInstance()
            val today = calendar.get(java.util.Calendar.DAY_OF_YEAR)
            val currentYear = calendar.get(java.util.Calendar.YEAR)
            
            val transactionCalendar = java.util.Calendar.getInstance()
            transactionCalendar.timeInMillis = transaction.date
            val transactionDay = transactionCalendar.get(java.util.Calendar.DAY_OF_YEAR)
            val transactionYear = transactionCalendar.get(java.util.Calendar.YEAR)
            
            transactionDay == today && transactionYear == currentYear
        }.size
        
        notifications.add("📊 Hôm nay: $todayTransactions giao dịch")
        notifications.add("📈 Tổng cộng: $transactionCount giao dịch")
        
        // Hiển thị dialog
        val message = if (notifications.isEmpty()) {
            "🔔 Không có thông báo mới"
        } else {
            notifications.joinToString("\n\n")
        }
        
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("🔔 Thông báo")
            .setMessage(message)
            .setPositiveButton("Đóng", null)
            .show()
    }
    
    private fun calculateTodayExpense(): Double {
        val calendar = java.util.Calendar.getInstance()
        val today = calendar.get(java.util.Calendar.DAY_OF_YEAR)
        val currentYear = calendar.get(java.util.Calendar.YEAR)
        
        return fullList.filter { transaction ->
            if (transaction.type != 0) return@filter false
            
            val transactionCalendar = java.util.Calendar.getInstance()
            transactionCalendar.timeInMillis = transaction.date
            val transactionDay = transactionCalendar.get(java.util.Calendar.DAY_OF_YEAR)
            val transactionYear = transactionCalendar.get(java.util.Calendar.YEAR)
            
            transactionDay == today && transactionYear == currentYear
        }.sumOf { it.amount }
    }
    
    private fun calculateMonthExpense(): Double {
        val calendar = java.util.Calendar.getInstance()
        val currentMonth = calendar.get(java.util.Calendar.MONTH)
        val currentYear = calendar.get(java.util.Calendar.YEAR)
        
        return fullList.filter { transaction ->
            if (transaction.type != 0) return@filter false
            
            val transactionCalendar = java.util.Calendar.getInstance()
            transactionCalendar.timeInMillis = transaction.date
            val transactionMonth = transactionCalendar.get(java.util.Calendar.MONTH)
            val transactionYear = transactionCalendar.get(java.util.Calendar.YEAR)
            
            transactionMonth == currentMonth && transactionYear == currentYear
        }.sumOf { it.amount }
    }

    // Refresh thông tin khi quay lại Fragment
    override fun onResume() {
        super.onResume()
        loadUserInfo()
    }

}