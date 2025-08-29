package com.adamson.fhydroscan

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.content.Intent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OrderHistory : AppCompatActivity() {
    private val orders = mutableListOf<Order>()
    private val dateFormat = SimpleDateFormat("MM-dd-yyyy", Locale.getDefault())
    private val TAG = "OrderHistoryActivity"

    data class Order(
        val date: Date = Date(),
        val address: String,
        val waterType: String,
        val quantity: Int
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_order_history)
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
            val ordersList = findViewById<LinearLayout>(R.id.ordersList)
                ?: throw IllegalStateException("Orders list view not found")

            // Clear previous views
            ordersList.removeAllViews()
            Log.d(TAG, "Order history - Total orders: ${orders.size}")

            if (orders.isEmpty()) {
                ordersList.addView(TextView(this).apply {
                    text = "No orders yet"
                    textSize = 16f
                    setPadding(0, 32, 0, 32)
                    gravity = android.view.Gravity.CENTER
                })
                Log.d(TAG, "No orders to display")
            } else {
                orders.sortedByDescending { it.date }.forEach { order ->
                    try {
                        ordersList.addView(TextView(this).apply {
                            text = """
                                Date: ${dateFormat.format(order.date)}
                                Address: ${order.address}
                                Water Type: ${order.waterType}
                                Quantity: ${order.quantity}
                                
                            """.trimIndent()
                            setPadding(16, 8, 16, 8)
                            setBackgroundResource(android.R.color.white)
                        })
                        Log.d(TAG, "Added order to history: $order")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error adding order to history view: ${e.message}", e)
                    }
                }
            }
            Log.d(TAG, "Order history setup completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up order history: ${e.message}", e)
            showToast("Error setting up order history")
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

    private fun showToast(message: String) {
        try {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            Log.d(TAG, "Toast shown: $message")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing toast: ${e.message}", e)
        }
    }
}
