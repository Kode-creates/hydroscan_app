package com.adamson.fhydroscan

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.adamson.fhydroscan.database.InventoryDatabaseHelper
import com.google.android.material.bottomnavigation.BottomNavigationView

class Inventory : AppCompatActivity() {
    // Quantity TextViews for Gallons
    private lateinit var qtySlimGallon: TextView
    private lateinit var qtyRoundGallon: TextView

    // Quantity TextViews for Seals
    private lateinit var qtySmallCapSeal: TextView
    private lateinit var qtyBigCapSeal: TextView
    private lateinit var qtyFaucetSeal: TextView
    private lateinit var qtyUmbrellaSeal: TextView

    // Quantity TextViews for Cap Covers
    private lateinit var qtySmallCapCover: TextView
    private lateinit var qtyBigCapCover: TextView
    private lateinit var qtyFullRoundCover: TextView
    private lateinit var qtyNonLeakCover: TextView

    // Database helper
    private lateinit var inventoryDatabaseHelper: InventoryDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inventory)

        // Initialize database helper
        inventoryDatabaseHelper = InventoryDatabaseHelper(this)

        // Initialize views
        initializeViews()
        
        // Load quantities from database
        loadQuantitiesFromDatabase()

        // Set up click listeners for inventory images
        setupImageClickListeners()

        // Set up bottom navigation
        setupBottomNavigation()
    }

    private fun initializeViews() {
        // Initialize Gallon TextViews
        qtySlimGallon = findViewById(R.id.qtySlimGallon)
        qtyRoundGallon = findViewById(R.id.qtyRoundGallon)

        // Initialize Seal TextViews
        qtySmallCapSeal = findViewById(R.id.qtySmallCapSeal)
        qtyBigCapSeal = findViewById(R.id.qtyBigCapSeal)
        qtyFaucetSeal = findViewById(R.id.qtyFaucetSeal)
        qtyUmbrellaSeal = findViewById(R.id.qtyUmbrellaSeal)

        // Initialize Cap Cover TextViews
        qtySmallCapCover = findViewById(R.id.qtySmallCapCover)
        qtyBigCapCover = findViewById(R.id.qtyBigCapCover)
        qtyFullRoundCover = findViewById(R.id.qtyFullRoundCover)
        qtyNonLeakCover = findViewById(R.id.qtyNonLeakCover)
    }

    private fun loadQuantitiesFromDatabase() {
        // Load all quantities from database
        val quantities = inventoryDatabaseHelper.getAllInventoryItems()
        updateAllQuantityDisplays(quantities)
    }

    private fun setupImageClickListeners() {
        // Set up Gallon image click listeners
        findViewById<ImageView>(R.id.slimGallonImage).setOnClickListener {
            showEditQuantityDialog("Slim Gallon", "slim_gallon")
        }
        
        findViewById<ImageView>(R.id.roundGallonImage).setOnClickListener {
            showEditQuantityDialog("Round Gallon", "round_gallon")
        }

        // Set up Seal image click listeners
        findViewById<ImageView>(R.id.smallCapSealImage).setOnClickListener {
            showEditQuantityDialog("Small Cap Seal", "small_cap_seal")
        }
        
        findViewById<ImageView>(R.id.bigCapSealImage).setOnClickListener {
            showEditQuantityDialog("Big Cap Seal", "big_cap_seal")
        }
        
        findViewById<ImageView>(R.id.faucetSealImage).setOnClickListener {
            showEditQuantityDialog("Faucet Seal", "faucet_seal")
        }
        
        findViewById<ImageView>(R.id.umbrellaSealImage).setOnClickListener {
            showEditQuantityDialog("Umbrella Seal", "umbrella_seal")
        }

        // Set up Cap Cover image click listeners
        findViewById<ImageView>(R.id.smallCapCoverImage).setOnClickListener {
            showEditQuantityDialog("Small Cap Cover", "small_cap_cover")
        }
        
        findViewById<ImageView>(R.id.bigCapCoverImage).setOnClickListener {
            showEditQuantityDialog("Big Cap Cover", "big_cap_cover")
        }
        
        findViewById<ImageView>(R.id.fullRoundCoverImage).setOnClickListener {
            showEditQuantityDialog("Full Round Cover", "full_round_cover")
        }
        
        findViewById<ImageView>(R.id.nonLeakCoverImage).setOnClickListener {
            showEditQuantityDialog("Non Leak Cover", "non_leak_cover")
        }
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
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

    private fun showEditQuantityDialog(itemName: String, itemKey: String) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_edit_quantity)

        // Get current quantity from database
        val currentQuantity = inventoryDatabaseHelper.getInventoryQuantity(itemKey)

        // Set up views
        val itemNameText = dialog.findViewById<TextView>(R.id.itemNameText)
        val currentQuantityText = dialog.findViewById<TextView>(R.id.currentQuantityText)
        val quantityInput = dialog.findViewById<EditText>(R.id.quantityInput)
        val saveButton = dialog.findViewById<Button>(R.id.saveButton)
        val cancelButton = dialog.findViewById<Button>(R.id.cancelButton)

        // Set current values
        itemNameText.text = itemName
        currentQuantityText.text = currentQuantity.toString()
        quantityInput.setText(currentQuantity.toString())

        // Set up buttons
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        saveButton.setOnClickListener {
            val newQuantity = quantityInput.text.toString().toIntOrNull()
            if (newQuantity == null || newQuantity < 0) {
                Toast.makeText(this, "Please enter a valid quantity", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Update quantity in database
            val success = inventoryDatabaseHelper.updateInventoryQuantity(itemKey, newQuantity)
            if (success) {
                Toast.makeText(this, "Updated $itemName quantity to $newQuantity", Toast.LENGTH_SHORT).show()
                loadQuantitiesFromDatabase() // Reload from database
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Failed to update quantity", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun updateAllQuantityDisplays(quantities: Map<String, Int> = inventoryDatabaseHelper.getAllInventoryItems()) {
        // Update Gallon quantities
        qtySlimGallon.text = getString(R.string.qty_format, quantities["slim_gallon"] ?: 0)
        qtyRoundGallon.text = getString(R.string.qty_format, quantities["round_gallon"] ?: 0)

        // Update Seal quantities
        qtySmallCapSeal.text = getString(R.string.qty_format, quantities["small_cap_seal"] ?: 0)
        qtyBigCapSeal.text = getString(R.string.qty_format, quantities["big_cap_seal"] ?: 0)
        qtyFaucetSeal.text = getString(R.string.qty_format, quantities["faucet_seal"] ?: 0)
        qtyUmbrellaSeal.text = getString(R.string.qty_format, quantities["umbrella_seal"] ?: 0)

        // Update Cap Cover quantities
        qtySmallCapCover.text = getString(R.string.qty_format, quantities["small_cap_cover"] ?: 0)
        qtyBigCapCover.text = getString(R.string.qty_format, quantities["big_cap_cover"] ?: 0)
        qtyFullRoundCover.text = getString(R.string.qty_format, quantities["full_round_cover"] ?: 0)
        qtyNonLeakCover.text = getString(R.string.qty_format, quantities["non_leak_cover"] ?: 0)
    }

    override fun onResume() {
        super.onResume()
        // Reload quantities when returning to the activity
        loadQuantitiesFromDatabase()
    }
}