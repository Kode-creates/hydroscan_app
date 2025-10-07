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
import android.widget.AutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import android.text.InputFilter
import android.widget.Toast
import android.widget.TextView
import android.app.Dialog
import android.widget.RadioButton
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.adamson.fhydroscan.database.OrderDatabaseHelper
import com.adamson.fhydroscan.utils.PricingUtils

class Generate : AppCompatActivity() {
    private lateinit var nameInput: EditText
    private lateinit var addressInput: EditText
    private lateinit var unitSpinner: AutoCompleteTextView
    private lateinit var waterTypeGroup: RadioGroup
    private lateinit var generateButton: Button
    private var lastGeneratedBitmap: Bitmap? = null
    private lateinit var orderDatabaseHelper: OrderDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_generate)

        // Initialize database helper
        orderDatabaseHelper = OrderDatabaseHelper(this)

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

        // Set up unit dropdown
        val units = arrayOf("20L Slim", "10L Slim", "20L Round")
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, units)
        unitSpinner.setAdapter(adapter)
        unitSpinner.setText(units[0], false) // Set default selection

        // Set up generate button
        generateButton.setOnClickListener {
            generateQRCode()
        }

        // Set up input validation
        setupInputValidation()

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

    private fun setupInputValidation() {
        // Name input filter: 2-50 characters, no numbers
        nameInput.filters = arrayOf(
            InputFilter.LengthFilter(50),
            InputFilter { source, start, end, dest, dstart, dend ->
                // Allow only letters, spaces, and common name characters (no numbers)
                val regex = Regex("[a-zA-Z\\s\\-'.]")
                for (i in start until end) {
                    if (!regex.matches(source[i].toString())) {
                        return@InputFilter ""
                    }
                }
                null
            }
        )
        
        // Address input filter: 5-100 characters
        addressInput.filters = arrayOf(InputFilter.LengthFilter(100))
        
        // Name text watcher for real-time feedback
        nameInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val text = s.toString()
                when {
                    text.length < 2 && text.isNotEmpty() -> {
                        nameInput.error = "Name must be at least 2 characters"
                    }
                    text.length > 50 -> {
                        nameInput.error = "Name must be 50 characters or less"
                    }
                    text.any { it.isDigit() } -> {
                        nameInput.error = "Name must not contain numbers"
                    }
                    else -> {
                        nameInput.error = null
                    }
                }
            }
        })
        
        // Address text watcher for real-time feedback
        addressInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val text = s.toString()
                when {
                    text.length < 5 && text.isNotEmpty() -> {
                        addressInput.error = "Address must be at least 5 characters"
                    }
                    text.length > 100 -> {
                        addressInput.error = "Address must be 100 characters or less"
                    }
                    else -> {
                        addressInput.error = null
                    }
                }
            }
        })
    }

    private fun generateQRCode() {
        val name = nameInput.text.toString().trim()
        val address = addressInput.text.toString().trim()
        val unit = unitSpinner.text.toString()
        val waterType = when (waterTypeGroup.checkedRadioButtonId) {
            R.id.mineralRadio -> "M"
            R.id.alkalineRadio -> "A"
            else -> ""
        }

        // Validate inputs with detailed error messages
        when {
            name.isEmpty() -> {
                Toast.makeText(this, "Please enter a name", Toast.LENGTH_SHORT).show()
                return
            }
            name.length < 2 -> {
                Toast.makeText(this, "Name must be at least 2 characters", Toast.LENGTH_SHORT).show()
                return
            }
            name.length > 50 -> {
                Toast.makeText(this, "Name must be 50 characters or less", Toast.LENGTH_SHORT).show()
                return
            }
            name.any { it.isDigit() } -> {
                Toast.makeText(this, "Name must not contain numbers", Toast.LENGTH_SHORT).show()
                return
            }
            address.isEmpty() -> {
                Toast.makeText(this, "Please enter an address", Toast.LENGTH_SHORT).show()
                return
            }
            address.length < 5 -> {
                Toast.makeText(this, "Address must be at least 5 characters", Toast.LENGTH_SHORT).show()
                return
            }
            address.length > 100 -> {
                Toast.makeText(this, "Address must be 100 characters or less", Toast.LENGTH_SHORT).show()
                return
            }
            unit.isBlank() -> {
                Toast.makeText(this, "Please select a gallon type", Toast.LENGTH_SHORT).show()
                return
            }
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
        // Get the current form data
        val name = nameInput.text.toString()
        val address = addressInput.text.toString()
        val unit = unitSpinner.text.toString()
        val waterType = when (waterTypeGroup.checkedRadioButtonId) {
            R.id.mineralRadio -> "Mineral"
            R.id.alkalineRadio -> "Alkaline"
            else -> "Mineral"
        }
        
        // Show quantity dialog
        showQuantityDialog(name, address, unit, waterType)
    }
    
    private fun showQuantityDialog(name: String, address: String, unit: String, waterType: String) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_qr_order_quantity)
        
        // Initialize views
        val customerNameDisplay = dialog.findViewById<TextView>(R.id.customerNameDisplay)
        val customerAddressDisplay = dialog.findViewById<TextView>(R.id.customerAddressDisplay)
        val productUomDisplay = dialog.findViewById<TextView>(R.id.productUomDisplay)
        val productWaterTypeDisplay = dialog.findViewById<TextView>(R.id.productWaterTypeDisplay)
        val minusButton = dialog.findViewById<Button>(R.id.minusButton)
        val plusButton = dialog.findViewById<Button>(R.id.plusButton)
        val quantityDisplay = dialog.findViewById<TextView>(R.id.quantityDisplay)
        val paymentStatusGroup = dialog.findViewById<RadioGroup>(R.id.paymentStatusGroup)
        val paidRadio = dialog.findViewById<RadioButton>(R.id.paidRadio)
        val unpaidRadio = dialog.findViewById<RadioButton>(R.id.unpaidRadio)
        val addButton = dialog.findViewById<Button>(R.id.addButton)
        val cancelButton = dialog.findViewById<Button>(R.id.cancelButton)
        
        // Set customer and product info
        customerNameDisplay.text = "Name: $name"
        customerAddressDisplay.text = "Address: $address"
        productUomDisplay.text = "UOM: $unit"
        productWaterTypeDisplay.text = "Water Type: $waterType"
        
        // Set default payment status
        paidRadio.isChecked = true
        
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
        
        // Add button
        addButton.setOnClickListener {
            val isPaid = paidRadio.isChecked
            addOrderToDatabase(name, address, unit, waterType, quantity, isPaid)
            dialog.dismiss()
        }
        
        // Cancel button
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    private fun addOrderToDatabase(name: String, address: String, unit: String, waterType: String, quantity: Int, isPaid: Boolean) {
        try {
            // Create product string in the format expected by the database
            val waterTypeCode = if (waterType == "Alkaline") "A" else "M"
            val product = "$unit $waterTypeCode x$quantity"
            
            // Calculate price (using the same logic as Today.kt)
            val price = calculatePrice(waterTypeCode, unit, quantity)
            
            // Create CustomerOrder object
            val customerOrder = CustomerOrder(
                name = name,
                address = address,
                product = product,
                total = price,
                isPaid = isPaid,
                status = "Pending"
            )
            
            // Add to database using the same method as Today page
            val success = orderDatabaseHelper.addCustomerOrder(customerOrder)
            
            if (success) {
                Toast.makeText(this, "Order added to Today's orders successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to add order to database", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error adding order: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }
    
    private fun calculatePrice(waterTypeCode: String, uom: String, quantity: Int): Double {
        // Get price from central pricing database
        val waterType = if (waterTypeCode == "A") "Alkaline" else "Mineral"
        val size = when (uom) {
            "20L Slim", "20L Round" -> "20L"
            "10L Slim" -> "10L"
            else -> "20L"
        }
        
        val basePrice = PricingUtils.getWaterPrice(this, waterType, size)
        return basePrice * quantity
    }
}