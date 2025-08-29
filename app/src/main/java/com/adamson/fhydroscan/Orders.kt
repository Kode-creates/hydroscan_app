package com.adamson.fhydroscan

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.View
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
        dialog.setContentView(R.layout.dialog_order_details)

        // Set dialog title (date)
        dialog.findViewById<TextView>(R.id.dateTitle).text = orderDetails.date

        // Set total quantity and sales
        dialog.findViewById<TextView>(R.id.totalQuantity).text = orderDetails.totalQty.toString()
        dialog.findViewById<TextView>(R.id.totalSales).text = String.format("₱%.2f", orderDetails.totalSales)

        // Add customers to the list
        val customerList = dialog.findViewById<LinearLayout>(R.id.customerList)
        orderDetails.customers.forEach { customer ->
            val customerView = TextView(this).apply {
                text = customer
                textSize = 16f
                setPadding(0, 8, 0, 8)
            }
            customerList.addView(customerView)
        }

        // Set up close button
        dialog.findViewById<View>(R.id.closeButton).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}