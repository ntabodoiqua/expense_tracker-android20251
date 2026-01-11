package com.example.expensetracker

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.expensetracker.databinding.FragmentHomeBinding
import com.example.expensetracker.databinding.FragmentSearchBinding
import com.example.expensetracker.ui.AddTransactionActivity
import com.example.expensetracker.ui.TransactionAdapter
import com.example.expensetracker.viewmodel.TransactionViewModel

class SearchFragment : Fragment() {
    private lateinit var binding: FragmentSearchBinding
    private lateinit var viewModel: TransactionViewModel
    private lateinit var adapter: TransactionAdapter
    private var fullList: List<com.example.expensetracker.data.Transaction> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Setup RecyclerView
        adapter = TransactionAdapter()

        adapter.onItemClick = { transaction ->
            val intent = Intent(requireContext(), AddTransactionActivity::class.java)
            intent.putExtra("transaction_data", transaction)
            startActivity(intent)
        }


        // Xử lý tìm kiếm
        binding.searchView.setOnQueryTextListener(object :
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterList(newText)
                return true
            }
        })
    }
    // Hàm lọc danh sách
    private fun filterList(query: String?) {
        if (query != null) {
            val filteredList = fullList.filter {
                // Tìm theo Tiêu đề HOẶC Ghi chú (không phân biệt hoa thường)
                it.title.lowercase().contains(query.lowercase()) ||
                        it.note.lowercase().contains(query.lowercase())
            }

            if (filteredList.isEmpty()) {
                // Nếu tìm không thấy thì hiện list rỗng
                adapter.setData(emptyList())
            } else {
                adapter.setData(filteredList)
            }
        }
    }


}