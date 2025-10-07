package com.adamson.fhydroscan

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.adamson.fhydroscan.database.OrderDatabaseHelper

class Dashboard : AppCompatActivity() {
    
    private lateinit var orderDatabaseHelper: OrderDatabaseHelper
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        
        // Initialize database helper
        orderDatabaseHelper = OrderDatabaseHelper(this)
        
        // Update overview data
        updateOverviewData()

        findViewById<CardView>(R.id.ordersCard).setOnClickListener {
            try {
                val intent = Intent(this, Today::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Unable to open Today's Orders. Please try again.", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }


        findViewById<CardView>(R.id.Sales).setOnClickListener {
            startActivity(Intent(this, Sales::class.java))
        }

        val dbscangen = findViewById<ImageView>(R.id.dbscangen)
        dbscangen.setOnClickListener {
            val intent = Intent(this, Scan::class.java)
            startActivity(intent)
        }

        val dbinv = findViewById<ImageView>(R.id.dbinv)
        dbinv.setOnClickListener {
            val intent = Intent(this, Inventory::class.java)
            startActivity(intent)
        }

        val dborders = findViewById<ImageView>(R.id.dborders)
        dborders.setOnClickListener {
            val intent = Intent(this, Orders::class.java)
            startActivity(intent)
        }



        // Arrow click listener for Sales Report title
        findViewById<ImageView>(R.id.salesArrow).setOnClickListener {
            val intent = Intent(this, Sales::class.java)
            startActivity(intent)
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.bthome -> {
                    true
                }
                R.id.btscan -> {
                    startActivity(Intent(this, Scan::class.java))
                    true
                }
                R.id.btsetting -> {
                    startActivity(Intent(this, Settings::class.java))
                    true
                }
                else -> false
            }
        }
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
            println("DEBUG: Dashboard - Found ${todaysOrders.size} orders today")
            todaysOrders.forEachIndexed { index, order ->
                println("DEBUG: Order $index: ${order.name} - Total: ${order.total}")
            }
            
            // Calculate metrics
            val totalOrders = todaysOrders.size
            val totalRevenue = todaysOrders.sumOf { it.total }
            val pendingOrders = todaysOrders.count { it.status == "Pending" }
            
            println("DEBUG: Dashboard - Total Revenue: $totalRevenue")
            
            // Update overview card UI
            findViewById<TextView>(R.id.ordersCount).text = totalOrders.toString()
            findViewById<TextView>(R.id.revenueAmount).text = String.format("₱%.2f", totalRevenue)
            findViewById<TextView>(R.id.pendingCount).text = pendingOrders.toString()
            
            // Update Today's Orders card
            updateTodaysOrdersCard(todaysOrders)
            
        } catch (e: Exception) {
            // Handle error gracefully
            println("DEBUG: Dashboard - Error updating overview: ${e.message}")
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





