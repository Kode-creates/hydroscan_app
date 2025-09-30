package com.adamson.fhydroscan

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.adamson.fhydroscan.R
import com.adamson.fhydroscan.data.CartItem
import com.adamson.fhydroscan.database.CartDatabaseHelper
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Customer : AppCompatActivity() {
    private val orders = mutableListOf<Order>()
    private val dateFormat = SimpleDateFormat("MM-dd-yyyy", Locale.getDefault())
    private val TAG = "CustomerActivity"
    private lateinit var cartDatabaseHelper: CartDatabaseHelper
    private var currentUserId: String = ""

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
            
            // Initialize cart database and get current user
            cartDatabaseHelper = CartDatabaseHelper(this)
            getCurrentUserId()
            
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
            
            // Determine product type and what selections to show
            val isRefill20L = title.contains("Refill") && title.contains("20L")
            val isRefill10L = title.contains("Refill") && title.contains("10L")
            val isNewGallon = title.contains("Gallon") && !title.contains("Refill")
            
            val waterTypeContainer = dialogView.findViewById<LinearLayout>(R.id.waterTypeContainer)
            val gallonTypeContainer = dialogView.findViewById<LinearLayout>(R.id.gallonTypeContainer)
            val radioMineral = dialogView.findViewById<RadioButton>(R.id.radioMineral)
            val radioAlkaline = dialogView.findViewById<RadioButton>(R.id.radioAlkaline)
            val radioNoRefill = dialogView.findViewById<RadioButton>(R.id.radioNoRefill)
            val radioSlim = dialogView.findViewById<RadioButton>(R.id.radioSlim)
            val radioRound = dialogView.findViewById<RadioButton>(R.id.radioRound)
            
            // Show/hide selections based on product type
            when {
                isRefill20L -> {
                    // prefilla and prefillm: Only gallon type and quantity
                    waterTypeContainer.visibility = LinearLayout.GONE
                    gallonTypeContainer.visibility = LinearLayout.VISIBLE
                    radioSlim.isChecked = true // Default to Slim
                }
                isRefill10L -> {
                    // prefillas and prefillms: Only quantity
                    waterTypeContainer.visibility = LinearLayout.GONE
                    gallonTypeContainer.visibility = LinearLayout.GONE
                }
                isNewGallon -> {
                    // pnews and pnewr: Only water type (3 options) and quantity
                    waterTypeContainer.visibility = LinearLayout.VISIBLE
                    gallonTypeContainer.visibility = LinearLayout.GONE
                    radioMineral.isChecked = true // Default to Mineral
                }
                else -> {
                    // Other products: No special selections
                    waterTypeContainer.visibility = LinearLayout.GONE
                    gallonTypeContainer.visibility = LinearLayout.GONE
                }
            }
            
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
                    // Get selected water type and gallon type based on product type
                    val selectedWaterType = when {
                        isRefill20L -> {
                            // For refill 20L, determine water type from title
                            if (title.contains("Alkaline")) "Alkaline" else "Mineral"
                        }
                        isRefill10L -> {
                            // For refill 10L, determine water type from title
                            if (title.contains("Alkaline")) "Alkaline" else "Mineral"
                        }
                        isNewGallon -> {
                            // For new gallons, get from radio selection
                            when {
                                radioMineral.isChecked -> "Mineral"
                                radioAlkaline.isChecked -> "Alkaline"
                                radioNoRefill.isChecked -> "No Refill"
                                else -> "Mineral"
                            }
                        }
                        else -> {
                            getProductDetails(title).first // Use default detection for other products
                        }
                    }
                    
                    val selectedGallonType = when {
                        isRefill20L -> {
                            // For refill 20L, get from radio selection
                            if (radioSlim.isChecked) "Slim" else "Round"
                        }
                        else -> {
                            "" // No gallon type for other products
                        }
                    }
                    
                    // Add item to cart with selected water type and gallon type
                    if (addToCartWithWaterTypeAndGallonType(title, quantity, selectedWaterType, selectedGallonType)) {
                        // Show success message
                        MaterialAlertDialogBuilder(this)
                            .setTitle("Success!")
                            .setMessage("Added $quantity item(s) to cart")
                            .setPositiveButton("OK") { _, _ ->
                                dialog.dismiss() // Close the dialog and return to customer page
                            }
                            .setCancelable(false)
                            .show()
                    } else {
                        showToast("Failed to add item to cart")
                    }
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

    private fun getCurrentUserId() {
        // Get current user ID from SharedPreferences
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        currentUserId = sharedPref.getString("current_user_id", "") ?: ""
        
        if (currentUserId.isEmpty()) {
            // If no user ID found, try to get from intent
            currentUserId = intent.getStringExtra("user_id") ?: ""
        }
    }

    private fun addToCart(productName: String, quantity: Int): Boolean {
        if (currentUserId.isEmpty()) {
            showToast("User not logged in")
            return false
        }
        
        // Determine water type and UOM based on product name
        val (waterType, uom) = getProductDetails(productName)
        
        return addToCartWithWaterType(productName, quantity, waterType)
    }

    private fun addToCartWithWaterType(productName: String, quantity: Int, waterType: String): Boolean {
        if (currentUserId.isEmpty()) {
            showToast("User not logged in")
            return false
        }
        
        // Determine UOM and price based on product name
        val uom = getProductDetails(productName).second
        val price = getProductPrice(productName)
        
        // Create cart item
        val cartItem = CartItem(
            userId = currentUserId,
            productName = productName,
            waterType = waterType,
            uom = uom,
            quantity = quantity,
            price = price,
            customerName = "Self", // Customer is adding for themselves
            customerAddress = "Self", // Customer is adding for themselves
            addedAt = "" // Will be set by database
        )
        
        // Check if item already exists in cart
        val existingItem = cartDatabaseHelper.itemExists(
            cartItem.userId, 
            cartItem.productName, 
            cartItem.waterType, 
            cartItem.customerName
        )
        
        return if (existingItem != null) {
            // Update quantity if item exists
            cartDatabaseHelper.updateQuantity(existingItem.id, existingItem.quantity + cartItem.quantity)
        } else {
            // Add new item to cart
            cartDatabaseHelper.addToCart(cartItem)
        }
    }

    private fun addToCartWithWaterTypeAndGallonType(productName: String, quantity: Int, waterType: String, gallonType: String): Boolean {
        if (currentUserId.isEmpty()) {
            showToast("User not logged in")
            return false
        }
        
        // Determine UOM and price based on product name
        val uom = getProductDetails(productName).second
        val price = getProductPrice(productName)
        
        // Create enhanced product name with gallon type if applicable
        val enhancedProductName = if (gallonType.isNotEmpty()) {
            "$productName ($gallonType)"
        } else {
            productName
        }
        
        // Create cart item
        val cartItem = CartItem(
            userId = currentUserId,
            productName = enhancedProductName,
            waterType = waterType,
            uom = uom,
            quantity = quantity,
            price = price,
            customerName = "Self", // Customer is adding for themselves
            customerAddress = "Self", // Customer is adding for themselves
            addedAt = "" // Will be set by database
        )
        
        // Check if item already exists in cart
        val existingItem = cartDatabaseHelper.itemExists(
            cartItem.userId, 
            cartItem.productName, 
            cartItem.waterType, 
            cartItem.customerName
        )
        
        return if (existingItem != null) {
            // Update quantity if item exists
            cartDatabaseHelper.updateQuantity(existingItem.id, existingItem.quantity + cartItem.quantity)
        } else {
            // Add new item to cart
            cartDatabaseHelper.addToCart(cartItem)
        }
    }

    private fun getProductDetails(productName: String): Pair<String, String> {
        return when {
            productName.contains("Alkaline", ignoreCase = true) -> {
                if (productName.contains("20L")) Pair("Alkaline", "20L")
                else Pair("Alkaline", "10L")
            }
            productName.contains("Mineral", ignoreCase = true) -> {
                if (productName.contains("20L")) Pair("Mineral", "20L")
                else Pair("Mineral", "10L")
            }
            productName.contains("Gallon", ignoreCase = true) -> {
                // For gallon products, water type will be determined by user selection
                // Default to Mineral, but this will be overridden by user choice
                if (productName.contains("Slim")) Pair("Mineral", "1 Gallon")
                else Pair("Mineral", "1 Gallon")
            }
            productName.contains("Cover", ignoreCase = true) -> {
                Pair("Accessory", "1 Piece")
            }
            else -> Pair("Mineral", "1 Piece")
        }
    }

    private fun getProductPrice(productName: String): Double {
        return when {
            productName.contains("Refill Alkaline 20L") -> 50.0
            productName.contains("Refill Mineral 20L") -> 30.0
            productName.contains("Refill Alkaline 10L") -> 25.0
            productName.contains("Refill Mineral 10L") -> 15.0
            productName.contains("New Slim Gallon") -> 200.0
            productName.contains("New Round Gallon") -> 200.0
            productName.contains("Big Cap Cover") -> 25.0
            productName.contains("Small Cap Cover") -> 10.0
            productName.contains("Round Cap Cover") -> 5.0
            productName.contains("Non-leak Cover") -> 3.0
            else -> 0.0
        }
    }
}