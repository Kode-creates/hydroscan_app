package com.adamson.fhydroscan

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView

class SettingE : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting_e)

        // Set up logout button
        val logoutButton = findViewById<LinearLayout>(R.id.logoutButton)
        logoutButton.setOnClickListener {
            // Redirect to login page
            val intent = Intent(this, Login::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        val bottomNave = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNave.selectedItemId = R.id.btsetting2 // Set settings as selected

        bottomNave.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.bthomee -> {
                    startActivity(Intent(this, DashboardE::class.java))
                    true
                }
                R.id.btscan2 -> {
                    startActivity(Intent(this, ScanE::class.java))
                    true
                }
                R.id.btsetting2 -> {
                    // Already on settings, no need to do anything
                    true
                }
                else -> false
            }
        }
    }
}
