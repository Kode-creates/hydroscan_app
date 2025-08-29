package com.adamson.fhydroscan

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.adamson.fhydroscan.databinding.ActivityScanEBinding
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScanE : AppCompatActivity() {
    private lateinit var binding: ActivityScanEBinding
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private lateinit var barcodeScanner: BarcodeScanner
    private var hasHandledScan: Boolean = false

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
        binding = ActivityScanEBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize barcode scanner
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        barcodeScanner = BarcodeScanning.getClient(options)

        // Initialize camera executor
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Set up bottom navigation
        binding.bottomNave.selectedItemId = R.id.btscan2 // Set scan as selected

        binding.bottomNave.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.bthomee -> {
                    startActivity(Intent(this, DashboardE::class.java))
                    true
                }
                R.id.btscan2 -> {
                    // Already on scan, no need to do anything
                    true
                }
                R.id.btsetting2 -> {
                    startActivity(Intent(this, SettingE::class.java))
                    true
                }
                else -> false
            }
        }

        // Request camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // Set up Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            // Set up image analyzer
            imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImageProxy(imageProxy)
                    }
                }

            try {
                // Unbind any bound use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
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
                        when (barcode.valueType) {
                            Barcode.TYPE_TEXT -> {
                                val text = barcode.rawValue
                                if (text != null) {
                                    if (!hasHandledScan) {
                                        hasHandledScan = true
                                        runOnUiThread { handleScannedPayload(text) }
                                    }
                                }
                            }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    e.printStackTrace()
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    private fun handleScannedPayload(payload: String) {
        val data = parsePayload(payload)
        val pretty = if (data.isNotEmpty()) {
            "Name: ${data["name"] ?: "-"}\n" +
            "Address: ${data["address"] ?: "-"}\n" +
            "Unit: ${data["unit"] ?: "-"}\n" +
            "Type: ${data["type"] ?: "-"}"
        } else payload

        AlertDialog.Builder(this)
            .setTitle("Scanned QR Data")
            .setMessage(pretty)
            .setPositiveButton("OK") { d, _ ->
                d.dismiss()
                hasHandledScan = false
            }
            .setCancelable(false)
            .show()
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
        } catch (_: Exception) { emptyMap() }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}