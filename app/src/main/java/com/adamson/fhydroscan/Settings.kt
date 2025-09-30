package com.adamson.fhydroscan

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView

class Settings : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Set up user management button
        val userManagementButton = findViewById<LinearLayout>(R.id.userManagementButton)
        userManagementButton.setOnClickListener {
            val intent = Intent(this, UserManagement::class.java)
            startActivity(intent)
        }

        // Set up products button
        val productsButton = findViewById<LinearLayout>(R.id.productsButton)
        productsButton.setOnClickListener {
            val intent = Intent(this, Products::class.java)
            startActivity(intent)
        }

        // Set up logout button
        val logoutButton = findViewById<LinearLayout>(R.id.logoutButton)
        logoutButton.setOnClickListener {
            // Redirect to login page
            val intent = Intent(this, Login::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.bthome -> {
                    startActivity(Intent(this, Dashboard::class.java))
                    true
                }
                R.id.btscan -> {
                    startActivity(Intent(this, Scan::class.java))
                    true
                }
                R.id.btsetting -> {
                    true
                }
                else -> false
            }
        }
    }
}