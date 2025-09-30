package com.adamson.fhydroscan

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView

class Dashboard : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

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


        // Arrow click listener for Today's Orders title
        findViewById<ImageView>(R.id.ordersArrow).setOnClickListener {
            val intent = Intent(this, Today::class.java)
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
}





