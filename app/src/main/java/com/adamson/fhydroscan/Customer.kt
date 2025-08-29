package com.adamson.fhydroscan

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.adamson.fhydroscan.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.content.Intent
import android.view.LayoutInflater
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Customer : AppCompatActivity() {
    private val orders = mutableListOf<Order>()
    private val dateFormat = SimpleDateFormat("MM-dd-yyyy", Locale.getDefault())
    private val TAG = "CustomerActivity"

    data class Order(
        val date: Date = Date(),
        val address: String,
        val waterType: String,
        val quantity: Int
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_customer)
            setupClickListeners()
            setupBottomNavigation()
            Log.d(TAG, "Activity created successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}", e)
            showToast("Error initializing the screen")
            finish()
        }
    }

    private fun setupClickListeners() {
        try {
            findViewById<ImageView>(R.id.orderWaterCard)?.let {
                it.setOnClickListener { 
                    Log.d(TAG, "Order Water card clicked")
                    startActivity(Intent(this, ScanC::class.java))
                }
            } ?: throw IllegalStateException("Order Water card not found")

            // Setup product image click listeners
            setupProductImageListeners()
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up click listeners: ${e.message}", e)
            showToast("Error setting up the screen")
        }
    }

    private fun setupProductImageListeners() {
        try {
            // Setup click listeners for each product image with custom titles
            findViewById<ImageView>(R.id.prefilla)?.setOnClickListener {
                showProductDialog("Refill Alkaline 20L")
            }
            
            findViewById<ImageView>(R.id.prefillm)?.setOnClickListener {
                showProductDialog("Refill Mineral 20L")
            }
            
            findViewById<ImageView>(R.id.prefillas)?.setOnClickListener {
                showProductDialog("Refill Alkaline 10L")
            }
            
            findViewById<ImageView>(R.id.prefillms)?.setOnClickListener {
                showProductDialog("Refill Mineral 10L")
            }
            
            findViewById<ImageView>(R.id.pnews)?.setOnClickListener {
                showProductDialog("New Slim Gallon")
            }
            
            findViewById<ImageView>(R.id.pnewr)?.setOnClickListener {
                showProductDialog("New Round Gallon")
            }
            
            findViewById<ImageView>(R.id.pbcap)?.setOnClickListener {
                showProductDialog("Big Cap Cover")
            }
            
            findViewById<ImageView>(R.id.pscap)?.setOnClickListener {
                showProductDialog("Small Cap Cover")
            }
            
            findViewById<ImageView>(R.id.prcap)?.setOnClickListener {
                showProductDialog("Round Cap Cover")
            }
            
            findViewById<ImageView>(R.id.pnscap)?.setOnClickListener {
                showProductDialog("Non-leak Cover")
            }
            
            Log.d(TAG, "Product image listeners setup completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up product image listeners: ${e.message}", e)
        }
    }

    private fun showProductDialog(title: String) {
        try {
            val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_product, null)
            
            // Set the title
            dialogView.findViewById<TextView>(R.id.dialogTitle).text = title
            
            // Setup quantity controls
            val quantityDisplay = dialogView.findViewById<TextView>(R.id.quantityDisplay)
            val minusButton = dialogView.findViewById<Button>(R.id.minusButton)
            val plusButton = dialogView.findViewById<Button>(R.id.plusButton)
            
            var quantity = 0
            
            minusButton.setOnClickListener {
                if (quantity > 0) {
                    quantity--
                    quantityDisplay.text = quantity.toString()
                }
            }
            
            plusButton.setOnClickListener {
                quantity++
                quantityDisplay.text = quantity.toString()
            }
            
            // Create the main dialog first
            val dialog = MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create()
            
            // Setup buttons
            dialogView.findViewById<Button>(R.id.addToCartButton).setOnClickListener {
                if (quantity > 0) {
                    // Show alert message
                    MaterialAlertDialogBuilder(this)
                        .setTitle("Success!")
                        .setMessage("Added $quantity item(s) to cart")
                        .setPositiveButton("OK") { _, _ ->
                            dialog.dismiss() // Close the dialog and return to customer page
                        }
                        .setCancelable(false)
                        .show()
                } else {
                    showToast("Please select quantity")
                }
            }
            
            dialogView.findViewById<Button>(R.id.cancelButton).setOnClickListener {
                dialog.dismiss()
            }
            
            // Show the dialog
            dialog.show()
                
        } catch (e: Exception) {
            Log.e(TAG, "Error showing product dialog: ${e.message}", e)
            showToast("Error showing product dialog")
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