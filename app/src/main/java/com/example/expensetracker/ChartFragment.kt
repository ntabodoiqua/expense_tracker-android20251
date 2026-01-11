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
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.bumptech.glide.Glide
import com.example.expensetracker.databinding.FragmentChartBinding

class ChartFragment : Fragment() {
    private lateinit var binding: FragmentChartBinding
    private lateinit var viewModel: TransactionViewModel
    private lateinit var adapter: TransactionAdapter

    private var fullList: List<com.example.expensetracker.data.Transaction> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentChartBinding.inflate(inflater, container, false)
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
                calculateBalance(it)
                updatePieChart(it)
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

    private fun calculateBalance(list: List<com.example.expensetracker.data.Transaction>) {
        var total = 0.0
        for (item in list) {
            if (item.type == 1) { // Thu
                total += item.amount
            } else { // Chi
                total -= item.amount
            }
        }

        val formatter = DecimalFormat("#,### đ")
        binding.currentBalanceValue.text = formatter.format(total)
    }
    private fun updatePieChart(list: List<com.example.expensetracker.data.Transaction>) {
        var totalExpense = 0.0
        var totalIncome = 0.0

        // Tính tổng thu và chi
        for (item in list) {
            if (item.type == 1) totalIncome += item.amount
            else totalExpense += item.amount
        }

        // Tạo dữ liệu cho biểu đồ
        val entries = ArrayList<PieEntry>()
        if (totalExpense > 0) entries.add(PieEntry(totalExpense.toFloat(), "Chi tiêu"))
        if (totalIncome > 0) entries.add(PieEntry(totalIncome.toFloat(), "Thu nhập"))

        // Cấu hình màu sắc, text
        val dataSet = PieDataSet(entries, "")
        dataSet.colors = listOf(Color.parseColor("#F44336"), Color.parseColor("#4CAF50")) // Đỏ cho Chi, Xanh cho Thu
        dataSet.valueTextSize = 14f
        dataSet.valueTextColor = Color.WHITE

        val data = PieData(dataSet)

        // Setup Chart
        binding.pieChart.data = data
        binding.pieChart.description.isEnabled = false // Tắt dòng chú thích góc dưới
        binding.pieChart.centerText = "Tổng quan"
        binding.pieChart.setCenterTextSize(16f)
        binding.pieChart.animateY(1000) // Hiệu ứng xoay xoay khi hiện
        binding.pieChart.invalidate() // Vẽ lại
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