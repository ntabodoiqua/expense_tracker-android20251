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
        binding.switchDailyLimit.isChecked = userPreferences.isDailyLimitEnabled
        binding.switchMonthlyLimit.isChecked = userPreferences.isMonthlyLimitEnabled

        // Hiện/ẩn input fields
        binding.layoutDailyLimit.visibility = if (userPreferences.isDailyLimitEnabled) View.VISIBLE else View.GONE
        binding.layoutMonthlyLimit.visibility = if (userPreferences.isMonthlyLimitEnabled) View.VISIBLE else View.GONE

        // Load giá trị giới hạn
        if (userPreferences.dailyLimit > 0) {
            binding.etDailyLimit.setText(userPreferences.dailyLimit.toLong().toString())
        }
        if (userPreferences.monthlyLimit > 0) {
            binding.etMonthlyLimit.setText(userPreferences.monthlyLimit.toLong().toString())
        }
    }

    private fun setupListeners() {
        // Switch Daily Limit
        binding.switchDailyLimit.setOnCheckedChangeListener { _, isChecked ->
            binding.layoutDailyLimit.visibility = if (isChecked) View.VISIBLE else View.GONE
            userPreferences.isDailyLimitEnabled = isChecked
            if (!isChecked) {
                userPreferences.dailyLimit = 0.0
                binding.etDailyLimit.text?.clear()
            }
        }

        // Switch Monthly Limit
        binding.switchMonthlyLimit.setOnCheckedChangeListener { _, isChecked ->
            binding.layoutMonthlyLimit.visibility = if (isChecked) View.VISIBLE else View.GONE
            userPreferences.isMonthlyLimitEnabled = isChecked
            if (!isChecked) {
                userPreferences.monthlyLimit = 0.0
                binding.etMonthlyLimit.text?.clear()
            }
        }

        // Save Limits Button
        binding.btnSaveLimits.setOnClickListener {
            saveLimits()
        }

        // Reset Data Button
        binding.btnResetData.setOnClickListener {
            showResetConfirmDialog()
        }

        // Edit Profile (click vào card profile)
        binding.cardProfile.setOnClickListener {
            // TODO: Mở màn hình chỉnh sửa profile
            Toast.makeText(context, "Tính năng đang phát triển", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveLimits() {
        var isValid = true

        // Validate daily limit
        if (binding.switchDailyLimit.isChecked) {
            val dailyLimitStr = binding.etDailyLimit.text.toString().trim()
            if (dailyLimitStr.isEmpty()) {
                binding.layoutDailyLimit.error = "Vui lòng nhập số tiền"
                isValid = false
            } else {
                val dailyLimit = dailyLimitStr.toDoubleOrNull()
                if (dailyLimit == null || dailyLimit <= 0) {
                    binding.layoutDailyLimit.error = "Số tiền không hợp lệ"
                    isValid = false
                } else {
                    binding.layoutDailyLimit.error = null
                    userPreferences.dailyLimit = dailyLimit
                }
            }
        }

        // Validate monthly limit
        if (binding.switchMonthlyLimit.isChecked) {
            val monthlyLimitStr = binding.etMonthlyLimit.text.toString().trim()
            if (monthlyLimitStr.isEmpty()) {
                binding.layoutMonthlyLimit.error = "Vui lòng nhập số tiền"
                isValid = false
            } else {
                val monthlyLimit = monthlyLimitStr.toDoubleOrNull()
                if (monthlyLimit == null || monthlyLimit <= 0) {
                    binding.layoutMonthlyLimit.error = "Số tiền không hợp lệ"
                    isValid = false
                } else {
                    binding.layoutMonthlyLimit.error = null
                    userPreferences.monthlyLimit = monthlyLimit
                }
            }
        }

        if (isValid) {
            // Ẩn bàn phím
            hideKeyboard()
            
            // Clear focus khỏi các EditText
            binding.etDailyLimit.clearFocus()
            binding.etMonthlyLimit.clearFocus()
            
            Toast.makeText(context, "✅ Đã lưu giới hạn chi tiêu!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    private fun showResetConfirmDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("⚠️ Xác nhận xóa dữ liệu")
            .setMessage("Bạn có chắc chắn muốn xóa tất cả dữ liệu? Hành động này không thể hoàn tác!")
            .setPositiveButton("Xóa") { _, _ ->
                // Xóa tất cả giao dịch trong database
                viewModel.deleteAllTransactions()
                
                // Xóa preferences
                userPreferences.clearAll()
                Toast.makeText(context, "Đã xóa tất cả dữ liệu!", Toast.LENGTH_SHORT).show()
                
                // Quay về màn hình onboarding
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