package com.adamson.fhydroscan

import android.Manifest
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
import com.adamson.fhydroscan.databinding.ActivityScanCBinding
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
    private var imageAnalyzer: ImageAnalysis? = null
    private var hasHandledScan = false

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
        val quantity = data["unit"] ?: "-"

        val dialogView = layoutInflater.inflate(R.layout.dialog_scanned_cus, null)

        val nameText = dialogView.findViewById<TextView>(R.id.nameText)
        val addressText = dialogView.findViewById<TextView>(R.id.addressText)
        val quantityText = dialogView.findViewById<TextView>(R.id.quantityText)
        val radioMineral = dialogView.findViewById<RadioButton>(R.id.radioMineral)
        val radioAlkaline = dialogView.findViewById<RadioButton>(R.id.radioAlkaline)
        val pickupDateButton = dialogView.findViewById<Button>(R.id.pickupDateButton)
        val deliveryDateButton = dialogView.findViewById<Button>(R.id.deliveryDateButton)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)
        val submitButton = dialogView.findViewById<Button>(R.id.submitButton)

        nameText.text = "Name: $name"
        addressText.text = "Address: $address"
        quantityText.text = "Quantity: $quantity"
        radioMineral.isChecked = true

        // Date pickers
        pickupDateButton.setOnClickListener { showDatePicker { date -> pickupDateButton.text = date } }
        deliveryDateButton.setOnClickListener { showDatePicker { date -> deliveryDateButton.text = date } }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        cancelButton.setOnClickListener { dialog.dismiss(); hasHandledScan = false }
        submitButton.setOnClickListener {
            val waterType = if (radioMineral.isChecked) "Mineral" else "Alkaline"
            val pickup = pickupDateButton.text.toString()
            val delivery = deliveryDateButton.text.toString()

            println("Order Submitted:")
            println("Name: $name")
            println("Address: $address")
            println("Quantity: $quantity")
            println("Water Type: $waterType")
            println("Pickup: $pickup")
            println("Delivery: $delivery")

            dialog.dismiss()
            hasHandledScan = false
        }

        dialog.show()
    }

    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            onDateSelected("${m + 1}/$d/$y")
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
