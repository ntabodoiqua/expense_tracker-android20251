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
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        holder.binding.tvDate.text = sdf.format(Date(currentItem.date))


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
                holder.binding.cardIcon.setCardBackgroundColor(Color.parseColor("#ffedd5"))
                holder.binding.imgIcon.setColorFilter(Color.parseColor("#f97316"))
            }
            "Đi lại" -> {
                holder.binding.imgIcon.setImageResource(R.drawable.car)
                holder.binding.cardIcon.setCardBackgroundColor(Color.parseColor("#dbeafe"))
                holder.binding.imgIcon.setColorFilter(Color.parseColor("#3b82f6"))
            }
            "Mua sắm" -> {
                holder.binding.imgIcon.setImageResource(R.drawable.shopping_bag)
                holder.binding.cardIcon.setCardBackgroundColor(Color.parseColor("#fce7f3"))
                holder.binding.imgIcon.setColorFilter(Color.parseColor("#ec4899"))
            }
            "Giải trí" -> {
                holder.binding.imgIcon.setImageResource(R.drawable.monitor_play)
                holder.binding.cardIcon.setCardBackgroundColor(Color.parseColor("#f3e8ff"))
                holder.binding.imgIcon.setColorFilter(Color.parseColor("#a855f7"))
            }
            "Tiền nhà" -> {
                holder.binding.imgIcon.setImageResource(R.drawable.house)
                holder.binding.cardIcon.setCardBackgroundColor(Color.parseColor("#e0e7ff"))
                holder.binding.imgIcon.setColorFilter(Color.parseColor("#6366f1"))
            }
            "Hóa đơn" -> {
                holder.binding.imgIcon.setImageResource(R.drawable.receipt_text)
                holder.binding.cardIcon.setCardBackgroundColor(Color.parseColor("#cffafe"))
                holder.binding.imgIcon.setColorFilter(Color.parseColor("#06b6d4"))
            }
            "Y tế" -> {
                holder.binding.imgIcon.setImageResource(R.drawable.briefcase_medical)
                holder.binding.cardIcon.setCardBackgroundColor(Color.parseColor("#fee2e2"))
                holder.binding.imgIcon.setColorFilter(Color.parseColor("#ef4444"))
            }
            "Giáo dục" -> {
                holder.binding.imgIcon.setImageResource(R.drawable.book_open)
                holder.binding.cardIcon.setCardBackgroundColor(Color.parseColor("#f5f5f4"))
                holder.binding.imgIcon.setColorFilter(Color.parseColor("#78716c"))
            }
            "Lương" -> {
                holder.binding.imgIcon.setImageResource(R.drawable.dollar_sign)
                holder.binding.cardIcon.setCardBackgroundColor(Color.parseColor("#dcfce7"))
                holder.binding.imgIcon.setColorFilter(Color.parseColor("#22c55e"))
            }
            "Thưởng" -> {
                holder.binding.imgIcon.setImageResource(R.drawable.hand_coins)
                holder.binding.cardIcon.setCardBackgroundColor(Color.parseColor("#fef9c3"))
                holder.binding.imgIcon.setColorFilter(Color.parseColor("#eab308"))
            }
            "Lãi tiết kiệm" -> {
                holder.binding.imgIcon.setImageResource(R.drawable.piggy_bank)
                holder.binding.cardIcon.setCardBackgroundColor(Color.parseColor("#ecfccb"))
                holder.binding.imgIcon.setColorFilter(Color.parseColor("#84cc16"))
            }
            "Bán hàng" -> {
                holder.binding.imgIcon.setImageResource(R.drawable.store)
                holder.binding.cardIcon.setCardBackgroundColor(Color.parseColor("#e0f2fe"))
                holder.binding.imgIcon.setColorFilter(Color.parseColor("#0ea5e9"))
            }
            "Quà tặng" -> {
                holder.binding.imgIcon.setImageResource(R.drawable.gift)
                holder.binding.cardIcon.setCardBackgroundColor(Color.parseColor("#fef3c7"))
                holder.binding.imgIcon.setColorFilter(Color.parseColor("#f59e0b"))
            }
            else -> {
                holder.binding.imgIcon.setImageResource(R.drawable.square_dashed)
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