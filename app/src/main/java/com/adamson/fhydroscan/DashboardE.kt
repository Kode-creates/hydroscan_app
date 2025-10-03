package com.adamson.fhydroscan

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.adamson.fhydroscan.database.OrderDatabaseHelper

class DashboardE : AppCompatActivity() {
    
    private lateinit var orderDatabaseHelper: OrderDatabaseHelper
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard_e)
        
        // Initialize database helper
        orderDatabaseHelper = OrderDatabaseHelper(this)

        // Set up orders card click
        findViewById<CardView>(R.id.ordersCard).setOnClickListener {
            startActivity(Intent(this, Today::class.java))
        }

        // Set up scan QR click
        findViewById<ImageView>(R.id.dbscanE).setOnClickListener {
            startActivity(Intent(this, ScanE::class.java))
        }

        // Set up bottom navigation
        val bottomNave = findViewById<BottomNavigationView>(R.id.bottomNave)
        bottomNave.selectedItemId = R.id.bthomee // Set home as selected

        bottomNave.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.bthomee -> {
                    // Already on home, no need to do anything
                    true
                }
                R.id.btscan2 -> {
                    // Handle scan button click
                    startActivity(Intent(this, ScanE::class.java))
                    true
                }
                R.id.btsetting2 -> {
                    // Handle settings button click
                    startActivity(Intent(this, SettingE::class.java))
                    true
                }
                else -> false
            }
        }


        // Update overview card with dynamic data
        updateOverviewData()
    }
    
    override fun onResume() {
        super.onResume()
        // Update data when returning to dashboard
        updateOverviewData()
    }

    private fun updateOverviewData() {
        try {
            // Get today's orders from database
            val todaysOrders = orderDatabaseHelper.getTodaysCustomerOrders()
            
            // Debug logging
            println("DEBUG: DashboardE - Found ${todaysOrders.size} orders today")
            todaysOrders.forEachIndexed { index, order ->
                println("DEBUG: Order $index: ${order.name} - Total: ${order.total}")
            }
            
            // Calculate metrics
            val totalOrders = todaysOrders.size
            val totalRevenue = todaysOrders.sumOf { it.total }
            val pendingOrders = todaysOrders.count { it.status == "Pending" }
            
            println("DEBUG: DashboardE - Total Revenue: $totalRevenue")
            
            // Update overview card UI
            findViewById<TextView>(R.id.ordersCount).text = totalOrders.toString()
            findViewById<TextView>(R.id.revenueAmount).text = String.format("₱%.2f", totalRevenue)
            findViewById<TextView>(R.id.pendingCount).text = pendingOrders.toString()
            
            // Update Today's Orders card
            updateTodaysOrdersCard(todaysOrders)
            
        } catch (e: Exception) {
            // Handle error gracefully
            println("DEBUG: DashboardE - Error updating overview: ${e.message}")
            findViewById<TextView>(R.id.ordersCount).text = "0"
            findViewById<TextView>(R.id.revenueAmount).text = "₱0.00"
            findViewById<TextView>(R.id.pendingCount).text = "0"
            findViewById<TextView>(R.id.todaysOrdersCount).text = "0"
            findViewById<TextView>(R.id.customerListText).text = "No recent orders"
            e.printStackTrace()
        }
    }
    
    private fun updateTodaysOrdersCard(orders: List<CustomerOrder>) {
        try {
            // Update orders count
            findViewById<TextView>(R.id.todaysOrdersCount).text = orders.size.toString()
            
            // Create customer list text
            if (orders.isEmpty()) {
                findViewById<TextView>(R.id.customerListText).text = "No recent orders"
            } else {
                // Sort orders by most recent first (assuming orders are added with increasing IDs or timestamps)
                // Take the last 3-4 orders (most recent) and reverse to show newest first
                val recentOrders = orders.takeLast(4).reversed()
                
                val customerList = recentOrders.joinToString("\n") { order ->
                    // Extract quantity from product string
                    val quantityMatch = Regex("x(\\d+)").find(order.product)
                    val quantity = quantityMatch?.groupValues?.get(1)?.toIntOrNull() ?: 1
                    "${order.name} x$quantity"
                }
                findViewById<TextView>(R.id.customerListText).text = customerList
            }
        } catch (e: Exception) {
            findViewById<TextView>(R.id.todaysOrdersCount).text = "0"
            findViewById<TextView>(R.id.customerListText).text = "No recent orders"
            e.printStackTrace()
        }
    }
}