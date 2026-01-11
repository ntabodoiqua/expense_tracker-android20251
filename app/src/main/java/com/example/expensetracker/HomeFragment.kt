package com.example.expensetracker

import android.content.Intent
import android.graphics.Color
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
import com.example.expensetracker.ui.AddTransactionActivity
import com.example.expensetracker.ui.TransactionAdapter
import com.example.expensetracker.viewmodel.TransactionViewModel
import java.text.DecimalFormat
import com.bumptech.glide.Glide
import com.example.expensetracker.data.Transaction

class HomeFragment : Fragment() {
    private lateinit var binding: FragmentHomeBinding
    private lateinit var viewModel: TransactionViewModel
    private lateinit var adapter: TransactionAdapter

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
        // Setup RecyclerView
        adapter = TransactionAdapter()
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        setupSwipeToDelete()
        adapter.onItemClick = { transaction ->
            val intent = Intent(requireContext(), AddTransactionActivity::class.java)
            intent.putExtra("transaction_data", transaction)
            startActivity(intent)
        }

        // Setup ViewModel
        viewModel = ViewModelProvider(this)[TransactionViewModel::class.java]

        // Quan sát dữ liệu thay đổi
        viewModel.allTransactions.observe(viewLifecycleOwner) { list ->
            // Cập nhật list vào adapter
            list?.let {
                fullList = it
                adapter.setData(it)
                updateDashboard(it)
                if (it.isEmpty()) {
                    // Nếu list rỗng -> Hiện thông báo, Ẩn RecyclerView
                    binding.layoutEmpty.visibility = android.view.View.VISIBLE
                    binding.recyclerView.visibility = android.view.View.GONE
                } else {
                    // Nếu có dữ liệu -> Ẩn thông báo, Hiện RecyclerView
                    binding.layoutEmpty.visibility = android.view.View.GONE
                    binding.recyclerView.visibility = android.view.View.VISIBLE
                }
            }

        }

        Glide.with(this) // 'this' nếu ở Activity, 'requireContext()' nếu ở Fragment
            .load(R.drawable.avatar) // Nguồn ảnh (sau này có thể là URL từ mạng)
            .circleCrop() // <-- LỆNH QUAN TRỌNG NHẤT: Cắt thành hình tròn
            .into(binding.imgAvatar) // Nơi hiển thị

        // Sự kiện click nút Thêm
        binding.fabAdd.setOnClickListener {
            val intent = Intent(requireContext(), AddTransactionActivity::class.java)
            startActivity(intent)
        }
    }

    // Hàm tính toán và cập nhật giao diện số dư
    private fun updateDashboard(list: List<Transaction>) {
        var totalIncome = 0.0
        var totalExpense = 0.0

        // 1. Duyệt qua danh sách để cộng dồn
        for (transaction in list) {
            if (transaction.type == 1) {
                // Nếu là Thu nhập (Type = 1)
                totalIncome += transaction.amount
            } else {
                // Nếu là Chi tiêu (Type = 0 hoặc khác 1)
                totalExpense += transaction.amount
            }
        }

        // 2. Tính số dư hiện tại
        val totalBalance = totalIncome - totalExpense

        // 3. Định dạng số tiền cho đẹp (VD: 5000000 -> 5.000.000 đ)
        val formatter = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("vi", "VN"))

        // 4. Gán vào TextView
        binding.currentBalanceValue.text = formatter.format(totalBalance)
        binding.tvIncome.text = formatter.format(totalIncome)   // ID này bạn vừa tạo ở bước trước
        binding.tvExpense.text = formatter.format(totalExpense) // ID này bạn vừa tạo ở bước trước
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

}