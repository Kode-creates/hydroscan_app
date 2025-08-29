package com.adamson.fhydroscan

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.app.DatePickerDialog
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.content.Intent
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CusOrder : AppCompatActivity() {
    private val dateFormat = SimpleDateFormat("MM-dd-yyyy", Locale.getDefault())
    private val TAG = "CusOrderActivity"
    
    // Scheduling variables
    private var pickupDate = ""
    private var deliveryDate = ""

    data class Order(
        val date: Date = Date(),
        val address: String,
        val waterType: String,
        val quantity: Int,
        val pickupDate: String = "",
        val deliveryDate: String = ""
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_cus_order)
            setupViews()
            setupBottomNavigation()
            Log.d(TAG, "CusOrder activity created successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}", e)
            showToast("Error initializing the order page")
            finish()
        }
    }

    private fun setupViews() {
        try {
            // Setup radio buttons (no setup needed, they're already configured in XML)

            val addressInput = findViewById<EditText>(R.id.addressInput) 
                ?: throw IllegalStateException("Address input not found")
            val quantityInput = findViewById<EditText>(R.id.quantityInput)
                ?: throw IllegalStateException("Quantity input not found")
            val submitButton = findViewById<Button>(R.id.submitButton)
                ?: throw IllegalStateException("Submit button not found")
            val cancelButton = findViewById<Button>(R.id.cancelButton)
                ?: throw IllegalStateException("Cancel button not found")
            
            // Setup scheduling
            setupScheduling()

            // Set up submit button
            submitButton.setOnClickListener {
                try {
                    val name = findViewById<EditText>(R.id.nameInput).text.toString().trim()
                    val address = addressInput.text.toString().trim()
                    val quantityStr = quantityInput.text.toString()
                    val quantity = quantityStr.toIntOrNull()
                    val waterType = when {
                        findViewById<RadioButton>(R.id.radioMineral).isChecked -> "M"
                        findViewById<RadioButton>(R.id.radioAlkaline).isChecked -> "A"
                        else -> "M" // Default to Mineral if somehow nothing is selected
                    }

                    Log.d(TAG, "Order attempt - Name: $name, Address: $address, Quantity: $quantity, Type: $waterType, Pickup: $pickupDate, Delivery: $deliveryDate")

                    when {
                        name.isBlank() -> {
                            Log.w(TAG, "Order validation failed: Empty name")
                            showToast("Please enter your name")
                        }
                        address.isBlank() -> {
                            Log.w(TAG, "Order validation failed: Empty address")
                            showToast("Please enter delivery address")
                        }
                        quantity == null || quantity <= 0 -> {
                            Log.w(TAG, "Order validation failed: Invalid quantity: $quantityStr")
                            showToast("Please enter a valid quantity")
                        }
                        pickupDate.isEmpty() && findViewById<Switch>(R.id.advanceOrderToggle).isChecked -> {
                            Log.w(TAG, "Order validation failed: No pick-up date selected")
                            showToast("Please select a pick-up date")
                        }
                        deliveryDate.isEmpty() && findViewById<Switch>(R.id.advanceOrderToggle).isChecked -> {
                            Log.w(TAG, "Order validation failed: No delivery date selected")
                            showToast("Please select a delivery date")
                        }
                        else -> {
                            val order = Order(
                                address = address, 
                                waterType = waterType, 
                                quantity = quantity,
                                pickupDate = if (findViewById<Switch>(R.id.advanceOrderToggle).isChecked) pickupDate else "",
                                deliveryDate = if (findViewById<Switch>(R.id.advanceOrderToggle).isChecked) deliveryDate else ""
                            )
                            Log.d(TAG, "Order added successfully: $order")
                            showToast("Order submitted successfully!")
                            // Clear the form
                            findViewById<EditText>(R.id.nameInput).text.clear()
                            addressInput.text.clear()
                            quantityInput.text.clear()
                            findViewById<RadioButton>(R.id.radioMineral).isChecked = true // Reset to default
                            findViewById<Switch>(R.id.advanceOrderToggle).isChecked = false
                            pickupDate = ""
                            deliveryDate = ""
                            findViewById<Button>(R.id.pickupDateButton).text = "Select Date"
                            findViewById<Button>(R.id.deliveryDateButton).text = "Select Date"
                            setSchedulingEnabled(false)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing order: ${e.message}", e)
                    showToast("Error processing order")
                }
            }

            // Set up cancel button
            cancelButton.setOnClickListener { 
                Log.d(TAG, "Order cancelled")
                finish() // Go back to previous page
            }

            Log.d(TAG, "Views setup completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up views: ${e.message}", e)
            showToast("Error setting up the order form")
        }
    }

    private fun setupScheduling() {
        try {
            // Setup pick-up date button
            findViewById<Button>(R.id.pickupDateButton).setOnClickListener {
                showDatePicker(true)
            }

            // Setup delivery date button
            findViewById<Button>(R.id.deliveryDateButton).setOnClickListener {
                showDatePicker(false)
            }

            // Setup advance order toggle
            setupAdvanceOrderToggle()

            Log.d(TAG, "Scheduling setup completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up scheduling: ${e.message}", e)
        }
    }

    private fun setupAdvanceOrderToggle() {
        try {
            val toggle = findViewById<Switch>(R.id.advanceOrderToggle)
            val titlesContainer = findViewById<LinearLayout>(R.id.schedulingTitlesContainer)
            val calendarsContainer = findViewById<LinearLayout>(R.id.schedulingCalendarsContainer)
            val pickupDateButton = findViewById<Button>(R.id.pickupDateButton)
            val deliveryDateButton = findViewById<Button>(R.id.deliveryDateButton)

            // Initially disable scheduling sections
            setSchedulingEnabled(false)

            toggle.setOnCheckedChangeListener { _, isChecked ->
                setSchedulingEnabled(isChecked)
                Log.d(TAG, "Advance order toggle: ${if (isChecked) "ON" else "OFF"}")
            }

            Log.d(TAG, "Advance order toggle setup completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up advance order toggle: ${e.message}", e)
        }
    }

    private fun setSchedulingEnabled(enabled: Boolean) {
        try {
            val toggle = findViewById<Switch>(R.id.advanceOrderToggle)
            val titlesContainer = findViewById<LinearLayout>(R.id.schedulingTitlesContainer)
            val calendarsContainer = findViewById<LinearLayout>(R.id.schedulingCalendarsContainer)
            val pickupDateButton = findViewById<Button>(R.id.pickupDateButton)
            val deliveryDateButton = findViewById<Button>(R.id.deliveryDateButton)

            // Set alpha for visual feedback
            val alpha = if (enabled) 1.0f else 0.5f
            titlesContainer.alpha = alpha
            calendarsContainer.alpha = alpha

            // Enable/disable buttons
            pickupDateButton.isEnabled = enabled
            deliveryDateButton.isEnabled = enabled

            // Update button text colors
            val textColor = if (enabled) resources.getColor(android.R.color.white, null) else resources.getColor(android.R.color.darker_gray, null)
            pickupDateButton.setTextColor(textColor)
            deliveryDateButton.setTextColor(textColor)

            // Update button background colors
            val backgroundColor = if (enabled) resources.getColor(android.R.color.holo_blue_dark, null) else resources.getColor(android.R.color.darker_gray, null)
            pickupDateButton.setBackgroundColor(backgroundColor)
            deliveryDateButton.setBackgroundColor(backgroundColor)

            // Update switch colors
            if (enabled) {
                toggle.thumbTintList = resources.getColorStateList(android.R.color.holo_blue_dark, null)
                toggle.trackTintList = resources.getColorStateList(android.R.color.holo_blue_light, null)
            } else {
                toggle.thumbTintList = resources.getColorStateList(android.R.color.darker_gray, null)
                toggle.trackTintList = resources.getColorStateList(android.R.color.darker_gray, null)
            }

            Log.d(TAG, "Scheduling sections ${if (enabled) "enabled" else "disabled"}")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting scheduling enabled state: ${e.message}", e)
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
                
                // Check if selected day is Sunday (1) or Wednesday (4)
                if (selectedCalendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY || 
                    selectedCalendar.get(Calendar.DAY_OF_WEEK) == Calendar.WEDNESDAY) {
                    showToast("Sundays and Wednesdays are not available")
                    return@DatePickerDialog
                }
                
                // Check if selected date is in the past
                if (selectedCalendar.before(Calendar.getInstance())) {
                    showToast("Please select a future date")
                    return@DatePickerDialog
                }
                
                val dateString = String.format("%02d-%02d-%04d", month + 1, dayOfMonth, year)
                
                if (isPickup) {
                    pickupDate = dateString
                    findViewById<Button>(R.id.pickupDateButton).text = dateString
                    Log.d(TAG, "Pick-up date selected: $pickupDate")
                } else {
                    deliveryDate = dateString
                    findViewById<Button>(R.id.deliveryDateButton).text = dateString
                    Log.d(TAG, "Delivery date selected: $deliveryDate")
                }
            },
            currentYear, currentMonth, currentDay
        )
        
        // Set minimum date to tomorrow
        val tomorrow = Calendar.getInstance()
        tomorrow.add(Calendar.DAY_OF_MONTH, 1)
        datePickerDialog.datePicker.minDate = tomorrow.timeInMillis
        
        // Customize the date picker to disable Sundays and Wednesdays
        datePickerDialog.datePicker.setOnDateChangedListener { _, year, month, dayOfMonth ->
            val selectedCalendar = Calendar.getInstance()
            selectedCalendar.set(year, month, dayOfMonth)
            
            // If the selected date is Sunday or Wednesday, show a message
            if (selectedCalendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY || 
                selectedCalendar.get(Calendar.DAY_OF_WEEK) == Calendar.WEDNESDAY) {
                showToast("Sundays and Wednesdays are not available")
            }
        }
        
        datePickerDialog.show()
    }



    private fun showToast(message: String) {
        try {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            Log.d(TAG, "Toast shown: $message")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing toast: ${e.message}", e)
        }
    }

    private fun setupBottomNavigation() {
        try {
            val bottomNavc = findViewById<BottomNavigationView>(R.id.bottomNavc)
            bottomNavc.selectedItemId = R.id.bthomec // Set home as selected

            bottomNavc.setOnItemSelectedListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.bthomec -> {
                        startActivity(Intent(this, Customer::class.java))
                        true
                    }
                    R.id.btcart -> {
                        startActivity(Intent(this, Cart::class.java))
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
            Log.d(TAG, "Bottom navigation setup completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up bottom navigation: ${e.message}", e)
        }
    }
}
