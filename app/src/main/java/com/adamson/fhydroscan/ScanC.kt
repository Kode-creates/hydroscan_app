package com.adamson.fhydroscan

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.adamson.fhydroscan.data.CartItem
import com.adamson.fhydroscan.database.CartDatabaseHelper
import com.adamson.fhydroscan.databinding.ActivityScanCBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.Calendar
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.app.DatePickerDialog
import com.google.mlkit.vision.barcode.common.Barcode

class ScanC : AppCompatActivity() {

    private lateinit var binding: ActivityScanCBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var cartDatabaseHelper: CartDatabaseHelper
    private var imageAnalyzer: ImageAnalysis? = null
    private var hasHandledScan = false
    private var currentUserId: String = ""

    private val requestPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanCBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Request camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
        
        // Initialize cart database and get current user
        cartDatabaseHelper = CartDatabaseHelper(this)
        getCurrentUserId()
        
        // Setup bottom navigation
        setupBottomNavigation()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also { it.setSurfaceProvider(binding.viewFinder.surfaceProvider) }

            val barcodeScannerOptions = BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()

            val scanner = BarcodeScanning.getClient(barcodeScannerOptions)

            imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val image = InputImage.fromMediaImage(
                                mediaImage,
                                imageProxy.imageInfo.rotationDegrees
                            )

                            scanner.process(image)
                                .addOnSuccessListener { barcodes ->
                                    for (barcode in barcodes) {
                                        barcode.rawValue?.let { payload ->
                                            if (!hasHandledScan) {
                                                hasHandledScan = true
                                                runOnUiThread {
                                                    showCustomerDialog(payload)
                                                }
                                            }
                                        }
                                    }
                                }
                                .addOnFailureListener { it.printStackTrace() }
                                .addOnCompleteListener { imageProxy.close() }
                        } else {
                            imageProxy.close()
                        }
                    }
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalyzer
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun showCustomerDialog(payload: String) {
        // Parse payload assuming format: name=Juan Dela Cruz;address=123 Main St;unit=2
        val data = payload.split(";").mapNotNull {
            val idx = it.indexOf("=")
            if (idx > 0 && idx < it.length - 1) it.substring(0, idx).trim() to it.substring(idx + 1).trim() else null
        }.toMap()

        val name = data["name"] ?: "-"
        val address = data["address"] ?: "-"
        val uom = data["unit"] ?: "-"

        val dialogView = layoutInflater.inflate(R.layout.dialog_scanned_cus, null)

        val nameValue = dialogView.findViewById<TextView>(R.id.nameValue)
        val addressValue = dialogView.findViewById<TextView>(R.id.addressValue)
        val uomValue = dialogView.findViewById<TextView>(R.id.uomValue)
        val quantityDisplay = dialogView.findViewById<TextView>(R.id.quantityDisplay)
        val minusButton = dialogView.findViewById<Button>(R.id.minusButton)
        val plusButton = dialogView.findViewById<Button>(R.id.plusButton)
        val radioMineral = dialogView.findViewById<RadioButton>(R.id.radioMineral)
        val radioAlkaline = dialogView.findViewById<RadioButton>(R.id.radioAlkaline)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)
        val submitButton = dialogView.findViewById<Button>(R.id.submitButton)

        nameValue.text = name
        addressValue.text = address
        uomValue.text = uom // This already contains the detailed gallon type like "20L Slim", "20L Round", "10L Slim"
        radioMineral.isChecked = true

        // Quantity counter
        var quantity = 1
        quantityDisplay.text = quantity.toString()

        minusButton.setOnClickListener {
            if (quantity > 1) {
                quantity--
                quantityDisplay.text = quantity.toString()
            }
        }

        plusButton.setOnClickListener {
            if (quantity < 30) {
                quantity++
                quantityDisplay.text = quantity.toString()
            }
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        cancelButton.setOnClickListener { dialog.dismiss(); hasHandledScan = false }
        submitButton.setOnClickListener {
            val waterType = if (radioMineral.isChecked) "Mineral" else "Alkaline"
            
            // Determine product name and price based on UOM and water type
            val productName = getProductNameFromQR(uom, waterType)
            val price = getPriceFromQR(uom, waterType)
            
            // Add item to cart
            val cartItem = CartItem(
                userId = currentUserId,
                productName = productName,
                waterType = waterType,
                uom = uom,
                quantity = quantity,
                price = price,
                customerName = name,
                customerAddress = address,
                addedAt = "" // Will be set by database
            )
            
            if (addToCart(cartItem)) {
                dialog.dismiss()
                showOrderConfirmationDialog()
            } else {
                Toast.makeText(this, "Failed to add item to cart", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun showOrderConfirmationDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_order_confirmation, null)
        
        val orderNowButton = dialogView.findViewById<Button>(R.id.orderNowButton)
        val addMoreButton = dialogView.findViewById<Button>(R.id.addMoreButton)
        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()
        
        orderNowButton.setOnClickListener {
            dialog.dismiss()
            // Navigate to Cart page
            startActivity(Intent(this, Cart::class.java))
            hasHandledScan = false
        }
        
        addMoreButton.setOnClickListener {
            dialog.dismiss()
            // Stay on ScanC page to scan more items
            hasHandledScan = false
        }
        
        dialog.show()
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavc)
        
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.bthomec -> {
                    // Navigate to Customer page (Customer activity)
                    startActivity(Intent(this, Customer::class.java))
                    true
                }
                R.id.btcart -> {
                    // Navigate to Cart page
                    startActivity(Intent(this, Cart::class.java))
                    true
                }
                R.id.bthis -> {
                    // Navigate to Order History page
                    startActivity(Intent(this, OrderHistory::class.java))
                    true
                }
                R.id.btsettingc -> {
                    // Navigate to Settings page
                    startActivity(Intent(this, Setting_C::class.java))
                    true
                }
                else -> false
            }
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

    private fun addToCart(cartItem: CartItem): Boolean {
        if (currentUserId.isEmpty()) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return false
        }
        
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

    private fun getProductNameFromQR(uom: String, waterType: String): String {
        val waterTypeCode = if (waterType == "Alkaline") "A" else "M"
        return when {
            uom.contains("20L", ignoreCase = true) -> {
                if (uom.contains("Slim", ignoreCase = true)) {
                    "20L Slim $waterTypeCode x1"
                } else if (uom.contains("Round", ignoreCase = true)) {
                    "20L Round $waterTypeCode x1"
                } else {
                    "20L Slim $waterTypeCode x1" // Default to Slim
                }
            }
            uom.contains("10L", ignoreCase = true) -> {
                "10L Slim $waterTypeCode x1"
            }
            else -> {
                // Default to 20L Slim based on water type
                "20L Slim $waterTypeCode x1"
            }
        }
    }

    private fun getPriceFromQR(uom: String, waterType: String): Double {
        return when {
            uom.contains("20L", ignoreCase = true) -> {
                if (waterType == "Alkaline") 50.0 else 30.0
            }
            uom.contains("10L", ignoreCase = true) -> {
                if (waterType == "Alkaline") 25.0 else 15.0
            }
            else -> {
                // Default to 20L pricing
                if (waterType == "Alkaline") 50.0 else 30.0
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
