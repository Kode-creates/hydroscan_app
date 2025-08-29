package com.adamson.fhydroscan

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class Setting_C : AppCompatActivity() {
    private val TAG = "SettingCActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_setting_c)
            setupViews()
            setupBottomNavigation()
            Log.d(TAG, "Setting_C activity created successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}", e)
            showToast("Error initializing the settings page")
            finish()
        }
    }

    private fun setupViews() {
        try {
            // Setup logout button
            findViewById<Button>(R.id.logoutButton)?.setOnClickListener {
                Log.d(TAG, "Logout button clicked")
                showToast("Logout successful")
                // Handle logout logic here
                // You can navigate to login page or clear user session
            }
            
            Log.d(TAG, "Views setup completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up views: ${e.message}", e)
            showToast("Error setting up the settings page")
        }
    }

    private fun setupBottomNavigation() {
        try {
            val bottomNavc = findViewById<BottomNavigationView>(R.id.bottomNavc)
            bottomNavc.selectedItemId = R.id.btsettingc // Set profile as selected

            bottomNavc.setOnItemSelectedListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.bthomec -> {
                        startActivity(Intent(this, Customer::class.java))
                        true
                    }
                    R.id.btcart -> {
                        startActivity(Intent(this, ScanC::class.java))
                        true
                    }
                    R.id.bthis -> {
                        startActivity(Intent(this, OrderHistory::class.java))
                        true
                    }
                    R.id.btsettingc -> {
                        true // Already on settings page
                    }
                    else -> false
                }
            }
            Log.d(TAG, "Bottom navigation setup completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up bottom navigation: ${e.message}", e)
        }
    }

    private fun showToast(message: String) {
        try {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            Log.d(TAG, "Toast shown: $message")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing toast: ${e.message}", e)
        }
    }
}
