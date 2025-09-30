package com.adamson.fhydroscan

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.adamson.fhydroscan.adapter.CartAdapter
import com.adamson.fhydroscan.data.CartItem
import com.adamson.fhydroscan.data.Order
import com.adamson.fhydroscan.data.OrderItem
import com.adamson.fhydroscan.data.OrderStatus
import com.adamson.fhydroscan.database.CartDatabaseHelper
import com.adamson.fhydroscan.database.HydroCoinDatabaseHelper
import com.adamson.fhydroscan.database.OrderDatabaseHelper
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.Calendar

class Cart : AppCompatActivity() {
    private val TAG = "CartActivity"

    private var pickupDate = ""
    private var deliveryDate = ""
    private lateinit var cartDatabaseHelper: CartDatabaseHelper
    private lateinit var orderDatabaseHelper: OrderDatabaseHelper
    private lateinit var hydroCoinDatabaseHelper: HydroCoinDatabaseHelper
    private lateinit var cartAdapter: CartAdapter
    private lateinit var cartRecyclerView: RecyclerView
    private lateinit var emptyCartText: TextView
    private var currentUserId: String = ""
    private var hydroCoinBalance: Int = 0
    private var isUsingHydroCoins: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_cart)
            
            // Initialize database and get current user
            cartDatabaseHelper = CartDatabaseHelper(this)
            orderDatabaseHelper = OrderDatabaseHelper(this)
            hydroCoinDatabaseHelper = HydroCoinDatabaseHelper(this)
            getCurrentUserId()
            
            // Setup UI components
            setupBottomNavigation()
            setupCheckoutButton()
            setupCartRecyclerView()
            loadCartItems()
            
            Log.d(TAG, "Cart activity created successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}", e)
            showToast("Error initializing the cart page")
            finish()
        }
    }

    private fun setupCheckoutButton() {
        try {
            findViewById<Button>(R.id.checkoutButton).setOnClickListener {
                showCheckoutDialog()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up checkout button: ${e.message}", e)
        }
    }

    private fun showCheckoutDialog() {
        try {
            val dialog = Dialog(this)
            dialog.setContentView(R.layout.dialog_checkout)
            dialog.window?.setLayout(
                (resources.displayMetrics.widthPixels * 0.9).toInt(),
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
            dialog.setCancelable(true)

            // Setup dialog components
            setupDialogComponents(dialog)

            dialog.show()
        } catch (e: Exception) {
            Log.e(TAG, "Error showing checkout dialog: ${e.message}", e)
            showToast("Error opening checkout")
        }
    }

    private fun setupDialogComponents(dialog: Dialog) {
        try {
            // Buttons
            dialog.findViewById<Button>(R.id.pickupDateButton).setOnClickListener { showDatePicker(true) }
            dialog.findViewById<Button>(R.id.deliveryDateButton).setOnClickListener { showDatePicker(false) }

            // Toggle
            val toggle = dialog.findViewById<Switch>(R.id.advanceOrderToggle)
            setSchedulingEnabled(dialog, false)
            toggle.setOnCheckedChangeListener { _, isChecked ->
                setSchedulingEnabled(dialog, isChecked)
            }

            // Submit Order Button
            dialog.findViewById<Button>(R.id.orderButton).setOnClickListener {
                showOrderSubmissionDialog(dialog)
                dialog.dismiss()
            }

            // Setup hydrocoin switch
            setupDialogHydroCoinSwitch(dialog)

            // Load current data
            loadDialogData(dialog)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up dialog components: ${e.message}", e)
        }
    }

    private fun setupDialogHydroCoinSwitch(dialog: Dialog) {
        val hydroCoinSwitch = dialog.findViewById<Switch>(R.id.useCoinsSwitch)
        hydroCoinSwitch.setOnCheckedChangeListener { _, isChecked ->
            isUsingHydroCoins = isChecked
            updateDialogTotalAmount(dialog)
        }
    }

    private fun loadDialogData(dialog: Dialog) {
        try {
            // Load hydrocoin balance
            hydroCoinBalance = hydroCoinDatabaseHelper.getHydroCoinBalance(currentUserId)
            dialog.findViewById<TextView>(R.id.hydroCoinBalanceText).text = "Balance: $hydroCoinBalance coins"

            // Calculate and display total amount
            updateDialogTotalAmount(dialog)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading dialog data: ${e.message}", e)
        }
    }

    private fun updateDialogTotalAmount(dialog: Dialog) {
        if (currentUserId.isNotEmpty()) {
            val cartItems = cartDatabaseHelper.getCartItems(currentUserId)
            val total = cartItems.sumOf { it.price * it.quantity }
            
            val totalAmountText = dialog.findViewById<TextView>(R.id.totalAmount)
            if (isUsingHydroCoins && hydroCoinBalance > 0) {
                val discountAmount = minOf(hydroCoinBalance.toDouble(), total)
                val finalTotal = total - discountAmount
                totalAmountText.text = "₱${String.format("%.2f", finalTotal)} (${hydroCoinBalance.toInt()} coins used)"
            } else {
                totalAmountText.text = "₱${String.format("%.2f", total)}"
            }
        }
    }

    private fun setSchedulingEnabled(dialog: Dialog, enabled: Boolean) {
        try {
            val titlesContainer = dialog.findViewById<LinearLayout>(R.id.schedulingTitlesContainer)
            val calendarsContainer = dialog.findViewById<LinearLayout>(R.id.schedulingCalendarsContainer)
            val pickupDateButton = dialog.findViewById<Button>(R.id.pickupDateButton)
            val deliveryDateButton = dialog.findViewById<Button>(R.id.deliveryDateButton)

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
                } else {
                    deliveryDate = dateString
                }
                
                // Update the dialog buttons if dialog is still showing
                updateDialogDateButtons()
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

    private fun updateDialogDateButtons() {
        // This method will be called to update any visible dialog buttons
        // For now, we'll store the dates and they'll be updated when dialog is shown again
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

    private fun getCurrentUserId() {
        // Get current user ID from SharedPreferences or intent
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        currentUserId = sharedPref.getString("current_user_id", "") ?: ""
        
        if (currentUserId.isEmpty()) {
            // If no user ID found, try to get from intent
            currentUserId = intent.getStringExtra("user_id") ?: ""
        }
    }

    private fun setupCartRecyclerView() {
        cartRecyclerView = findViewById(R.id.cartRecyclerView)
        emptyCartText = findViewById(R.id.emptyCartText)
        
        cartAdapter = CartAdapter(
            emptyList(),
            onQuantityChanged = { item, newQuantity ->
                updateCartItemQuantity(item, newQuantity)
            },
            onItemDeleted = { item ->
                deleteCartItem(item)
            }
        )
        
        cartRecyclerView.layoutManager = LinearLayoutManager(this)
        cartRecyclerView.adapter = cartAdapter
    }

    private fun loadCartItems() {
        if (currentUserId.isNotEmpty()) {
            val cartItems = cartDatabaseHelper.getCartItems(currentUserId)
            cartAdapter.updateCartItems(cartItems)
            
            // Show/hide empty cart message
            if (cartItems.isEmpty()) {
                emptyCartText.visibility = TextView.VISIBLE
                cartRecyclerView.visibility = RecyclerView.GONE
            } else {
                emptyCartText.visibility = TextView.GONE
                cartRecyclerView.visibility = RecyclerView.VISIBLE
            }
        } else {
            showToast("User not logged in")
            emptyCartText.visibility = TextView.VISIBLE
            cartRecyclerView.visibility = RecyclerView.GONE
        }
    }

    private fun updateCartItemQuantity(item: CartItem, newQuantity: Int) {
        if (cartDatabaseHelper.updateQuantity(item.id, newQuantity)) {
            loadCartItems() // Refresh the cart
        } else {
            showToast("Failed to update quantity")
        }
    }

    private fun deleteCartItem(item: CartItem) {
        if (cartDatabaseHelper.removeFromCart(item.id)) {
            showToast("Item removed from cart")
            loadCartItems() // Refresh the cart
        } else {
            showToast("Failed to remove item")
        }
    }

    private fun showOrderSubmissionDialog(dialog: Dialog? = null) {
        try {
            if (currentUserId.isEmpty()) {
                showToast("User not logged in")
                return
            }
            
            val cartItems = cartDatabaseHelper.getCartItems(currentUserId)
            if (cartItems.isEmpty()) {
                showToast("Cart is empty")
                return
            }
            
            // Create order
            val orderNumber = orderDatabaseHelper.generateOrderNumber()
            val totalAmount = cartItems.sumOf { it.price * it.quantity }
            val totalItems = cartItems.sumOf { it.quantity }
            
            // Calculate final amount after hydrocoin discount
            val finalAmount = if (isUsingHydroCoins && hydroCoinBalance > 0) {
                val discountAmount = minOf(hydroCoinBalance.toDouble(), totalAmount)
                totalAmount - discountAmount
            } else {
                totalAmount
            }
            
            val orderItems = cartItems.map { cartItem ->
                OrderItem(
                    orderId = 0, // Will be set by database
                    productName = cartItem.productName,
                    waterType = cartItem.waterType,
                    uom = cartItem.uom,
                    quantity = cartItem.quantity,
                    price = cartItem.price,
                    customerName = cartItem.customerName,
                    customerAddress = cartItem.customerAddress
                )
            }
            
            // Get advance order information from dialog if available
            val isAdvanceOrder = dialog?.findViewById<Switch>(R.id.advanceOrderToggle)?.isChecked ?: false
            val dialogPickupDate = dialog?.findViewById<Button>(R.id.pickupDateButton)?.text?.toString()?.takeIf { it != "Select" }
            val dialogDeliveryDate = dialog?.findViewById<Button>(R.id.deliveryDateButton)?.text?.toString()?.takeIf { it != "Select" }
            
            val order = Order(
                userId = currentUserId,
                orderNumber = orderNumber,
                orderDate = java.util.Date(),
                status = OrderStatus.PROCESSING,
                totalAmount = finalAmount,
                totalItems = totalItems,
                items = orderItems,
                isAdvanceOrder = isAdvanceOrder,
                pickupDate = dialogPickupDate ?: pickupDate,
                deliveryDate = dialogDeliveryDate ?: deliveryDate
            )
            
            // Save order to database
            if (orderDatabaseHelper.addOrder(order)) {
                Log.d(TAG, "Order saved successfully: $orderNumber")
                
                // Spend hydrocoins if used
                if (isUsingHydroCoins && hydroCoinBalance > 0) {
                    val coinsToSpend = minOf(hydroCoinBalance, totalAmount.toInt())
                    hydroCoinDatabaseHelper.spendHydroCoins(currentUserId, coinsToSpend)
                    Log.d(TAG, "Spent $coinsToSpend hydrocoins")
                }
                
                AlertDialog.Builder(this)
                    .setTitle("Order Submitted!")
                    .setMessage("Your order has been successfully submitted.\nOrder Number: $orderNumber")
                    .setPositiveButton("OK") { _, _ ->
                        try {
                            // Clear the cart after successful order submission
                            cartDatabaseHelper.clearCart(currentUserId)
                            loadCartItems() // Refresh the cart to show empty state
                            
                            // Navigate to order history page
                            val intent = Intent(this, OrderHistory::class.java)
                            intent.putExtra("user_id", currentUserId)
                            startActivity(intent)
                            
                            Log.d(TAG, "Navigating to OrderHistory with user_id: $currentUserId")
                        } catch (e: Exception) {
                            Log.e(TAG, "Error after order submission: ${e.message}", e)
                            showToast("Order submitted but error occurred")
                        }
                    }
                    .setCancelable(false)
                    .show()
            } else {
                Log.e(TAG, "Failed to save order to database")
                showToast("Failed to submit order. Please try again.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing order submission dialog: ${e.message}", e)
            showToast("Error submitting order")
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh cart and hydrocoin balance when activity resumes
        loadCartItems()
    }

    private fun showToast(message: String) {
        try {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error showing toast: ${e.message}", e)
        }
    }
}
