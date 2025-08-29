package com.adamson.fhydroscan

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.Calendar

class Cart : AppCompatActivity() {
    private val TAG = "CartActivity"

    private var pickupDate = ""
    private var deliveryDate = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_cart)
            setupBottomNavigation()
            setupScheduling()
            Log.d(TAG, "Cart activity created successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}", e)
            showToast("Error initializing the cart page")
            finish()
        }
    }

    private fun setupScheduling() {
        try {
            // Buttons
            findViewById<Button>(R.id.pickupDateButton).setOnClickListener { showDatePicker(true) }
            findViewById<Button>(R.id.deliveryDateButton).setOnClickListener { showDatePicker(false) }

            // Toggle
            val toggle = findViewById<Switch>(R.id.advanceOrderToggle)
            setSchedulingEnabled(false)
            toggle.setOnCheckedChangeListener { _, isChecked ->
                setSchedulingEnabled(isChecked)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up scheduling: ${e.message}", e)
        }
    }

    private fun setSchedulingEnabled(enabled: Boolean) {
        try {
            val titlesContainer = findViewById<LinearLayout>(R.id.schedulingTitlesContainer)
            val calendarsContainer = findViewById<LinearLayout>(R.id.schedulingCalendarsContainer)
            val pickupDateButton = findViewById<Button>(R.id.pickupDateButton)
            val deliveryDateButton = findViewById<Button>(R.id.deliveryDateButton)

            val alpha = if (enabled) 1.0f else 0.5f
            titlesContainer.alpha = alpha
            calendarsContainer.alpha = alpha

            pickupDateButton.isEnabled = enabled
            deliveryDateButton.isEnabled = enabled

            val textColor = if (enabled) resources.getColor(android.R.color.white, null) else resources.getColor(android.R.color.darker_gray, null)
            val backgroundColor = if (enabled) resources.getColor(android.R.color.holo_blue_dark, null) else resources.getColor(android.R.color.darker_gray, null)
            pickupDateButton.setTextColor(textColor)
            deliveryDateButton.setTextColor(textColor)
            pickupDateButton.setBackgroundColor(backgroundColor)
            deliveryDateButton.setBackgroundColor(backgroundColor)
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling scheduling enabled state: ${e.message}", e)
        }
    }

    private fun showDatePicker(isPickup: Boolean) {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(year, month, dayOfMonth)

                if (selectedCalendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY ||
                    selectedCalendar.get(Calendar.DAY_OF_WEEK) == Calendar.WEDNESDAY) {
                    showToast("Sundays and Wednesdays are not available")
                    return@DatePickerDialog
                }

                if (selectedCalendar.before(Calendar.getInstance())) {
                    showToast("Please select a future date")
                    return@DatePickerDialog
                }

                val dateString = String.format("%02d-%02d-%04d", month + 1, dayOfMonth, year)
                if (isPickup) {
                    pickupDate = dateString
                    findViewById<Button>(R.id.pickupDateButton).text = dateString
                } else {
                    deliveryDate = dateString
                    findViewById<Button>(R.id.deliveryDateButton).text = dateString
                }
            },
            currentYear, currentMonth, currentDay
        )

        val tomorrow = Calendar.getInstance()
        tomorrow.add(Calendar.DAY_OF_MONTH, 1)
        datePickerDialog.datePicker.minDate = tomorrow.timeInMillis

        datePickerDialog.datePicker.setOnDateChangedListener { _, year, month, dayOfMonth ->
            val selectedCalendar = Calendar.getInstance()
            selectedCalendar.set(year, month, dayOfMonth)
            if (selectedCalendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY ||
                selectedCalendar.get(Calendar.DAY_OF_WEEK) == Calendar.WEDNESDAY) {
                showToast("Sundays and Wednesdays are not available")
            }
        }

        datePickerDialog.show()
    }

    private fun setupBottomNavigation() {
        try {
            val bottomNavc = findViewById<BottomNavigationView>(R.id.bottomNavc)
            bottomNavc.selectedItemId = R.id.btcart

            bottomNavc.setOnItemSelectedListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.bthomec -> {
                        startActivity(Intent(this, Customer::class.java))
                        true
                    }
                    R.id.btcart -> {
                        true
                    }
                    R.id.bthis -> {
                        startActivity(Intent(this, OrderHistory::class.java))
                        true
                    }
                    R.id.btsettingc -> {
                        startActivity(Intent(this, Setting_C::class.java))
                        true
                    }
                    else -> false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up bottom navigation: ${e.message}", e)
        }
    }

    private fun showToast(message: String) {
        try {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error showing toast: ${e.message}", e)
        }
    }
}
