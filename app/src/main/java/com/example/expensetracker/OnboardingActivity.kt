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

    // Launcher để chọn ảnh từ gallery
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedAvatarUri = it
            // Hiển thị ảnh đã chọn
            Glide.with(this)
                .load(it)
                .circleCrop()
                .into(binding.imgAvatar)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userPreferences = UserPreferences(this)

        // Kiểm tra nếu không phải lần đầu -> chuyển thẳng vào MainActivity
        if (!userPreferences.isFirstTime) {
            navigateToMain()
            return
        }

        setupViews()
    }

    private fun setupViews() {
        // Load avatar mặc định
        Glide.with(this)
            .load(R.drawable.avatar)
            .circleCrop()
            .into(binding.imgAvatar)

        // Click vào avatar hoặc nút camera để đổi ảnh
        binding.imgAvatar.setOnClickListener { pickImage() }
        binding.fabChangeAvatar.setOnClickListener { pickImage() }

        // Nút Bắt đầu
        binding.btnStart.setOnClickListener {
            saveUserInfo(skipMode = false)
        }

        // Nút Bỏ qua
        binding.tvSkip.setOnClickListener {
            saveUserInfo(skipMode = true)
        }
    }

    private fun pickImage() {
        pickImageLauncher.launch("image/*")
    }

    private fun saveUserInfo(skipMode: Boolean) {
        val name = binding.etName.text.toString().trim()

        // Validate tên nếu không phải skip mode
        if (!skipMode && name.isEmpty()) {
            binding.etName.error = "Vui lòng nhập tên của bạn"
            binding.etName.requestFocus()
            return
        } else {
            binding.etName.error = null
        }

        // Lưu thông tin
        userPreferences.userName = if (name.isNotEmpty()) name else "Username"
        
        // Lưu avatar URI nếu có
        selectedAvatarUri?.let {
            // Cần cấp quyền persistent để giữ URI sau khi restart app
            try {
                contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                userPreferences.userAvatar = it.toString()
            } catch (e: Exception) {
                // Nếu không được thì dùng avatar mặc định
                userPreferences.userAvatar = ""
            }
        }

        // Đánh dấu đã hoàn thành onboarding
        userPreferences.isFirstTime = false

        // Hiển thị thông báo
        val message = if (skipMode) "Bạn có thể thiết lập sau trong Cài đặt" else "Chào mừng ${userPreferences.userName}!"
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

        navigateToMain()
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
