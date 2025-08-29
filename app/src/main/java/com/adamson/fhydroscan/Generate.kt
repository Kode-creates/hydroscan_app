package com.adamson.fhydroscan

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.Toast
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class Generate : AppCompatActivity() {
    private lateinit var nameInput: EditText
    private lateinit var addressInput: EditText
    private lateinit var unitSpinner: Spinner
    private lateinit var waterTypeGroup: RadioGroup
    private lateinit var generateButton: Button
    private var lastGeneratedBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_generate)

        // Initialize views
        nameInput = findViewById(R.id.nameLayout)
        addressInput = findViewById(R.id.addressInput)
        unitSpinner = findViewById(R.id.unitSpinner)
        waterTypeGroup = findViewById(R.id.waterTypeGroup)
        generateButton = findViewById(R.id.generateButton)

        // Set up back button
        val backButton = findViewById<ImageButton>(R.id.backButton)
        backButton.setOnClickListener {
            finish()
        }

        // Set up unit spinner
        val units = arrayOf("20L", "10L", "7L", "6L")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, units)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        unitSpinner.adapter = adapter

        // Set up generate button
        generateButton.setOnClickListener {
            generateQRCode()
        }

        // Set up bottom navigation
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
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

    private fun generateQRCode() {
        val name = nameInput.text.toString()
        val address = addressInput.text.toString()
        val unit = unitSpinner.selectedItem.toString()
        val waterType = when (waterTypeGroup.checkedRadioButtonId) {
            R.id.mineralRadio -> "M"
            R.id.alkalineRadio -> "A"
            else -> ""
        }

        // Validate inputs (all required except water type)
        if (name.isEmpty() || address.isEmpty() || unit.isBlank()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Build payload
        val payload = "name=$name;address=$address;unit=$unit;type=$waterType"

        // Generate QR Bitmap
        try {
            val hints = hashMapOf<com.google.zxing.EncodeHintType, Any>(
                com.google.zxing.EncodeHintType.CHARACTER_SET to "UTF-8",
                com.google.zxing.EncodeHintType.MARGIN to 1
            )
            val bitMatrix = com.google.zxing.MultiFormatWriter().encode(
                payload,
                com.google.zxing.BarcodeFormat.QR_CODE,
                600,
                600,
                hints
            )
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bmp.setPixel(x, y, if (bitMatrix.get(x, y)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
                }
            }
            lastGeneratedBitmap = bmp
            // Show QR Code Dialog with bitmap
            showQRCodeDialog(bmp)
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to generate QR", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showQRCodeDialog(qrBitmap: Bitmap) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_qr_generated, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        // Place the generated QR in the ImageView
        dialogView.findViewById<ImageView>(R.id.qrCodeImage)?.setImageBitmap(qrBitmap)

        // Set the user's name under the QR
        val name = nameInput.text.toString()
        dialogView.findViewById<TextView>(R.id.qrNameText)?.text = name

        // Set up save button
        val saveButton = dialogView.findViewById<Button>(R.id.saveButton)
        saveButton.setOnClickListener {
            // Show "Image saved" alert below the button
            Toast.makeText(this, "Image saved", Toast.LENGTH_SHORT).show()
        }

        // Set up check icon button
        val checkIconButton = dialogView.findViewById<ImageButton>(R.id.checkIconButton)
        checkIconButton.setOnClickListener {
            // Show confirmation dialog
            showAddOrderConfirmation()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showAddOrderConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Add new QR as an order today?")
            .setMessage("Would you like to add this QR code as a new order?")
            .setPositiveButton("Yes") { _, _ ->
                // TODO: Add logic to add as order
                Toast.makeText(this, "Added as new order", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("No") { _, _ ->
                // Do nothing or handle as needed
            }
            .setCancelable(false)
            .show()
    }
}