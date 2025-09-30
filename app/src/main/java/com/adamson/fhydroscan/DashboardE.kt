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

class DashboardE : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard_e)

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

    private fun updateOverviewData() {
        // TODO: Replace with actual data from database or shared preferences
        findViewById<TextView>(R.id.gallonsCount).text = "25"
        findViewById<TextView>(R.id.revenueAmount).text = "₱950"
        findViewById<TextView>(R.id.pendingCount).text = "4"
    }
}