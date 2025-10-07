package com.adamson.fhydroscan

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.adamson.fhydroscan.OrderHistoryAdapter
import com.adamson.fhydroscan.data.Order
import com.adamson.fhydroscan.data.OrderItem
import com.adamson.fhydroscan.data.OrderStatus
import com.adamson.fhydroscan.database.DatabaseHelper
import com.adamson.fhydroscan.database.OrderDatabaseHelper
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.SimpleDateFormat
import java.util.*

class OrderHistory : AppCompatActivity() {
    private val TAG = "OrderHistoryActivity"
    private lateinit var orderDatabaseHelper: OrderDatabaseHelper
    private lateinit var userDatabaseHelper: DatabaseHelper
    private lateinit var orderAdapter: OrderHistoryAdapter
    private lateinit var ordersRecyclerView: RecyclerView
    private lateinit var emptyOrdersText: TextView
    private var currentUserId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_order_history)
            
            // Initialize database and get current user
            orderDatabaseHelper = OrderDatabaseHelper(this)
            userDatabaseHelper = DatabaseHelper(this)
            getCurrentUserId()
            
            // Setup UI components
            setupOrderHistory()
            setupBottomNavigation()
            
            Log.d(TAG, "OrderHistory activity created successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}", e)
            showToast("Error initializing the order history page")
            finish()
        }
    }

    private fun setupOrderHistory() {
        try {
            ordersRecyclerView = findViewById(R.id.ordersRecyclerView)
            emptyOrdersText = findViewById(R.id.emptyOrdersText)
            
            // Setup RecyclerView
            orderAdapter = OrderHistoryAdapter(mutableListOf()) { order ->
                showOrderDetailDialog(order)
            }
            ordersRecyclerView.layoutManager = LinearLayoutManager(this)
            ordersRecyclerView.adapter = orderAdapter
            
            // Load orders
            loadOrders()
            
            Log.d(TAG, "Order history setup completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up order history: ${e.message}", e)
            showToast("Error setting up order history")
        }
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

    private fun loadOrders() {
        if (currentUserId.isNotEmpty()) {
            Log.d(TAG, "Loading orders for user: $currentUserId")
            val orders = orderDatabaseHelper.getOrders(currentUserId)
            Log.d(TAG, "Found ${orders.size} orders")
            
            orderAdapter.updateOrders(orders)
            
            // Show/hide empty orders message
            if (orders.isEmpty()) {
                emptyOrdersText.visibility = TextView.VISIBLE
                ordersRecyclerView.visibility = RecyclerView.GONE
                Log.d(TAG, "No orders found, showing empty message")
            } else {
                emptyOrdersText.visibility = TextView.GONE
                ordersRecyclerView.visibility = RecyclerView.VISIBLE
                Log.d(TAG, "Showing ${orders.size} orders")
            }
        } else {
            Log.w(TAG, "No user ID found")
            showToast("User not logged in")
            emptyOrdersText.visibility = TextView.VISIBLE
            ordersRecyclerView.visibility = RecyclerView.GONE
        }
    }

    private fun setupBottomNavigation() {
        try {
            val bottomNavc = findViewById<BottomNavigationView>(R.id.bottomNavc)
            bottomNavc.selectedItemId = R.id.bthis // Set order history as selected

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

    private fun showOrderDetailDialog(order: Order) {
        try {
            val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_order_detail, null)
            
            // Get user information (we'll need to get this from the database)
            val userInfo = getUserInfo(order.userId)
            
            // Set order information
            dialogView.findViewById<TextView>(R.id.orderDate).text = "Date Ordered: ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(order.orderDate)}"
            dialogView.findViewById<TextView>(R.id.orderTotal).text = "Total Amount: ₱${String.format("%.2f", order.totalAmount)}"
            
            // Handle advance order information
            val advanceOrderInfo = dialogView.findViewById<LinearLayout>(R.id.advanceOrderInfo)
            if (order.isAdvanceOrder) {
                advanceOrderInfo.visibility = View.VISIBLE
                dialogView.findViewById<TextView>(R.id.pickupDate).text = "Date of Pick up: ${order.pickupDate ?: "-"}"
                dialogView.findViewById<TextView>(R.id.scheduledDeliveryDate).text = "Date to be Delivered: ${order.deliveryDate ?: "-"}"
            } else {
                advanceOrderInfo.visibility = View.GONE
            }
            
            // Set customer information
            dialogView.findViewById<TextView>(R.id.customerName).text = "Name: ${userInfo.first}"
            dialogView.findViewById<TextView>(R.id.customerPhone).text = "Phone: ${userInfo.second}"
            dialogView.findViewById<TextView>(R.id.customerAddress).text = "Address: ${userInfo.third}"
            
            // Set order status
            val statusText = dialogView.findViewById<TextView>(R.id.orderStatus)
            statusText.text = order.status.displayName
            when (order.status) {
                OrderStatus.PROCESSING -> {
                    statusText.setBackgroundResource(R.drawable.status_processing_background)
                }
                OrderStatus.DELIVERED -> {
                    statusText.setBackgroundResource(R.drawable.status_delivered_background)
                }
                OrderStatus.CANCELLED -> {
                    statusText.setBackgroundResource(R.drawable.status_cancelled_background)
                }
            }
            
            // Set delivery date
            val deliveryDateText = dialogView.findViewById<TextView>(R.id.deliveryDate)
            if (order.status == OrderStatus.DELIVERED) {
                deliveryDateText.text = "Date Delivered: ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(order.orderDate)}"
            } else {
                deliveryDateText.text = "Date Delivered: -"
            }
            
            // Populate items list
            populateItemsList(dialogView, order.items)
            
                   // Setup cancel button for processing orders
                   val cancelButton = dialogView.findViewById<Button>(R.id.cancelOrderButton)
                   if (order.status == OrderStatus.PROCESSING) {
                       cancelButton.visibility = android.view.View.VISIBLE
                       cancelButton.setOnClickListener {
                           showCancelConfirmationDialog(order)
                       }
                   } else {
                       cancelButton.visibility = android.view.View.GONE
                   }

            // Create and show dialog
            val dialog = AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create()

            
            dialogView.findViewById<Button>(R.id.closeButton).setOnClickListener {
                dialog.dismiss()
            }
            
            dialog.show()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error showing order detail dialog: ${e.message}", e)
            showToast("Error loading order details")
        }
    }

    private fun getUserInfo(userId: String): Triple<String, String, String> {
        try {
            val user = userDatabaseHelper.getUserById(userId)
            return if (user != null) {
                Triple(
                    user.fullName ?: "Customer Name",
                    user.phoneNumber ?: userId,
                    user.address ?: "Customer Address"
                )
            } else {
                Triple("Customer Name", userId, "Customer Address")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user info: ${e.message}", e)
            return Triple("Customer Name", userId, "Customer Address")
        }
    }

    private fun populateItemsList(dialogView: android.view.View, items: List<OrderItem>) {
        val itemsContainer = dialogView.findViewById<LinearLayout>(R.id.itemsListContainer)
        itemsContainer.removeAllViews()
        
        for (item in items) {
            val itemView = LayoutInflater.from(this).inflate(R.layout.item_order_detail_item, null)
            
            itemView.findViewById<TextView>(R.id.itemName).text = item.productName
            itemView.findViewById<TextView>(R.id.itemQuantity).text = "Qty: ${item.quantity}"
            itemView.findViewById<TextView>(R.id.itemPrice).text = "₱${String.format("%.2f", item.price * item.quantity)}"
            
            val details = buildString {
                // Extract gallon type from product name if present
                val gallonType = extractGallonTypeFromProductName(item.productName)
                val waterTypeDisplay = if (gallonType.isNotEmpty()) {
                    "${item.waterType} (${gallonType})"
                } else {
                    item.waterType
                }
                append("Water Type: $waterTypeDisplay")
                if (item.waterType != "Accessory") {
                    append(" • UOM: ${item.uom}")
                }
            }
            itemView.findViewById<TextView>(R.id.itemDetails).text = details
            
            itemsContainer.addView(itemView)
        }
    }
    
    private fun extractGallonTypeFromProductName(productName: String): String {
        // Check if product name contains gallon type in parentheses
        val regex = "\\((Slim|Round)\\)".toRegex()
        val matchResult = regex.find(productName)
        return matchResult?.groupValues?.get(1) ?: ""
    }

    private fun showCancelConfirmationDialog(order: Order) {
        AlertDialog.Builder(this)
            .setTitle("Cancel Order")
            .setMessage("Are you sure you want to cancel this order?\n\nTotal: ₱${String.format("%.2f", order.totalAmount)}")
            .setPositiveButton("Yes, Cancel") { _, _ ->
                cancelOrder(order)
            }
            .setNegativeButton("Keep Order") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(true)
            .show()
    }

    private fun cancelOrder(order: Order) {
        try {
            if (orderDatabaseHelper.updateOrderStatus(order.id, OrderStatus.CANCELLED)) {
                showToast("Order cancelled successfully")
                loadOrders() // Refresh the orders list
            } else {
                showToast("Failed to cancel order. Please try again.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling order: ${e.message}", e)
            showToast("Error cancelling order")
        }
    }


    override fun onResume() {
        super.onResume()
        // Refresh orders when activity resumes
        loadOrders()
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
