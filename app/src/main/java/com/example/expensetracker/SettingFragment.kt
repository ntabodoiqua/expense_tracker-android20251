package com.example.expensetracker

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.expensetracker.data.UserPreferences
import com.example.expensetracker.databinding.FragmentSettingBinding
import com.example.expensetracker.viewmodel.TransactionViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SettingFragment : Fragment() {

    private var _binding: FragmentSettingBinding? = null
    private val binding get() = _binding!!
    private lateinit var userPreferences: UserPreferences
    private lateinit var viewModel: TransactionViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userPreferences = UserPreferences(requireContext())
        viewModel = ViewModelProvider(this)[TransactionViewModel::class.java]

        loadUserInfo()
        loadLimitSettings()
        setupListeners()
    }

    private fun loadUserInfo() {
        // Hiển thị tên
        binding.tvUserName.text = userPreferences.userName

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

    private fun loadLimitSettings() {
        // Load trạng thái switch
        val isDailyEnabled = userPreferences.isDailyLimitEnabled
        val isMonthlyEnabled = userPreferences.isMonthlyLimitEnabled

        binding.switchDailyLimit.isChecked = isDailyEnabled
        binding.switchMonthlyLimit.isChecked = isMonthlyEnabled

        // Hiện/ẩn input fields dựa trên trạng thái (SỬA LẠI THEO XML)
        binding.etDailyLimit.visibility = if (isDailyEnabled) View.VISIBLE else View.GONE
        binding.etMonthlyLimit.visibility = if (isMonthlyEnabled) View.VISIBLE else View.GONE

        // Load giá trị giới hạn (Chuyển về chuỗi số nguyên cho đẹp nếu không có lẻ)
        if (userPreferences.dailyLimit > 0) {
            binding.etDailyLimit.setText(formatAmount(userPreferences.dailyLimit))
        }
        if (userPreferences.monthlyLimit > 0) {
            binding.etMonthlyLimit.setText(formatAmount(userPreferences.monthlyLimit))
        }
    }

    private fun formatAmount(amount: Double): String {
        return if (amount % 1.0 == 0.0) {
            amount.toLong().toString()
        } else {
            amount.toString()
        }
    }

    private fun setupListeners() {
        // 1. Switch Daily Limit
        binding.switchDailyLimit.setOnCheckedChangeListener { _, isChecked ->
            binding.etDailyLimit.visibility = if (isChecked) View.VISIBLE else View.GONE
            userPreferences.isDailyLimitEnabled = isChecked

            // Nếu tắt thì xóa dữ liệu cũ đi cho sạch
            if (!isChecked) {
                userPreferences.dailyLimit = 0.0
                binding.etDailyLimit.text?.clear()
            }
        }

        // 2. Switch Monthly Limit
        binding.switchMonthlyLimit.setOnCheckedChangeListener { _, isChecked ->
            binding.etMonthlyLimit.visibility = if (isChecked) View.VISIBLE else View.GONE
            userPreferences.isMonthlyLimitEnabled = isChecked

            if (!isChecked) {
                userPreferences.monthlyLimit = 0.0
                binding.etMonthlyLimit.text?.clear()
            }
        }

        // 3. Save Limits Button
        binding.btnSaveLimits.setOnClickListener {
            saveLimits()
        }

        // 4. Reset Data Button
        binding.btnResetData.setOnClickListener {
            showResetConfirmDialog()
        }

        // 5. Click vào phần Profile để chỉnh sửa
        binding.btnEditProfile.setOnClickListener {
            Toast.makeText(context, "Tính năng đang phát triển", Toast.LENGTH_SHORT).show()
        }

    }

    private fun saveLimits() {
        var isValid = true

        // Validate daily limit
        if (binding.switchDailyLimit.isChecked) {
            val dailyLimitStr = binding.etDailyLimit.text.toString().trim()
            if (dailyLimitStr.isEmpty()) {
                binding.etDailyLimit.error = "Vui lòng nhập số tiền"
                binding.etDailyLimit.requestFocus()
                isValid = false
            } else {
                val dailyLimit = dailyLimitStr.toDoubleOrNull()
                if (dailyLimit == null || dailyLimit <= 0) {
                    binding.etDailyLimit.error = "Số tiền không hợp lệ"
                    isValid = false
                } else {
                    binding.etDailyLimit.error = null
                    userPreferences.dailyLimit = dailyLimit
                }
            }
        }

        // Validate monthly limit (Chỉ check tiếp nếu cái trên đúng hoặc switch tắt)
        if (isValid && binding.switchMonthlyLimit.isChecked) {
            val monthlyLimitStr = binding.etMonthlyLimit.text.toString().trim()
            if (monthlyLimitStr.isEmpty()) {
                binding.etMonthlyLimit.error = "Vui lòng nhập số tiền"
                binding.etMonthlyLimit.requestFocus()
                isValid = false
            } else {
                val monthlyLimit = monthlyLimitStr.toDoubleOrNull()
                if (monthlyLimit == null || monthlyLimit <= 0) {
                    binding.etMonthlyLimit.error = "Số tiền không hợp lệ"
                    isValid = false
                } else {
                    binding.etMonthlyLimit.error = null
                    userPreferences.monthlyLimit = monthlyLimit
                }
            }
        }

        if (isValid) {
            hideKeyboard()
            binding.etDailyLimit.clearFocus()
            binding.etMonthlyLimit.clearFocus()
            Toast.makeText(context, "Đã lưu giới hạn!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun hideKeyboard() {
        val currentFocus = activity?.currentFocus
        if (currentFocus != null) {
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus.windowToken, 0)
        }
    }

    private fun showResetConfirmDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("⚠️ Xác nhận xóa dữ liệu")
            .setMessage("Bạn có chắc chắn muốn xóa tất cả dữ liệu không? Hành động này không thể hoàn tác!")
            .setPositiveButton("Xóa") { _, _ ->
                // 1. Xóa tất cả giao dịch trong database Room
                viewModel.deleteAllTransactions()

                // 2. Xóa preferences (thông tin user, limit...)
                userPreferences.clearAll()

                Toast.makeText(context, "Đã xóa tất cả dữ liệu!", Toast.LENGTH_SHORT).show()

                // 3. Reset app về màn hình chào mừng
                val intent = Intent(requireContext(), OnboardingActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                requireActivity().finish()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}