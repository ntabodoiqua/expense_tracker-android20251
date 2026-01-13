package com.example.expensetracker

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.expensetracker.databinding.ActivityMainBinding

class  MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadFragment(HomeFragment())

        // 2. Bắt sự kiện click vào menu dưới đáy
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    loadFragment(HomeFragment())
                    true
                }
                R.id.nav_search -> {
                    loadFragment(SearchFragment())
                    true
                }
                R.id.nav_chart -> {
                    loadFragment(ChartFragment())
                    true
                }
                R.id.nav_setting -> {
                    loadFragment(SettingFragment())
                    true
                }
                else -> false
            }
        }

    }

    // Hàm thay thế Fragment vào cái khung FrameLayout
    private fun loadFragment(fragment: androidx.fragment.app.Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, fragment)
        transaction.commit()
    }

}