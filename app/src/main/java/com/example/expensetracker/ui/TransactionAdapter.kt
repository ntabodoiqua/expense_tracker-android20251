package com.example.expensetracker.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
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

        // 1. Gán Text
        holder.binding.tvTitle.text = currentItem.title

        // 2. Format Ngày giờ
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) // Bỏ giờ phút cho gọn nếu muốn
        holder.binding.tvDate.text = sdf.format(Date(currentItem.date))
        
        // Hiển thị ghi chú (nếu có)
        if (currentItem.note.isNotEmpty()) {
            holder.binding.tvNote.text = currentItem.note
            holder.binding.tvNote.visibility = android.view.View.VISIBLE
        } else {
            holder.binding.tvNote.visibility = android.view.View.GONE
        }

        // 3. Format Tiền tệ
        val formatter = DecimalFormat("#,### ₫")

        if (currentItem.type == 1) { // Thu nhập
            holder.binding.tvAmount.setTextColor(Color.parseColor("#22c55e")) // Xanh lá
            holder.binding.tvAmount.text = "+ ${formatter.format(currentItem.amount)}"
        } else { // Chi tiêu
            holder.binding.tvAmount.setTextColor(Color.parseColor("#ef4444")) // Đỏ
            holder.binding.tvAmount.text = "- ${formatter.format(currentItem.amount)}"
        }

        // 4. Xử lý Click
        holder.itemView.setOnClickListener {
            onItemClick?.invoke(currentItem)
        }

        // ============================================================
        // 5. XỬ LÝ MÀU SẮC ICON THEO CATEGORY (PHẦN MỚI)
        // ============================================================

        when (currentItem.category) {
            "Ăn uống" -> {
                holder.binding.imgIcon.setImageResource(R.drawable.utensils)
                // Nền Cam Nhạt
                holder.binding.cardIcon.setCardBackgroundColor(Color.parseColor("#fed7aa"))
                // Icon Cam Đậm
                holder.binding.imgIcon.setColorFilter(Color.parseColor("#ea580c"))
            }
            "Đi lại" -> {
                holder.binding.imgIcon.setImageResource(R.drawable.car)
                // Nền Xanh Dương Nhạt
                holder.binding.cardIcon.setCardBackgroundColor(Color.parseColor("#bae6fd"))
                // Icon Xanh Dương Đậm
                holder.binding.imgIcon.setColorFilter(Color.parseColor("#2563eb"))
            }
            "Mua sắm" -> {
                holder.binding.imgIcon.setImageResource(R.drawable.shopping_bag)
                // Nền Tím Nhạt
                holder.binding.cardIcon.setCardBackgroundColor(Color.parseColor("#e9d5ff"))
                // Icon Tím Đậm
                holder.binding.imgIcon.setColorFilter(Color.parseColor("#9333ea"))
            }
            "Giải trí" -> {
                // Bạn có thể tìm thêm icon ic_gamepad hoặc ic_film
                holder.binding.imgIcon.setImageResource(R.drawable.shopping_bag)
                // Nền Vàng Nhạt
                holder.binding.cardIcon.setCardBackgroundColor(Color.parseColor("#fef08a"))
                // Icon Vàng Đậm
                holder.binding.imgIcon.setColorFilter(Color.parseColor("#ca8a04"))
            }
            "Lương", "Thưởng" -> {
                // Ưu tiên dùng icon tiền
                if (currentItem.category == "Lương") holder.binding.imgIcon.setImageResource(R.drawable.dollar_sign)
                else holder.binding.imgIcon.setImageResource(R.drawable.hand_coins)

                // Nền Xanh Lá Nhạt
                holder.binding.cardIcon.setCardBackgroundColor(Color.parseColor("#bbf7d0"))
                // Icon Xanh Lá Đậm
                holder.binding.imgIcon.setColorFilter(Color.parseColor("#16a34a"))
            }
            else -> {
                // Mặc định: Màu xám
                holder.binding.imgIcon.setImageResource(R.drawable.circle)
                holder.binding.cardIcon.setCardBackgroundColor(Color.parseColor("#e5e7eb"))
                holder.binding.imgIcon.setColorFilter(Color.parseColor("#4b5563"))
            }
        }
    }

    override fun getItemCount() = transactions.size

    fun setData(newTransactions: List<Transaction>) {
        this.transactions = newTransactions
        notifyDataSetChanged()
    }

    fun getTransactionAt(position: Int): Transaction {
        return transactions[position]
    }
}