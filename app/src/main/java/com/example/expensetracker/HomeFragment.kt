package com.example.expensetracker

import android.content.Intent
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
import com.example.expensetracker.ui.AddTransactionFragment

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
                adapter.setData(it)
                updateDashboard(it)
                if (it.isEmpty()) {
                    binding.layoutEmpty.visibility = View.VISIBLE
                    binding.recyclerView.visibility = View.GONE
                } else {
                    binding.layoutEmpty.visibility = View.GONE
                    binding.recyclerView.visibility = View.VISIBLE
                }
            }
        }

        // 4. Load Avatar tròn
        Glide.with(this)
            .load(R.drawable.avatar)
            .circleCrop()
            .into(binding.imgAvatar)

        // ==============================================================
        // 5. XỬ LÝ SỰ KIỆN SỬA (BẤM VÀO ITEM) - Đã sửa lỗi Intent
        // ==============================================================
        adapter.onItemClick = { transaction ->
            // Tạo BottomSheet
            val bottomSheet = AddTransactionFragment()

            // Đóng gói dữ liệu cũ gửi sang
            val bundle = Bundle()
            bundle.putSerializable("transaction_data", transaction)
            bottomSheet.arguments = bundle

            // Xử lý khi bấm nút "Cập nhật" ở bên kia
            bottomSheet.onSaveClick = { amount, typeStr, category, note ->
                // Convert chuỗi "Thu nhập" -> số 1, "Chi tiêu" -> số 0
                val typeInt = if (typeStr == "Thu nhập") 1 else 0

                // Tạo đối tượng mới dựa trên cái cũ (giữ nguyên ID và Ngày tháng)
                val updatedTransaction = transaction.copy(
                    amount = amount,
                    type = typeInt,
                    category = category,
                    note = note
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
            bottomSheet.onSaveClick = { amount, typeStr, category, note ->
                val typeInt = if (typeStr == "Thu nhập") 1 else 0

                // Tạo giao dịch mới (ID = 0 để Room tự tăng)
                val newTransaction = Transaction(
                    id = 0,
                    title = category,
                    amount = amount,
                    type = typeInt,
                    category = category,
                    note = note,
                    date = System.currentTimeMillis() // Lấy giờ hiện tại
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