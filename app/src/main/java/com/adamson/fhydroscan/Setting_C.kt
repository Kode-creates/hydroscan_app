package com.adamson.fhydroscan

import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.adamson.fhydroscan.database.DatabaseHelper
import com.adamson.fhydroscan.database.HydroCoinDatabaseHelper
import com.google.android.material.bottomnavigation.BottomNavigationView

class Setting_C : AppCompatActivity() {
    private val TAG = "SettingCActivity"
    private lateinit var userDatabaseHelper: DatabaseHelper
    private lateinit var hydroCoinDatabaseHelper: HydroCoinDatabaseHelper
    private lateinit var customerText: TextView
    private lateinit var hydroCoinBalanceText: TextView
    private var currentUserId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_setting_c)
            
            // Initialize database helpers
            userDatabaseHelper = DatabaseHelper(this)
            hydroCoinDatabaseHelper = HydroCoinDatabaseHelper(this)
            
            // Get current user
            getCurrentUserId()
            
            // Initialize views
            initializeViews()
            
            setupViews()
            setupBottomNavigation()
            loadUserData()
            
            Log.d(TAG, "Setting_C activity created successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}", e)
            showToast("Error initializing the settings page")
            finish()
        }
    }

    private fun initializeViews() {
        customerText = findViewById(R.id.customerText)
        hydroCoinBalanceText = findViewById(R.id.hydroCoinBalanceText)
    }

    private fun getCurrentUserId() {
        // Get current user ID from SharedPreferences
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        currentUserId = sharedPref.getString("current_user_id", "") ?: ""
        
        if (currentUserId.isEmpty()) {
            // If no user ID found, try to get from intent
            currentUserId = intent.getStringExtra("user_id") ?: ""
        }
    }

    private fun loadUserData() {
        try {
            if (currentUserId.isNotEmpty()) {
                // Get user information
                val user = userDatabaseHelper.getUserById(currentUserId)
                if (user != null) {
                    // Display user name instead of "Customer"
                    val displayName = user.fullName ?: "Customer"
                    customerText.text = displayName
                } else {
                    customerText.text = "Customer"
                }
                
                // Get hydrocoin balance
                val hydroCoinBalance = hydroCoinDatabaseHelper.getHydroCoinBalance(currentUserId)
                hydroCoinBalanceText.text = hydroCoinBalance.toString()
            } else {
                customerText.text = "Customer"
                hydroCoinBalanceText.text = "0"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading user data: ${e.message}", e)
            customerText.text = "Customer"
            hydroCoinBalanceText.text = "0"
        }
    }

    private fun setupViews() {
        try {
            // Setup edit profile button
            findViewById<LinearLayout>(R.id.editProfileButton)?.setOnClickListener {
                showEditProfileDialog()
            }
            
            // Setup logout button
            findViewById<LinearLayout>(R.id.logoutButton)?.setOnClickListener {
                showLogoutConfirmationDialog()
            }
            
            Log.d(TAG, "Views setup completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up views: ${e.message}", e)
            showToast("Error setting up the settings page")
        }
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes, Logout") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(true)
            .show()
    }

    private fun showEditProfileDialog() {
        try {
            val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_profile, null)
            
            // Get current user data
            val user = userDatabaseHelper.getUserById(currentUserId)
            if (user == null) {
                showToast("User not found")
                return
            }
            
            // Initialize edit fields
            val phoneNumberEdit = dialogView.findViewById<EditText>(R.id.editPhoneNumber)
            val fullNameEdit = dialogView.findViewById<EditText>(R.id.editFullName)
            val addressEdit = dialogView.findViewById<EditText>(R.id.editAddress)
            val cancelButton = dialogView.findViewById<Button>(R.id.cancelEditButton)
            val saveButton = dialogView.findViewById<Button>(R.id.saveEditButton)
            
            // Populate fields with current data
            phoneNumberEdit.setText(user.phoneNumber ?: "")
            fullNameEdit.setText(user.fullName ?: "")
            addressEdit.setText(user.address ?: "")
            
            val dialog = AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create()
            
            cancelButton.setOnClickListener {
                dialog.dismiss()
            }
            
            saveButton.setOnClickListener {
                val newPhoneNumber = phoneNumberEdit.text.toString().trim()
                val newFullName = fullNameEdit.text.toString().trim()
                val newAddress = addressEdit.text.toString().trim()
                
                // Validate input
                when {
                    newPhoneNumber.isEmpty() || newFullName.isEmpty() || newAddress.isEmpty() -> {
                        showToast("Please fill in all fields")
                    }
                    !isValidPhoneNumber(newPhoneNumber) -> {
                        showToast("Please enter a valid phone number")
                    }
                    userDatabaseHelper.isPhoneNumberTaken(newPhoneNumber, currentUserId) -> {
                        showToast("Phone number is already taken by another user")
                    }
                    else -> {
                        // Check if name or address changed (requires QR generation)
                        val nameChanged = newFullName != (user.fullName ?: "")
                        val addressChanged = newAddress != (user.address ?: "")
                        
                        if (nameChanged || addressChanged) {
                            showQRGenerationWarningDialog(dialog, newPhoneNumber, newFullName, newAddress)
                        } else {
                            // Only phone number changed, no QR generation needed
                            updateUserProfile(newPhoneNumber, newFullName, newAddress)
                            dialog.dismiss()
                        }
                    }
                }
            }
            
            dialog.show()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error showing edit profile dialog: ${e.message}", e)
            showToast("Error loading edit profile dialog")
        }
    }

    private fun showQRGenerationWarningDialog(editDialog: AlertDialog, phoneNumber: String, fullName: String, address: String) {
        try {
            val warningView = LayoutInflater.from(this).inflate(R.layout.dialog_qr_generation_warning, null)
            
            val cancelButton = warningView.findViewById<Button>(R.id.cancelQrButton)
            val confirmButton = warningView.findViewById<Button>(R.id.confirmQrButton)
            
            val warningDialog = AlertDialog.Builder(this)
                .setView(warningView)
                .setCancelable(true)
                .create()
            
            cancelButton.setOnClickListener {
                warningDialog.dismiss()
            }
            
            confirmButton.setOnClickListener {
                updateUserProfile(phoneNumber, fullName, address)
                warningDialog.dismiss()
                editDialog.dismiss()
            }
            
            warningDialog.show()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error showing QR generation warning: ${e.message}", e)
            showToast("Error showing warning dialog")
        }
    }

    private fun updateUserProfile(phoneNumber: String, fullName: String, address: String) {
        try {
            if (userDatabaseHelper.updateUserProfile(currentUserId, phoneNumber, fullName, address)) {
                showToast("Profile updated successfully!")
                loadUserData() // Refresh the displayed data
            } else {
                showToast("Failed to update profile. Please try again.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating profile: ${e.message}", e)
            showToast("Error updating profile")
        }
    }

    private fun isValidPhoneNumber(phoneNumber: String): Boolean {
        val cleanNumber = phoneNumber.replace(Regex("[^0-9]"), "")
        return cleanNumber.length >= 10 && cleanNumber.length <= 15
    }

    private fun performLogout() {
        try {
            // Clear user session
            val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
            with(sharedPref.edit()) {
                remove("current_user_id")
                apply()
            }
            
            showToast("Logged out successfully")
            
            // Navigate to login page
            val intent = Intent(this, Login::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during logout: ${e.message}", e)
            showToast("Error during logout")
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

    override fun onResume() {
        super.onResume()
        // Refresh user data when activity resumes
        loadUserData()
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















