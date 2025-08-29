package com.adamson.fhydroscan

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

    // Current quantities
    private val quantities = mutableMapOf<String, Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inventory)

        // Initialize views
        initializeViews()
        
        // Initialize quantities
        initializeQuantities()

        // Set up click listeners for add buttons
        setupAddButtons()

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

    private fun initializeQuantities() {
        // Initialize Gallon quantities
        quantities["slim_gallon"] = 0
        quantities["round_gallon"] = 0

        // Initialize Seal quantities
        quantities["small_cap_seal"] = 0
        quantities["big_cap_seal"] = 0
        quantities["faucet_seal"] = 0
        quantities["umbrella_seal"] = 0

        // Initialize Cap Cover quantities
        quantities["small_cap_cover"] = 0
        quantities["big_cap_cover"] = 0
        quantities["full_round_cover"] = 0
        quantities["non_leak_cover"] = 0

        updateAllQuantityDisplays()
    }

    private fun setupAddButtons() {
        // Set up Gallon add button
        findViewById<View>(R.id.addGallon).setOnClickListener {
            showAddDialog("Gallon", arrayOf("Slim Gallon", "Round Gallon"))
        }

        // Set up Seal add button
        findViewById<View>(R.id.addSeal).setOnClickListener {
            showAddDialog("Seal", arrayOf("Small Cap Seal", "Big Cap Seal", "Faucet Seal", "Umbrella Seal"))
        }

        // Set up Cap Cover add button
        findViewById<View>(R.id.addCapCover).setOnClickListener {
            showAddDialog("Cap Cover", arrayOf("Small Cap Cover", "Big Cap Cover", "Full Round Cover", "Non Leak Cover"))
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

    private fun showAddDialog(category: String, items: Array<String>) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_add_item)
        dialog.setTitle(getString(R.string.add_item_format, category))

        // Set up the spinner with item types
        val spinner = dialog.findViewById<Spinner>(R.id.typeSpinner)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, items)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        // Set up quantity input
        val quantityInput = dialog.findViewById<EditText>(R.id.quantityInput)

        // Set up buttons
        dialog.findViewById<Button>(R.id.cancelButton).setOnClickListener {
            dialog.dismiss()
        }

        dialog.findViewById<Button>(R.id.addButton).setOnClickListener {
            val quantity = quantityInput.text.toString().toIntOrNull()
            if (quantity == null || quantity < 0) {
                Toast.makeText(this, getString(R.string.please_enter_quantity), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedItem = spinner.selectedItem.toString()
            val key = selectedItem.lowercase().replace(" ", "_")
            quantities[key] = (quantities[key] ?: 0) + quantity
            updateAllQuantityDisplays()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun updateAllQuantityDisplays() {
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
}