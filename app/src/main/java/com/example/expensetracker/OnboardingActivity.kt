package com.example.expensetracker

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.expensetracker.data.UserPreferences
import com.example.expensetracker.databinding.ActivityOnboardingBinding

class OnboardingActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var userPreferences: UserPreferences
    private var selectedAvatarUri: Uri? = null
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedAvatarUri = it
            Glide.with(this).load(it).circleCrop().into(binding.imgAvatar)
        }
    }

    // Khởi tạo Activity và kiểm tra lần đầu mở app
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        userPreferences = UserPreferences(this)
        if (!userPreferences.isFirstTime) {
            navigateToMain()
            return
        }
        setupViews()
    }

    // Thiết lập các thành phần UI và sự kiện
    private fun setupViews() {
        Glide.with(this).load(R.drawable.avatar).circleCrop().into(binding.imgAvatar)
        binding.imgAvatar.setOnClickListener { pickImage() }
        binding.fabChangeAvatar.setOnClickListener { pickImage() }
        binding.btnStart.setOnClickListener { saveUserInfo(skipMode = false) }
        binding.tvSkip.setOnClickListener { saveUserInfo(skipMode = true) }
    }

    // Mở gallery để chọn ảnh
    private fun pickImage() {
        pickImageLauncher.launch("image/*")
    }

    // Lưu thông tin người dùng và chuyển sang màn hình chính
    private fun saveUserInfo(skipMode: Boolean) {
        val name = binding.etName.text.toString().trim()
        if (!skipMode && name.isEmpty()) {
            binding.etName.error = "Vui lòng nhập tên của bạn"
            binding.etName.requestFocus()
            return
        } else {
            binding.etName.error = null
        }
        userPreferences.userName = if (name.isNotEmpty()) name else "Username"
        selectedAvatarUri?.let {
            try {
                contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                userPreferences.userAvatar = it.toString()
            } catch (e: Exception) {
                userPreferences.userAvatar = ""
            }
        }
        userPreferences.isFirstTime = false
        val message = if (skipMode) "Bạn có thể thiết lập sau trong Cài đặt" else "Chào mừng ${userPreferences.userName}!"
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        navigateToMain()
    }

    // Chuyển sang MainActivity
    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
