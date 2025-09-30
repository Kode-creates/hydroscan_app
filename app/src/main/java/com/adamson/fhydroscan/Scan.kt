package com.adamson.fhydroscan

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.adamson.fhydroscan.databinding.ActivityScanBinding
import com.adamson.fhydroscan.database.OrderDatabaseHelper
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class Scan : AppCompatActivity() {
    private lateinit var binding: ActivityScanBinding
    private lateinit var cameraExecutor: ExecutorService
    private var imageAnalyzer: ImageAnalysis? = null
    private lateinit var barcodeScanner: BarcodeScanner
    private var hasHandledScan: Boolean = false
    private lateinit var orderDatabaseHelper: OrderDatabaseHelper

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
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
        binding = ActivityScanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize database helper
        orderDatabaseHelper = OrderDatabaseHelper(this)

        val btgenqr = findViewById<android.widget.ImageView>(R.id.btgenqr)
        btgenqr.setOnClickListener {
            val intent = Intent(this, Generate::class.java)
            startActivity(intent)
        }

        // Initialize barcode scanner
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        barcodeScanner = BarcodeScanning.getClient(options)

        // Initialize camera executor
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Bottom navigation
        binding.bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.bthome -> {
                    startActivity(Intent(this, Dashboard::class.java))
                    true
                }
                R.id.btscan -> true
                R.id.btsetting -> {
                    startActivity(Intent(this, Settings::class.java))
                    true
                }
                else -> false
            }
        }

        // Request camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImageProxy(imageProxy)
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

    private fun processImageProxy(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            barcodeScanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        if (barcode.valueType == Barcode.TYPE_TEXT) {
                            val text = barcode.rawValue
                            if (text != null && !hasHandledScan) {
                                hasHandledScan = true
                                runOnUiThread {
                                    handleScannedPayload(text)
                                }
                            }
                        }
                    }
                }
                .addOnFailureListener { e -> e.printStackTrace() }
                .addOnCompleteListener { imageProxy.close() }
        } else {
            imageProxy.close()
        }
    }

    private fun handleScannedPayload(payload: String) {
        val data = parsePayload(payload)
        val name = data["name"] ?: "-"
        val address = data["address"] ?: "-"
        val uom = data["unit"] ?: "-"

        val dialogView = layoutInflater.inflate(R.layout.dialog_scanned_admin, null)

        val nameValue = dialogView.findViewById<TextView>(R.id.nameValue)
        val addressValue = dialogView.findViewById<TextView>(R.id.addressValue)
        val uomValue = dialogView.findViewById<TextView>(R.id.uomValue)
        val quantityDisplay = dialogView.findViewById<TextView>(R.id.quantityDisplay)
        val minusButton = dialogView.findViewById<Button>(R.id.minusButton)
        val plusButton = dialogView.findViewById<Button>(R.id.plusButton)
        val radioMineral = dialogView.findViewById<RadioButton>(R.id.radioMineral)
        val radioAlkaline = dialogView.findViewById<RadioButton>(R.id.radioAlkaline)
        val radioPaid = dialogView.findViewById<RadioButton>(R.id.radioPaid)
        val radioUnpaid = dialogView.findViewById<RadioButton>(R.id.radioUnpaid)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)
        val submitButton = dialogView.findViewById<Button>(R.id.submitButton)

        nameValue.text = name
        addressValue.text = address
        uomValue.text = uom
        radioMineral.isChecked = true
        radioPaid.isChecked = true // Default to paid

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
            quantity++
            quantityDisplay.text = quantity.toString()
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        cancelButton.setOnClickListener {
            dialog.dismiss()
            hasHandledScan = false
        }

        submitButton.setOnClickListener {
            val waterType = if (radioMineral.isChecked) "Mineral" else "Alkaline"
            val isPaid = radioPaid.isChecked
            
            // Save to database
            val success = orderDatabaseHelper.addOrderItem(
                customerName = name,
                customerAddress = address,
                waterType = waterType,
                uom = uom,
                quantity = quantity,
                isPaid = isPaid
            )
            
            if (success) {
                // Update inventory for gallons
                val itemKey = orderDatabaseHelper.getItemKeyForInventory(waterType, uom)
                if (itemKey != null) {
                    orderDatabaseHelper.processPurchaseAndUpdateInventory(itemKey, quantity, this)
                }
                
                Toast.makeText(this, "Order added successfully", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                hasHandledScan = false
            } else {
                Toast.makeText(this, "Failed to add order", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun parsePayload(payload: String): Map<String, String> {
        return try {
            payload.split(';')
                .mapNotNull { part ->
                    val idx = part.indexOf('=')
                    if (idx > 0 && idx < part.length - 1) {
                        val key = part.substring(0, idx).trim()
                        val value = part.substring(idx + 1).trim()
                        key to value
                    } else null
                }
                .toMap()
        } catch (_: Exception) {
            emptyMap()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
