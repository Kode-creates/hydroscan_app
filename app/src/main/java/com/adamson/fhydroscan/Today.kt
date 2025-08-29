package com.adamson.fhydroscan

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

import com.google.android.material.floatingactionbutton.FloatingActionButton

class Today : AppCompatActivity() {
    data class CustomerOrder(
        val name: String,
        val waterType: String,
        val uom: String,
        val quantity: Int,
        val total: Double,
        var isPaid: Boolean
    )

    private val customerOrders = mutableListOf<CustomerOrder>()
    private lateinit var ordersList: LinearLayout
    private lateinit var totalQtyText: TextView
    private lateinit var totalUomText: TextView
    private lateinit var totalRevenueText: TextView
    private lateinit var completedCountText: TextView

    // Define UOM options and prices
    private val uomOptions = listOf("20L", "10L", "7L", "6L")
    private val waterTypes = mapOf(
        "Alkaline" to "A",
        "Mineral" to "M"
    )
    private val prices = mapOf(
        "Alkaline" to mapOf(
            "20L" to 50.0,
            "10L" to 25.0
        ),
        "Mineral" to mapOf(
            "20L" to 30.0,
            "10L" to 15.0,
            "7L" to 11.0,
            "6L" to 10.0
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_today)

            // Enable the home button in the action bar
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setDisplayShowHomeEnabled(true)

            // Initialize views
            ordersList = findViewById(R.id.ordersList)
            totalQtyText = findViewById(R.id.totalQty)
            totalUomText = findViewById(R.id.totalUom)
            totalRevenueText = findViewById(R.id.totalRevenue)
            completedCountText = findViewById(R.id.completedCount)

            // Add sample data
            addCustomerOrder(CustomerOrder(
                name = "Michelle",
                waterType = "A",
                uom = "20L",
                quantity = 2,
                total = 100.0,
                isPaid = true
            ))

            addCustomerOrder(CustomerOrder(
                name = "Mark",
                waterType = "A",
                uom = "20L",
                quantity = 2,
                total = 100.0,
                isPaid = true
            ))

            addCustomerOrder(CustomerOrder(
                name = "Kel",
                waterType = "A",
                uom = "20L",
                quantity = 1,
                total = 50.0,
                isPaid = false
            ))

            // Set up add order button
            findViewById<FloatingActionButton>(R.id.addOrderButton).setOnClickListener {
                showAddOrderDialog()
            }



            updateSummary()
        } catch (e: Exception) {
            Toast.makeText(this, "Error initializing Today page: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
            finish()
        }
    }

    // Add override for home button click
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun addCustomerOrder(order: CustomerOrder) {
        try {
            customerOrders.add(order)
            addOrderToList(order)
            updateSummary()
        } catch (e: Exception) {
            Toast.makeText(this, "Error adding order: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun addOrderToList(order: CustomerOrder) {
        val orderView = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(16, 8, 16, 8)
        }

        // Create TextViews with appropriate weights
        val views = listOf(
            Pair(order.name, 2f),
            Pair(order.uom, 1f),
            Pair(order.waterType, 1f),
            Pair(order.quantity.toString(), 1f),
            Pair(String.format("₱%.2f", order.total), 1f),
            Pair(if (order.isPaid) "Paid" else "Unpaid", 1.5f)
        )

        views.forEach { (text, weight) ->
            TextView(this).apply {
                this.text = text
                setTextColor(resources.getColor(
                    when {
                        text == "Paid" -> android.R.color.holo_green_dark
                        text == "Unpaid" -> android.R.color.holo_orange_dark
                        else -> R.color.black
                    }, null
                ))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight)
                setPadding(8, 8, 8, 8)
            }.also { orderView.addView(it) }
        }

        ordersList.addView(orderView)
    }

    private fun updateSummary() {
        val totalQty = customerOrders.sumOf { it.quantity }
        val totalRevenue = customerOrders.sumOf { it.total }
        val completedCount = customerOrders.count { it.isPaid }
        val mainUom = customerOrders.groupBy { it.uom }
            .maxByOrNull { it.value.size }?.key ?: "20L"

        totalQtyText.text = totalQty.toString()
        totalUomText.text = mainUom
        totalRevenueText.text = String.format("₱%.2f", totalRevenue)
        completedCountText.text = "$completedCount/${customerOrders.size}"
    }

    private fun showAddOrderDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_add_order)

        // Initialize spinners
        val uomSpinner = dialog.findViewById<Spinner>(R.id.uomSpinner)
        val waterTypeSpinner = dialog.findViewById<Spinner>(R.id.waterTypeSpinner)

        // Set up UOM spinner
        ArrayAdapter(this, android.R.layout.simple_spinner_item, uomOptions).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            uomSpinner.adapter = adapter
        }

        // Set up water type spinner with full names
        ArrayAdapter(this, android.R.layout.simple_spinner_item, waterTypes.keys.toList()).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            waterTypeSpinner.adapter = adapter
        }

        // Update available UOM options based on water type selection
        waterTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedWaterType = waterTypes.keys.toList()[position]
                val availableUOMs = when (selectedWaterType) {
                    "Alkaline" -> listOf("20L", "10L")
                    "Mineral" -> uomOptions
                    else -> emptyList()
                }
                
                ArrayAdapter(this@Today, android.R.layout.simple_spinner_item, availableUOMs).also { adapter ->
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    uomSpinner.adapter = adapter
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Set up buttons
        dialog.findViewById<Button>(R.id.addButton).setOnClickListener {
            try {
                val name = dialog.findViewById<EditText>(R.id.nameInput).text.toString()
                val quantity = dialog.findViewById<EditText>(R.id.quantityInput).text.toString().toIntOrNull()
                val isPaid = dialog.findViewById<RadioButton>(R.id.paidRadio).isChecked
                val selectedWaterType = waterTypeSpinner.selectedItem.toString()
                val selectedUOM = uomSpinner.selectedItem.toString()

                if (name.isNotBlank() && quantity != null) {
                    val waterTypeCode = waterTypes[selectedWaterType] ?: "A"
                    val total = calculatePrice(waterTypeCode, selectedUOM, quantity)
                    
                    val order = CustomerOrder(
                        name = name,
                        waterType = waterTypeCode,
                        uom = selectedUOM,
                        quantity = quantity,
                        total = total,
                        isPaid = isPaid
                    )
                    addCustomerOrder(order)
                    dialog.dismiss()
                } else {
                    Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Error adding order: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }

        dialog.findViewById<Button>(R.id.cancelButton).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun calculatePrice(waterType: String, uom: String, quantity: Int): Double {
        val fullWaterType = waterTypes.entries.find { it.value == waterType }?.key ?: return 0.0
        return (prices[fullWaterType]?.get(uom) ?: 0.0) * quantity
    }
}