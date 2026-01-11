package com.example.expensetracker.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.expensetracker.R
import com.example.expensetracker.data.Transaction
import com.example.expensetracker.databinding.ItemTransactionBinding
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransactionAdapter : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    private var transactions = emptyList<Transaction>()

    var onItemClick: ((Transaction) -> Unit)? = null

    class TransactionViewHolder(val binding: ItemTransactionBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val currentItem = transactions[position]

        // Gán dữ liệu lên giao diện
        holder.binding.tvTitle.text = currentItem.title

        // Format ngày tháng (ví dụ: 09/01/2026)
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        holder.binding.tvDate.text = sdf.format(Date(currentItem.date))

        // Format tiền tệ (ví dụ: 50,000 đ)
        val formatter = DecimalFormat("#,### đ")
        holder.binding.tvAmount.text = formatter.format(currentItem.amount)

        // Logic màu sắc: Thu nhập màu Xanh, Chi tiêu màu Đỏ
        if (currentItem.type == 1) { // Thu
            holder.binding.tvAmount.setTextColor(Color.parseColor("#4CAF50")) // Xanh
            holder.binding.tvAmount.text = "+ ${formatter.format(currentItem.amount)}"
        } else { // Chi
            holder.binding.tvAmount.setTextColor(Color.parseColor("#F44336")) // Đỏ
            holder.binding.tvAmount.text = "- ${formatter.format(currentItem.amount)}"
        }
        holder.itemView.setOnClickListener {
            onItemClick?.invoke(currentItem)
        }
        // Dựa vào tên Category để chọn ảnh
        when (currentItem.category) {
            "Ăn uống" -> holder.binding.imgIcon.setImageResource(R.drawable.ic_food)
            "Đi lại" -> holder.binding.imgIcon.setImageResource(R.drawable.ic_transport)
            "Mua sắm" -> holder.binding.imgIcon.setImageResource(R.drawable.ic_shopping)
            "Lương", "Thưởng" -> holder.binding.imgIcon.setImageResource(R.drawable.ic_money)
            else -> holder.binding.imgIcon.setImageResource(R.drawable.ic_other)
        }
    }

    override fun getItemCount() = transactions.size

    // Hàm để cập nhật dữ liệu mới từ Activity
    fun setData(newTransactions: List<Transaction>) {
        this.transactions = newTransactions
        notifyDataSetChanged()
    }

    fun getTransactionAt(position: Int): Transaction {
        return transactions[position]
    }
}