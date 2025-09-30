package com.adamson.fhydroscan

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class Orders : AppCompatActivity() {

    data class OrderDetails(
        val date: String,
        val totalQty: Int,
        val totalSales: Double,
        val customers: List<String>
    )

    private val orderData = mapOf(
        "April 17" to OrderDetails(
            "April 17",
            34,
            1296.0,
            listOf("Customer A", "Customer B", "Customer C")
        ),
        "April 16" to OrderDetails(
            "April 16",
            34,
            1296.0,
            listOf("Customer D", "Customer E", "Customer F")
        ),
        "April 15" to OrderDetails(
            "April 15",
            34,
            1296.0,
            listOf("Customer G", "Customer H", "Customer I")
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_orders)

        setupCardClickListeners()
        setupBottomNavigation()
    }

    private fun setupCardClickListeners() {
        findViewById<View>(R.id.april17Card).setOnClickListener {
            showOrderDetailsDialog(orderData["April 17"]!!)
        }
        findViewById<View>(R.id.april16Card).setOnClickListener {
            showOrderDetailsDialog(orderData["April 16"]!!)
        }
        findViewById<View>(R.id.april15Card).setOnClickListener {
            showOrderDetailsDialog(orderData["April 15"]!!)
        }
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.bthome -> {
                    startActivity(Intent(this, Dashboard::class.java))
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

    private fun showOrderDetailsDialog(orderDetails: OrderDetails) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_order_detail)

        // Set order date
        dialog.findViewById<TextView>(R.id.orderDate).text = "Date Ordered: ${orderDetails.date}"

        // Set total amount
        dialog.findViewById<TextView>(R.id.orderTotal).text = "Total Amount: ${String.format("₱%.2f", orderDetails.totalSales)}"

        // Add customers to the items list container
        val itemsListContainer = dialog.findViewById<LinearLayout>(R.id.itemsListContainer)
        orderDetails.customers.forEach { customer ->
            val customerView = TextView(this).apply {
                text = "• $customer"
                textSize = 10f
                setTextColor(resources.getColor(android.R.color.black, null))
                setPadding(0, 4, 0, 4)
            }
            itemsListContainer.addView(customerView)
        }

        // Set up close button
        dialog.findViewById<Button>(R.id.closeButton).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}