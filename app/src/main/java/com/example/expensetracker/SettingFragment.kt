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

    // Khởi tạo giao diện từ XML binding
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Thiết lập ViewModel và tải dữ liệu người dùng
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        userPreferences = UserPreferences(requireContext())
        viewModel = ViewModelProvider(this)[TransactionViewModel::class.java]
        loadUserInfo()
        loadLimitSettings()
        setupListeners()
    }

    // Hiển thị thông tin người dùng và avatar
    private fun loadUserInfo() {
        binding.tvUserName.text = userPreferences.userName
        val avatarUri = userPreferences.userAvatar
        if (avatarUri.isNotEmpty()) {
            try {
                Glide.with(this).load(Uri.parse(avatarUri)).circleCrop()
                    .placeholder(R.drawable.avatar).error(R.drawable.avatar).into(binding.imgAvatar)
            } catch (e: Exception) {
                loadDefaultAvatar()
            }
        } else {
            loadDefaultAvatar()
        }
    }

    // Tải avatar mặc định
    private fun loadDefaultAvatar() {
        Glide.with(this).load(R.drawable.avatar).circleCrop().into(binding.imgAvatar)
    }

    // Tải cài đặt giới hạn chi tiêu
    private fun loadLimitSettings() {
        val isDailyEnabled = userPreferences.isDailyLimitEnabled
        val isMonthlyEnabled = userPreferences.isMonthlyLimitEnabled
        binding.switchDailyLimit.isChecked = isDailyEnabled
        binding.switchMonthlyLimit.isChecked = isMonthlyEnabled
        binding.etDailyLimit.visibility = if (isDailyEnabled) View.VISIBLE else View.GONE
        binding.etMonthlyLimit.visibility = if (isMonthlyEnabled) View.VISIBLE else View.GONE
        if (userPreferences.dailyLimit > 0) {
            binding.etDailyLimit.setText(formatAmount(userPreferences.dailyLimit))
        }
        if (userPreferences.monthlyLimit > 0) {
            binding.etMonthlyLimit.setText(formatAmount(userPreferences.monthlyLimit))
        }
    }

    // Định dạng số tiền để hiển thị
    private fun formatAmount(amount: Double): String {
        return if (amount % 1.0 == 0.0) {
            amount.toLong().toString()
        } else {
            amount.toString()
        }
    }

    // Thiết lập các sự kiện click và switch
    private fun setupListeners() {
        binding.switchDailyLimit.setOnCheckedChangeListener { _, isChecked ->
            binding.etDailyLimit.visibility = if (isChecked) View.VISIBLE else View.GONE
            userPreferences.isDailyLimitEnabled = isChecked
            if (!isChecked) {
                userPreferences.dailyLimit = 0.0
                binding.etDailyLimit.text?.clear()
            }
        }
        binding.switchMonthlyLimit.setOnCheckedChangeListener { _, isChecked ->
            binding.etMonthlyLimit.visibility = if (isChecked) View.VISIBLE else View.GONE
            userPreferences.isMonthlyLimitEnabled = isChecked
            if (!isChecked) {
                userPreferences.monthlyLimit = 0.0
                binding.etMonthlyLimit.text?.clear()
            }
        }
        binding.btnSaveLimits.setOnClickListener { saveLimits() }
        binding.btnResetData.setOnClickListener { showResetConfirmDialog() }
        binding.btnEditProfile.setOnClickListener {
            Toast.makeText(context, "Tính năng đang phát triển", Toast.LENGTH_SHORT).show()
        }
    }

    // Lưu giới hạn chi tiêu sau khi validate
    private fun saveLimits() {
        var isValid = true
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

    // Ẩn bàn phím
    private fun hideKeyboard() {
        val currentFocus = activity?.currentFocus
        if (currentFocus != null) {
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus.windowToken, 0)
        }
    }

    // Hiển thị dialog xác nhận xóa dữ liệu
    private fun showResetConfirmDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("⚠️ Xác nhận xóa dữ liệu")
            .setMessage("Bạn có chắc chắn muốn xóa tất cả dữ liệu không? Hành động này không thể hoàn tác!")
            .setPositiveButton("Xóa") { _, _ ->
                viewModel.deleteAllTransactions()
                userPreferences.clearAll()
                Toast.makeText(context, "Đã xóa tất cả dữ liệu!", Toast.LENGTH_SHORT).show()
                val intent = Intent(requireContext(), OnboardingActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                requireActivity().finish()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    // Giải phóng binding khi view bị hủy
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
