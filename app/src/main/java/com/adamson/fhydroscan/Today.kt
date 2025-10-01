package com.adamson.fhydroscan

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.adamson.fhydroscan.data.Order
import com.adamson.fhydroscan.data.OrderItem
import com.adamson.fhydroscan.database.OrderDatabaseHelper
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout

class Today : AppCompatActivity() {

    private val customerOrders = mutableListOf<CustomerOrder>()
    private val filteredOrders = mutableListOf<CustomerOrder>()
    private lateinit var ordersTable: TableLayout
    private lateinit var quickTotalQtyText: TextView
    private lateinit var quickRevenueText: TextView
    private lateinit var orderDatabaseHelper: OrderDatabaseHelper
    private lateinit var summaryHandle: LinearLayout
    private var currentTab = 0 // 0: All Orders, 1: Pendings, 2: Delivered

    // Define UOM options and prices
    private val uomOptions = listOf("20L Slim", "10L Slim", "20L Round")
    private val waterTypes = mapOf(
        "Alkaline" to "A",
        "Mineral" to "M"
    )
    private val prices = mapOf(
        "Alkaline" to mapOf(
            "20L Slim" to 50.0,
            "10L Slim" to 25.0
        ),
        "Mineral" to mapOf(
            "20L Slim" to 30.0,
            "10L Slim" to 15.0,
            "20L Round" to 30.0
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_today)

            // Enable the home button in the action bar
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setDisplayShowHomeEnabled(true)

            // Initialize database helper
            orderDatabaseHelper = OrderDatabaseHelper(this)

            // Initialize views
            ordersTable = findViewById(R.id.ordersTable)
            quickTotalQtyText = findViewById(R.id.quickTotalQty)
            quickRevenueText = findViewById(R.id.quickRevenue)
            summaryHandle = findViewById(R.id.summaryHandle)
            
            // Debug TableLayout setup
            println("DEBUG: TableLayout setup complete")

            // Set up summary bottom sheet
            setupSummaryBottomSheet()

            // Load today's orders from database FIRST
            loadTodaysOrders()

            // Set up tab layout
            val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
            tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    currentTab = tab?.position ?: 0
                    println("DEBUG: Tab selected - position: $currentTab")
                    loadOrdersForCurrentTab()
                    updateSummaryVisibility()
                }
                override fun onTabUnselected(tab: TabLayout.Tab?) {}
                override fun onTabReselected(tab: TabLayout.Tab?) {}
            })
            
            // Update tab labels with counts
            updateTabLabels()

            // Set up add order button
            findViewById<FloatingActionButton>(R.id.addOrderButton).setOnClickListener {
                showAddOrderDialog()
            }

            updateSummary()
            
            // Ensure the first tab is selected and data is loaded
            tabLayout.selectTab(tabLayout.getTabAt(0))
            currentTab = 0
            loadOrdersForCurrentTab()
            
            // Test TableLayout visibility after a short delay to ensure layout is complete
            ordersTable.post {
                testTableLayoutVisibility()
            }
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

    private fun loadTodaysOrders() {
        try {
            // Clear existing orders
            customerOrders.clear()
            
            // Load orders from database
            val orders = orderDatabaseHelper.getTodaysCustomerOrders()
            customerOrders.addAll(orders)
            
            println("DEBUG: loadTodaysOrders - Loaded ${customerOrders.size} orders from database")
            
            // Load orders for current tab
            loadOrdersForCurrentTab()
        } catch (e: Exception) {
            Toast.makeText(this, "Error loading today's orders: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun loadOrdersForCurrentTab() {
        println("DEBUG: loadOrdersForCurrentTab - currentTab: $currentTab, customerOrders.size: ${customerOrders.size}")
        
        val newFilteredOrders = when (currentTab) {
            0 -> customerOrders // All Orders
            1 -> customerOrders.filter { it.status == "Pending" } // Pendings
            2 -> customerOrders.filter { it.status == "Delivered" } // Delivered
            else -> customerOrders
        }
        
        println("DEBUG: loadOrdersForCurrentTab - newFilteredOrders.size: ${newFilteredOrders.size}")
        
        filteredOrders.clear()
        filteredOrders.addAll(newFilteredOrders)
        
        println("DEBUG: loadOrdersForCurrentTab - filteredOrders.size after addAll: ${filteredOrders.size}")
        
        // Populate the TableLayout with filtered orders
        populateTableLayout()
        
        println("DEBUG: Loaded ${filteredOrders.size} orders for tab $currentTab")
        filteredOrders.forEach { order ->
            println("DEBUG: Order - ${order.name}: ${order.product} - ${order.status}")
        }
    }

    private fun populateTableLayout() {
        // Clear existing rows
        ordersTable.removeAllViews()
        
        // Add each order as a table row
        filteredOrders.forEachIndexed { index, order ->
            val row = createTableRow(order, index)
            ordersTable.addView(row)
        }
    }
    
    private fun createTableRow(order: CustomerOrder, position: Int): TableRow {
        val row = TableRow(this)
        row.setPadding(8, 12, 8, 12)
        
        // Set alternating row background
        if (position % 2 == 0) {
            row.setBackgroundColor(0xFFFFFFFF.toInt())
        } else {
            row.setBackgroundColor(0xFFF8F8F8.toInt())
        }
        
        // Customer Name
        val nameText = TextView(this).apply {
            text = order.name
            textSize = 11f
            setTextColor(0xFF000000.toInt())
            setPadding(8, 0, 8, 0)
            layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 2f)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        
        // Product - Multi-line support with bullet points
        val productText = TextView(this).apply {
            // Split products by comma and create multi-line display
            val products = order.product.split(", ").filter { it.isNotBlank() }
            val productLines = products.mapIndexed { index, product ->
                val bullet = if (index == 0) "•" else "  •"
                "$bullet $product"
            }
            text = productLines.joinToString("\n")
            textSize = 10f
            setTextColor(when {
                order.product.contains(" A ") -> 0xFFF44336.toInt() // Red for Alkaline
                order.product.contains(" M ") -> 0xFF2196F3.toInt() // Blue for Mineral
                else -> 0xFF000000.toInt() // Black for others
            })
            setPadding(8, 0, 8, 0)
            layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 2f)
            maxLines = 0 // Allow unlimited lines
            ellipsize = null // No ellipsis for multi-line
            setLineSpacing(2f, 1.2f) // Add line spacing
        }
        
        // Total Price
        val totalText = TextView(this).apply {
            text = String.format("₱%.2f", order.total)
            textSize = 10f
            setTextColor(0xFF000000.toInt())
            setPadding(8, 0, 8, 0)
            layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
            gravity = android.view.Gravity.END
        }
        
        // Payment Status
        val paymentText = TextView(this).apply {
            text = if (order.isPaid) "P" else "UP"
            textSize = 11f
            setTextColor(if (order.isPaid) 0xFF4CAF50.toInt() else 0xFFF44336.toInt())
            setPadding(8, 0, 8, 0)
            layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
            gravity = android.view.Gravity.CENTER
        }
        
        // Delivery Status
        val statusText = TextView(this).apply {
            text = when (order.status) {
                "Delivered" -> "✓" // Checkmark symbol
                "Pending" -> "🕐" // Clock symbol
                else -> order.status
            }
            textSize = 12f
            setTextColor(when (order.status) {
                "Delivered" -> 0xFF4CAF50.toInt()
                "Pending" -> 0xFFFF9800.toInt()
                else -> 0xFF000000.toInt()
            })
            setPadding(8, 0, 8, 0)
            layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
            gravity = android.view.Gravity.CENTER
        }
        
        // Add views to row
        row.addView(nameText)
        row.addView(productText)
        row.addView(totalText)
        row.addView(paymentText)
        row.addView(statusText)
        
        // Set click listener for row actions
        row.setOnClickListener {
            showOrderActionMenu(order, position)
        }
        
        return row
    }

    private fun updateSummaryVisibility() {
        // Summary handle is always visible, no need to hide it
    }

    private fun setupSummaryBottomSheet() {
        summaryHandle.setOnClickListener {
            showSummaryBottomSheet()
        }
    }

    private fun showSummaryBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_summary, null)
        bottomSheetDialog.setContentView(view)

        // Initialize bottom sheet TextViews
        val bottomTotalQty = view.findViewById<TextView>(R.id.bottomTotalQty)
        val bottomAlkalineQty = view.findViewById<TextView>(R.id.bottomAlkalineQty)
        val bottomMineralQty = view.findViewById<TextView>(R.id.bottomMineralQty)
        val bottomTotalUom = view.findViewById<TextView>(R.id.bottomTotalUom)
        val bottomPendingsCount = view.findViewById<TextView>(R.id.bottomPendingsCount)
        val bottomDeliveredCount = view.findViewById<TextView>(R.id.bottomDeliveredCount)
        val bottomTotalRevenue = view.findViewById<TextView>(R.id.bottomTotalRevenue)
        val bottomPaidCount = view.findViewById<TextView>(R.id.bottomPaidCount)
        val bottomUnpaidCount = view.findViewById<TextView>(R.id.bottomUnpaidCount)
        val bottomCollectedRevenue = view.findViewById<TextView>(R.id.bottomCollectedRevenue)

        // Update bottom sheet with current data
        updateBottomSheetSummary(
            bottomTotalQty, bottomAlkalineQty, bottomMineralQty, bottomTotalUom,
            bottomPendingsCount, bottomDeliveredCount, bottomTotalRevenue,
            bottomPaidCount, bottomUnpaidCount, bottomCollectedRevenue
        )

        bottomSheetDialog.show()
    }

    private fun updateBottomSheetSummary(
        totalQty: TextView, alkalineQty: TextView, mineralQty: TextView, totalUom: TextView,
        pendingsCount: TextView, deliveredCount: TextView, totalRevenue: TextView,
        paidCount: TextView, unpaidCount: TextView, collectedRevenue: TextView
    ) {
        val totalRevenueValue = customerOrders.sumOf { it.total }
        val deliveredCountValue = customerOrders.count { it.status == "Delivered" }
        val pendingCountValue = customerOrders.count { it.status == "Pending" }
        val paidCountValue = customerOrders.count { it.isPaid }
        val unpaidCountValue = customerOrders.count { !it.isPaid }
        val collectedRevenueValue = customerOrders.filter { it.isPaid }.sumOf { it.total }
        
        // Extract quantity from product string for total quantity calculation
        val totalQtyValue = customerOrders.sumOf { order ->
            val quantityMatch = Regex("x(\\d+)").find(order.product)
            quantityMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
        }
        
        // Calculate Alkaline and Mineral quantities
        var alkalineQtyValue = 0
        var mineralQtyValue = 0
        
        customerOrders.forEach { order ->
            val quantityMatch = Regex("x(\\d+)").find(order.product)
            val quantity = quantityMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
            
            when {
                order.product.contains(" A ") -> alkalineQtyValue += quantity
                order.product.contains(" M ") -> mineralQtyValue += quantity
            }
        }
        
        // Extract most common UOM from product strings
        val uomCounts = customerOrders.mapNotNull { order ->
            val uomMatch = Regex("^(\\w+)").find(order.product)
            uomMatch?.groupValues?.get(1)
        }.groupBy { it }.mapValues { it.value.size }
        val mainUom = uomCounts.maxByOrNull { it.value }?.key ?: "20L"

        // Update bottom sheet TextViews
        totalQty.text = totalQtyValue.toString()
        alkalineQty.text = alkalineQtyValue.toString()
        mineralQty.text = mineralQtyValue.toString()
        totalUom.text = mainUom
        pendingsCount.text = pendingCountValue.toString()
        deliveredCount.text = deliveredCountValue.toString()
        totalRevenue.text = String.format("₱%.2f", totalRevenueValue)
        paidCount.text = paidCountValue.toString()
        unpaidCount.text = unpaidCountValue.toString()
        collectedRevenue.text = String.format("₱%.2f", collectedRevenueValue)
    }

    private fun updateTabLabels() {
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        
        // Calculate counts for each tab based on status
        val allOrdersCount = customerOrders.size
        val pendingsCount = customerOrders.count { it.status == "Pending" }
        val deliveredCount = customerOrders.count { it.status == "Delivered" }
        
        // Update tab labels with counts
        if (tabLayout.tabCount >= 3) {
            tabLayout.getTabAt(0)?.text = "All Orders ($allOrdersCount)"
            tabLayout.getTabAt(1)?.text = "Pendings ($pendingsCount)"
            tabLayout.getTabAt(2)?.text = "Delivered ($deliveredCount)"
        }
    }

    // Swipe actions removed - using ListView instead of RecyclerView

    private fun showOrderActionMenu(order: CustomerOrder, position: Int) {
        val options = arrayOf(
            if (order.isPaid) "Mark as Unpaid" else "Mark as Paid",
            if (order.status == "Delivered") "Mark as Pending" else "Mark as Delivered",
            "View Details"
        )

        AlertDialog.Builder(this)
            .setTitle(order.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> togglePaymentStatus(order, position)
                    1 -> toggleDeliveryStatus(order, position)
                    2 -> showOrderDetailsDialog(order, position)
                }
            }
            .show()
    }

    private fun togglePaymentStatus(order: CustomerOrder, position: Int) {
        val newPaidStatus = !order.isPaid
        val updatedOrder = order.copy(isPaid = newPaidStatus)
        
        // Update in the main list
        val mainIndex = customerOrders.indexOfFirst { 
            it.name == order.name && it.product == order.product && it.total == order.total 
        }
        if (mainIndex != -1) {
            customerOrders[mainIndex] = updatedOrder
        }
        
        // Update in filtered list
        filteredOrders[position] = updatedOrder
        
        // Refresh the table
        populateTableLayout()
        
        // Update database
        updateOrderInDatabase(updatedOrder)
        
        Toast.makeText(this, "Payment status updated", Toast.LENGTH_SHORT).show()
        updateSummary()
        updateTabLabels()
    }

    private fun toggleDeliveryStatus(order: CustomerOrder, position: Int) {
        val newStatus = if (order.status == "Delivered") "Pending" else "Delivered"
        val updatedOrder = order.copy(status = newStatus)
        
        // Update in the main list
        val mainIndex = customerOrders.indexOfFirst { 
            it.name == order.name && it.product == order.product && it.total == order.total 
        }
        if (mainIndex != -1) {
            customerOrders[mainIndex] = updatedOrder
        }
        
        // Update in filtered list
        filteredOrders[position] = updatedOrder
        
        // Refresh the table
        populateTableLayout()
        
        // Update database
        updateOrderInDatabase(updatedOrder)
        
        Toast.makeText(this, "Delivery status updated", Toast.LENGTH_SHORT).show()
        updateSummary()
        updateTabLabels()
        
        // Reload current tab to reflect filtering changes
        loadOrdersForCurrentTab()
    }

    private fun updateOrderInDatabase(order: CustomerOrder) {
        try {
            // Find the original order to update
            val originalOrder = customerOrders.find { 
                it.name == order.name && it.product == order.product && it.total == order.total 
            }
            
            if (originalOrder != null) {
                val success = orderDatabaseHelper.updateCustomerOrder(order, originalOrder)
                if (!success) {
                    Toast.makeText(this, "Failed to update order in database", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error updating order: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun showOrderDetailsDialog(order: CustomerOrder, position: Int) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_order_details)

        // Make dialog wider
        val window = dialog.window
        window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(), // 90% of screen width
            android.view.WindowManager.LayoutParams.WRAP_CONTENT
        )

        // Set order details
        dialog.findViewById<TextView>(R.id.detailName).text = order.name
        dialog.findViewById<TextView>(R.id.detailAddress).text = order.address
        
        // Format products with bullet points for multi-line display
        val products = order.product.split(", ").filter { it.isNotBlank() }
        val productLines = products.mapIndexed { index, product ->
            val bullet = if (index == 0) "•" else "  •"
            "$bullet $product"
        }
        dialog.findViewById<TextView>(R.id.detailProduct).text = productLines.joinToString("\n")
        
        dialog.findViewById<TextView>(R.id.detailTotal).text = String.format("₱%.2f", order.total)
        dialog.findViewById<TextView>(R.id.detailPayment).text = if (order.isPaid) "Paid" else "Unpaid"
        dialog.findViewById<TextView>(R.id.detailStatus).text = order.status

        // Set up buttons
        dialog.findViewById<Button>(R.id.editButton).setOnClickListener {
            dialog.dismiss()
            showPinDialog("Edit Order") { 
                showEditOrderDialog(order, position)
            }
        }

        dialog.findViewById<Button>(R.id.deleteButton).setOnClickListener {
            dialog.dismiss()
            showPinDialog("Delete Order") { 
                deleteOrder(order, position)
            }
        }

        dialog.findViewById<Button>(R.id.doneButton).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showPinDialog(action: String, onSuccess: () -> Unit) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_pin_access)

        val pinInput = dialog.findViewById<EditText>(R.id.pinInput)
        val submitButton = dialog.findViewById<Button>(R.id.submitPinButton)
        val cancelButton = dialog.findViewById<Button>(R.id.cancelPinButton)

        dialog.findViewById<TextView>(R.id.pinTitle).text = "Enter PIN to $action"

        submitButton.setOnClickListener {
            val enteredPin = pinInput.text.toString()
            if (enteredPin == "0000") {
                dialog.dismiss()
                onSuccess()
            } else {
                Toast.makeText(this, "Invalid PIN", Toast.LENGTH_SHORT).show()
                pinInput.text.clear()
            }
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showEditOrderDialog(order: CustomerOrder, position: Int) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_edit_order)

        // Initialize views
        val nameDisplay = dialog.findViewById<TextView>(R.id.nameDisplay)
        val addressDisplay = dialog.findViewById<TextView>(R.id.addressDisplay)
        val quantityInput = dialog.findViewById<EditText>(R.id.quantityInput)
        val uomSpinner = dialog.findViewById<AutoCompleteTextView>(R.id.uomSpinner)
        val waterTypeSpinner = dialog.findViewById<AutoCompleteTextView>(R.id.waterTypeSpinner)
        val paymentStatusGroup = dialog.findViewById<RadioGroup>(R.id.paymentStatusGroup)
        val paidRadio = dialog.findViewById<RadioButton>(R.id.paidRadio)
        val unpaidRadio = dialog.findViewById<RadioButton>(R.id.unpaidRadio)

        // Set current values (name and address are read-only)
        nameDisplay.text = order.name
        addressDisplay.text = order.address

        // Parse product string to extract quantity, UOM, and water type
        val productParts = order.product.split(" ")
        var quantity = 1
        var uom = "20L"
        var waterType = "A"
        
        for (part in productParts) {
            if (part.startsWith("x") && part.length > 1) {
                quantity = part.substring(1).toIntOrNull() ?: 1
            } else if (part == "A" || part == "M") {
                waterType = part
            } else if (part.endsWith("L")) {
                uom = part
            }
        }

        quantityInput.setText(quantity.toString())

        // Set up UOM spinner
        val uomOptions = arrayOf("20L", "5L", "1L", "500ml")
        val uomAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, uomOptions)
        uomSpinner.setAdapter(uomAdapter)
        uomSpinner.setText(uom, false)

        // Set up water type spinner
        val waterTypeOptions = arrayOf("A", "M")
        val waterTypeAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, waterTypeOptions)
        waterTypeSpinner.setAdapter(waterTypeAdapter)
        waterTypeSpinner.setText(waterType, false)

        // Set up payment status
        if (order.isPaid) {
            paidRadio.isChecked = true
        } else {
            unpaidRadio.isChecked = true
        }

        // Set up buttons
        dialog.findViewById<Button>(R.id.saveButton).setOnClickListener {
            try {
                val newQuantity = quantityInput.text.toString().toIntOrNull()
                val newUom = uomSpinner.text.toString()
                val newWaterType = waterTypeSpinner.text.toString()
                val newIsPaid = paidRadio.isChecked

                if (newQuantity != null && newQuantity > 0) {
                    val newProduct = "$newUom $newWaterType x$newQuantity"
                    
                    val updatedOrder = order.copy(
                        product = newProduct,
                        isPaid = newIsPaid
                    )

                    // Update in main list
                    val mainIndex = customerOrders.indexOfFirst { 
                        it.name == order.name && it.product == order.product && it.total == order.total 
                    }
                    if (mainIndex != -1) {
                        customerOrders[mainIndex] = updatedOrder
                    }

                    // Update in filtered list
                    filteredOrders[position] = updatedOrder
                    
                    // Refresh the table
                    populateTableLayout()

                    // Update database
                    updateOrderInDatabase(updatedOrder)

                    Toast.makeText(this, "Order updated successfully", Toast.LENGTH_SHORT).show()
                    updateSummary()
                    updateTabLabels()
                    
                    // Reload current tab to reflect filtering changes
                    loadOrdersForCurrentTab()
                    
                    dialog.dismiss()
                } else {
                    Toast.makeText(this, "Please enter a valid quantity", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Error updating order: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }

        dialog.findViewById<Button>(R.id.cancelButton).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun deleteOrder(order: CustomerOrder, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Delete Order")
            .setMessage("Are you sure you want to delete this order?\n\nThis action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                try {
                    val success = orderDatabaseHelper.deleteCustomerOrder(order)
                    if (success) {
                        // Remove from main list
                        customerOrders.removeAll { 
                            it.name == order.name && it.product == order.product && it.total == order.total 
                        }
                        
                        // Update filtered list
                        loadOrdersForCurrentTab()
                        
                        Toast.makeText(this, "Order deleted successfully", Toast.LENGTH_SHORT).show()
                        updateSummary()
                        updateTabLabels()
                    } else {
                        Toast.makeText(this, "Failed to delete order from database", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Error deleting order: ${e.message}", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateSummary() {
        val totalRevenue = customerOrders.sumOf { it.total }
        
        // Extract quantity from product string for total quantity calculation
        val totalQty = customerOrders.sumOf { order ->
            val quantityMatch = Regex("x(\\d+)").find(order.product)
            quantityMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
        }
        
        // Update quick summary
        quickTotalQtyText.text = totalQty.toString()
        quickRevenueText.text = String.format("₱%.2f", totalRevenue)
    }
    
    private fun testTableLayoutVisibility() {
        println("DEBUG: Testing TableLayout visibility")
        println("DEBUG: TableLayout child count: ${ordersTable.childCount}")
        println("DEBUG: Filtered orders size: ${filteredOrders.size}")
        println("DEBUG: Customer orders size: ${customerOrders.size}")
        
        // Force a layout pass
        ordersTable.requestLayout()
        ordersTable.invalidate()
        
        println("DEBUG: TableLayout test complete")
    }

    private fun addCustomerOrderToDatabase(order: CustomerOrder) {
        try {
            val success = orderDatabaseHelper.addCustomerOrder(order)
            
            if (success) {
                // Update inventory for gallons
                val productParts = order.product.split(" ")
                var waterTypeName = "Mineral"
                var uom = "20L"
                var quantity = 1
                
                for (part in productParts) {
                    if (part.endsWith("L")) {
                        uom = part
                    } else if (part == "A") {
                        waterTypeName = "Alkaline"
                    } else if (part.startsWith("x") && part.length > 1) {
                        quantity = part.substring(1).toIntOrNull() ?: 1
                    }
                }
                
                if (waterTypeName.isNotEmpty()) {
                    val itemKey = orderDatabaseHelper.getItemKeyForInventory(waterTypeName, uom)
                    if (itemKey != null) {
                        orderDatabaseHelper.processPurchaseAndUpdateInventory(itemKey, quantity, this)
                    }
                }
                
                Toast.makeText(this, "Order added successfully", Toast.LENGTH_SHORT).show()
                // Reload today's orders to reflect the new data
                loadTodaysOrders()
                updateSummary()
                updateTabLabels()
            } else {
                Toast.makeText(this, "Failed to add order to database", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error saving order: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun showAddOrderDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_add_order)

        // Initialize views
        val productTypeSpinner = dialog.findViewById<Spinner>(R.id.productTypeSpinner)
        val uomSpinner = dialog.findViewById<Spinner>(R.id.uomSpinner)
        val waterTypeGroup = dialog.findViewById<RadioGroup>(R.id.waterTypeGroup)
        val mineralRadio = dialog.findViewById<RadioButton>(R.id.mineralRadio)
        val alkalineRadio = dialog.findViewById<RadioButton>(R.id.alkalineRadio)
        val noRefillRadio = dialog.findViewById<RadioButton>(R.id.noRefillRadio)
        val accessoriesLabel = dialog.findViewById<TextView>(R.id.accessoriesLabel)
        val accessoriesSpinner = dialog.findViewById<Spinner>(R.id.accessoriesSpinner)
        val minusButton = dialog.findViewById<Button>(R.id.minusButton)
        val plusButton = dialog.findViewById<Button>(R.id.plusButton)
        val quantityDisplay = dialog.findViewById<TextView>(R.id.quantityDisplay)
        val itemsContainer = dialog.findViewById<LinearLayout>(R.id.itemsContainer)
        val addItemButton = dialog.findViewById<Button>(R.id.addItemButton)
        val totalPriceDisplay = dialog.findViewById<TextView>(R.id.totalPriceDisplay)
        
        // Data class for order items
        data class OrderItemData(
            var productType: String,
            var uom: String?,
            var waterType: String?,
            var accessory: String?,
            var quantity: Int,
            var view: View
        )
        
        // List to store additional order items (not the default one)
        val additionalItems = mutableListOf<OrderItemData>()

        // Initialize product type dropdown
        val productTypes = listOf("Water", "New Gallon", "Accessories")
        val productTypeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, productTypes)
        productTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        productTypeSpinner.adapter = productTypeAdapter
        
        // Set default selection to "Water"
        productTypeSpinner.setSelection(0)

        // Initialize accessories dropdown
        val accessories = listOf(
            "Small Cap Cover", 
            "Big Cap Cover", 
            "Round Cap Cover", 
            "Non Leak Cover"
        )
        val accessoriesAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, accessories)
        accessoriesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        accessoriesSpinner.adapter = accessoriesAdapter

        // Function to update field visibility based on product type
        fun updateFieldVisibility(productType: String) {
            println("DEBUG: Updating field visibility for product type: $productType")
            when (productType) {
                "Water" -> {
                    println("DEBUG: Setting Water fields visible")
                    uomSpinner.visibility = View.VISIBLE
                    waterTypeGroup.visibility = View.VISIBLE
                    noRefillRadio.visibility = View.GONE
                    accessoriesLabel.visibility = View.GONE
                    accessoriesSpinner.visibility = View.GONE
                }
                "New Gallon" -> {
                    println("DEBUG: Setting New Gallon fields visible")
                    uomSpinner.visibility = View.VISIBLE
                    waterTypeGroup.visibility = View.VISIBLE
                    noRefillRadio.visibility = View.VISIBLE
                    accessoriesLabel.visibility = View.GONE
                    accessoriesSpinner.visibility = View.GONE
                }
                "Accessories" -> {
                    println("DEBUG: Setting Accessories fields visible")
                    uomSpinner.visibility = View.GONE
                    waterTypeGroup.visibility = View.GONE
                    noRefillRadio.visibility = View.GONE
                    accessoriesLabel.visibility = View.VISIBLE
                    accessoriesSpinner.visibility = View.VISIBLE
                }
            }
        }

        // Function to update total price
        fun updateTotalPrice() {
            var total = 0.0
            
            // Calculate default item price
            val productType = productTypeSpinner.selectedItem.toString()
            val quantity = quantityDisplay.text.toString().toIntOrNull() ?: 1
            
            total += when (productType) {
                "Water" -> {
                    val uom = uomSpinner.selectedItem.toString()
                    val waterType = if (alkalineRadio.isChecked) "Alkaline" else "Mineral"
                    val waterTypeCode = if (waterType == "Alkaline") "A" else "M"
                    calculatePrice(waterTypeCode, uom, quantity)
                }
                "New Gallon" -> {
                    val uom = uomSpinner.selectedItem.toString()
                    val waterType = when {
                        alkalineRadio.isChecked -> "Alkaline"
                        mineralRadio.isChecked -> "Mineral"
                        noRefillRadio.isChecked -> "No Refill"
                        else -> "Mineral"
                    }
                    val waterTypeCode = when (waterType) {
                        "Alkaline" -> "A"
                        "Mineral" -> "M"
                        "No Refill" -> "NR"
                        else -> "M"
                    }
                    calculatePrice(waterTypeCode, uom, quantity)
                }
                "Accessories" -> {
                    val accessory = accessoriesSpinner.selectedItem.toString()
                    calculateAccessoryPrice(accessory, quantity)
                }
                else -> 0.0
            }
            
            // Calculate additional items prices
            for (item in additionalItems) {
                total += when (item.productType) {
                "Water" -> {
                    val uom = item.view.findViewById<Spinner>(R.id.itemUomSpinner).selectedItem.toString()
                        val waterType = if (item.view.findViewById<RadioButton>(R.id.itemAlkalineRadio).isChecked) "Alkaline" else "Mineral"
                        val waterTypeCode = if (waterType == "Alkaline") "A" else "M"
                        calculatePrice(waterTypeCode, uom, item.quantity)
                    }
                    "New Gallon" -> {
                        val uom = item.view.findViewById<Spinner>(R.id.itemUomSpinner).selectedItem.toString()
                        val waterType = when {
                            item.view.findViewById<RadioButton>(R.id.itemAlkalineRadio).isChecked -> "Alkaline"
                            item.view.findViewById<RadioButton>(R.id.itemMineralRadio).isChecked -> "Mineral"
                            item.view.findViewById<RadioButton>(R.id.itemNoRefillRadio).isChecked -> "No Refill"
                            else -> "Mineral"
                        }
                        val waterTypeCode = when (waterType) {
                            "Alkaline" -> "A"
                            "Mineral" -> "M"
                            "No Refill" -> "NR"
                            else -> "M"
                        }
                        calculatePrice(waterTypeCode, uom, item.quantity)
                    }
                    "Accessories" -> {
                        val accessory = item.view.findViewById<Spinner>(R.id.itemAccessoriesSpinner).selectedItem.toString()
                        calculateAccessoryPrice(accessory, item.quantity)
                    }
                    else -> 0.0
                }
            }
            totalPriceDisplay.text = "Total: ₱${String.format("%.2f", total)}"
        }

        // Set up product type change listener
        productTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedType = productTypes[position]
                println("DEBUG: Product type selected: $selectedType")
                updateFieldVisibility(selectedType)
                updateTotalPrice()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Set default selection (Mineral)
        mineralRadio.isChecked = true
        
        // Initialize field visibility based on default product type
        updateFieldVisibility("Water")
        
        // Set up UOM dropdown with correct options based on default water type
        val defaultUomOptions = listOf("20L Slim", "10L Slim", "20L Round") // Mineral options
        val uomAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, defaultUomOptions)
        uomAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        uomSpinner.adapter = uomAdapter
        
        // Set default UOM selection
        uomSpinner.setSelection(0)

        // Default quantity counter
        var defaultQuantity = 1
        quantityDisplay.text = defaultQuantity.toString()

        minusButton.setOnClickListener {
            if (defaultQuantity > 1) {
                defaultQuantity--
                quantityDisplay.text = defaultQuantity.toString()
                updateTotalPrice()
            }
        }

        plusButton.setOnClickListener {
            defaultQuantity++
            quantityDisplay.text = defaultQuantity.toString()
            updateTotalPrice()
        }

        // Update available UOM options based on water type selection for default controls
        waterTypeGroup.setOnCheckedChangeListener { _, checkedId ->
            val selectedWaterType = when (checkedId) {
                R.id.alkalineRadio -> "Alkaline"
                R.id.mineralRadio -> "Mineral"
                R.id.noRefillRadio -> "No Refill"
                else -> "Mineral"
            }
            
            val availableUOMs = when (selectedWaterType) {
                "Alkaline" -> listOf("20L Slim", "10L Slim")
                "Mineral" -> uomOptions
                "No Refill" -> uomOptions
                else -> uomOptions
            }
            
            val uomAdapter = ArrayAdapter(this@Today, android.R.layout.simple_spinner_item, availableUOMs)
            uomAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            uomSpinner.adapter = uomAdapter
            
            // Set the first available UOM as selected
            uomSpinner.setSelection(0)
            
            updateTotalPrice()
        }

        // Function to add a new additional item row
        fun addItemRow() {
            val itemView = layoutInflater.inflate(R.layout.item_order_row, itemsContainer, false)
            
            val itemProductTypeSpinner = itemView.findViewById<Spinner>(R.id.itemProductTypeSpinner)
            val itemUomSpinner = itemView.findViewById<Spinner>(R.id.itemUomSpinner)
            val itemWaterTypeGroup = itemView.findViewById<RadioGroup>(R.id.itemWaterTypeGroup)
            val itemMineralRadio = itemView.findViewById<RadioButton>(R.id.itemMineralRadio)
            val itemAlkalineRadio = itemView.findViewById<RadioButton>(R.id.itemAlkalineRadio)
            val itemNoRefillRadio = itemView.findViewById<RadioButton>(R.id.itemNoRefillRadio)
            val itemAccessoriesRow = itemView.findViewById<LinearLayout>(R.id.itemAccessoriesRow)
            val itemAccessoriesSpinner = itemView.findViewById<Spinner>(R.id.itemAccessoriesSpinner)
            val itemMinusButton = itemView.findViewById<Button>(R.id.itemMinusButton)
            val itemPlusButton = itemView.findViewById<Button>(R.id.itemPlusButton)
            val itemQuantityDisplay = itemView.findViewById<TextView>(R.id.itemQuantityDisplay)
            val removeButton = itemView.findViewById<Button>(R.id.itemRemoveButton)
            
            // Set up product type dropdown
            val itemProductTypeAdapter = ArrayAdapter(this@Today, android.R.layout.simple_spinner_item, productTypes)
            itemProductTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            itemProductTypeSpinner.adapter = itemProductTypeAdapter
            itemProductTypeSpinner.setSelection(0)
            
            // Set up UOM dropdown with correct options based on default water type
            val defaultUomOptions = listOf("20L Slim", "10L Slim", "20L Round") // Mineral options
            val itemUomAdapter = ArrayAdapter(this@Today, android.R.layout.simple_spinner_item, defaultUomOptions)
            itemUomAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            itemUomSpinner.adapter = itemUomAdapter
            itemUomSpinner.setSelection(0)
            
            // Set up accessories dropdown
            val itemAccessoriesAdapter = ArrayAdapter(this@Today, android.R.layout.simple_spinner_item, accessories)
            itemAccessoriesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            itemAccessoriesSpinner.adapter = itemAccessoriesAdapter
            itemAccessoriesSpinner.setSelection(0)
            
            // Set default selection (Mineral)
            itemMineralRadio.isChecked = true
            
            // Function to update field visibility for this item
            fun updateItemFieldVisibility(productType: String) {
                when (productType) {
                    "Water" -> {
                        itemUomSpinner.visibility = View.VISIBLE
                        itemWaterTypeGroup.visibility = View.VISIBLE
                        itemNoRefillRadio.visibility = View.GONE
                        itemAccessoriesRow.visibility = View.GONE
                    }
                    "New Gallon" -> {
                        itemUomSpinner.visibility = View.VISIBLE
                        itemWaterTypeGroup.visibility = View.VISIBLE
                        itemNoRefillRadio.visibility = View.VISIBLE
                        itemAccessoriesRow.visibility = View.GONE
                    }
                    "Accessories" -> {
                        itemUomSpinner.visibility = View.GONE
                        itemWaterTypeGroup.visibility = View.GONE
                        itemNoRefillRadio.visibility = View.GONE
                        itemAccessoriesRow.visibility = View.VISIBLE
                    }
                }
            }
            
            // Initialize field visibility based on default product type
            updateItemFieldVisibility("Water")
            
            // Set up product type change listener
            itemProductTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val selectedType = productTypes[position]
                    updateItemFieldVisibility(selectedType)
                    // Update the order item data
                    val existingItem = additionalItems.find { it.view == itemView }
                    existingItem?.productType = selectedType
                    updateTotalPrice()
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
            
            // Quantity counter
            var quantity = 1
            itemQuantityDisplay.text = quantity.toString()
            
            itemMinusButton.setOnClickListener {
                if (quantity > 1) {
                    quantity--
                    itemQuantityDisplay.text = quantity.toString()
                    // Update the order item data
                    val existingItem = additionalItems.find { it.view == itemView }
                    existingItem?.quantity = quantity
                    updateTotalPrice()
                }
            }
            
            itemPlusButton.setOnClickListener {
                quantity++
                itemQuantityDisplay.text = quantity.toString()
                // Update the order item data
                val existingItem = additionalItems.find { it.view == itemView }
                existingItem?.quantity = quantity
                updateTotalPrice()
            }
            
            // Update available UOM options based on water type selection
            itemWaterTypeGroup.setOnCheckedChangeListener { _, checkedId ->
                val selectedWaterType = when (checkedId) {
                    R.id.itemAlkalineRadio -> "Alkaline"
                    R.id.itemMineralRadio -> "Mineral"
                    R.id.itemNoRefillRadio -> "No Refill"
                    else -> "Mineral"
                }
                
                val availableUOMs = when (selectedWaterType) {
                    "Alkaline" -> listOf("20L Slim", "10L Slim")
                    "Mineral" -> uomOptions
                    "No Refill" -> uomOptions
                    else -> uomOptions
                }
                
                val itemUomAdapter = ArrayAdapter(this@Today, android.R.layout.simple_spinner_item, availableUOMs)
                itemUomAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                itemUomSpinner.adapter = itemUomAdapter
                
                // Set the first available UOM as selected
                itemUomSpinner.setSelection(0)
                
                updateTotalPrice()
            }
            
            // Remove button
            removeButton.setOnClickListener {
                itemsContainer.removeView(itemView)
                additionalItems.removeAll { it.view == itemView }
                updateTotalPrice()
            }
            
            // Create order item data
            val orderItem = OrderItemData(
                productType = itemProductTypeSpinner.selectedItem.toString(),
                uom = itemUomSpinner.selectedItem.toString(),
                waterType = if (itemAlkalineRadio.isChecked) "Alkaline" else "Mineral",
                accessory = itemAccessoriesSpinner.selectedItem.toString(),
                quantity = quantity,
                view = itemView
            )
            
            additionalItems.add(orderItem)
            itemsContainer.addView(itemView)
            updateTotalPrice()
        }
        
        // Initialize total price
        updateTotalPrice()
        
        // Add item button
        addItemButton.setOnClickListener {
            addItemRow()
        }

        // Set up buttons
        dialog.findViewById<Button>(R.id.addButton).setOnClickListener {
            try {
                val name = dialog.findViewById<EditText>(R.id.nameInput).text.toString()
                val address = dialog.findViewById<EditText>(R.id.addressInput).text.toString()
                val isPaid = dialog.findViewById<RadioButton>(R.id.paidRadio).isChecked

                if (name.isNotBlank() && address.isNotBlank()) {
                    // Create product string from all items
                    val productStrings = mutableListOf<String>()
                    var totalPrice = 0.0
                    
                    // Add default item
                    val productType = productTypeSpinner.selectedItem.toString()
                    val quantity = quantityDisplay.text.toString().toIntOrNull() ?: 1
                    
                    val defaultItemString = when (productType) {
                        "Water" -> {
                            val uom = uomSpinner.selectedItem.toString()
                            val waterType = if (alkalineRadio.isChecked) "Alkaline" else "Mineral"
                            val waterTypeCode = if (waterType == "Alkaline") "A" else "M"
                            val price = calculatePrice(waterTypeCode, uom, quantity)
                            totalPrice += price
                            "${uom} ${waterTypeCode} x${quantity}"
                        }
                        "New Gallon" -> {
                            val uom = uomSpinner.selectedItem.toString()
                            val waterType = when {
                                alkalineRadio.isChecked -> "Alkaline"
                                mineralRadio.isChecked -> "Mineral"
                                noRefillRadio.isChecked -> "No Refill"
                                else -> "Mineral"
                            }
                            val waterTypeCode = when (waterType) {
                                "Alkaline" -> "A"
                                "Mineral" -> "M"
                                "No Refill" -> "NR"
                                else -> "M"
                            }
                            val price = calculatePrice(waterTypeCode, uom, quantity)
                            totalPrice += price
                            "${uom} ${waterTypeCode} x${quantity}"
                        }
                        "Accessories" -> {
                            val accessory = accessoriesSpinner.selectedItem.toString()
                            val price = calculateAccessoryPrice(accessory, quantity)
                            totalPrice += price
                            "${accessory} x${quantity}"
                        }
                        else -> ""
                    }
                    productStrings.add(defaultItemString)
                    
                    // Add additional items
                    for (item in additionalItems) {
                        val itemString = when (item.productType) {
                            "Water" -> {
                                val uom = item.view.findViewById<Spinner>(R.id.itemUomSpinner).selectedItem.toString()
                                val waterType = if (item.view.findViewById<RadioButton>(R.id.itemAlkalineRadio).isChecked) "Alkaline" else "Mineral"
                                val waterTypeCode = if (waterType == "Alkaline") "A" else "M"
                                val price = calculatePrice(waterTypeCode, uom, item.quantity)
                                totalPrice += price
                                "${uom} ${waterTypeCode} x${item.quantity}"
                            }
                            "New Gallon" -> {
                                val uom = item.view.findViewById<Spinner>(R.id.itemUomSpinner).selectedItem.toString()
                                val waterType = when {
                                    item.view.findViewById<RadioButton>(R.id.itemAlkalineRadio).isChecked -> "Alkaline"
                                    item.view.findViewById<RadioButton>(R.id.itemMineralRadio).isChecked -> "Mineral"
                                    item.view.findViewById<RadioButton>(R.id.itemNoRefillRadio).isChecked -> "No Refill"
                                    else -> "Mineral"
                                }
                                val waterTypeCode = when (waterType) {
                                    "Alkaline" -> "A"
                                    "Mineral" -> "M"
                                    "No Refill" -> "NR"
                                    else -> "M"
                                }
                                val price = calculatePrice(waterTypeCode, uom, item.quantity)
                                totalPrice += price
                                "${uom} ${waterTypeCode} x${item.quantity}"
                            }
                            "Accessories" -> {
                                val accessory = item.view.findViewById<Spinner>(R.id.itemAccessoriesSpinner).selectedItem.toString()
                                val price = calculateAccessoryPrice(accessory, item.quantity)
                                totalPrice += price
                                "${accessory} x${item.quantity}"
                            }
                            else -> ""
                        }
                        productStrings.add(itemString)
                    }
                    
                    val product = productStrings.joinToString("\n") // Use newline instead of comma for better display
                    
                    val order = CustomerOrder(
                        name = name,
                        address = address,
                        product = product,
                        total = totalPrice,
                        isPaid = isPaid,
                        status = "Pending" // New orders start as pending
                    )
                    addCustomerOrderToDatabase(order)
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
        return when (waterType) {
            "A" -> {
                val fullWaterType = "Alkaline"
                (prices[fullWaterType]?.get(uom) ?: 0.0) * quantity
            }
            "M" -> {
                val fullWaterType = "Mineral"
                (prices[fullWaterType]?.get(uom) ?: 0.0) * quantity
            }
            "NR" -> {
                // No refill - just the gallon price (accessory price)
                val gallonPrices = mapOf(
                    "20L Slim" to 200.0,  // Slim gallon
                    "10L Slim" to 200.0,  // Slim gallon
                    "20L Round" to 200.0  // Round gallon
                )
                (gallonPrices[uom] ?: 0.0) * quantity
            }
            else -> 0.0
        }
    }

    private fun calculateAccessoryPrice(accessory: String, quantity: Int): Double {
        val accessoryPrices = mapOf(
            "Small Cap Cover" to 10.0,
            "Big Cap Cover" to 25.0,
            "Round Cap Cover" to 5.0,
            "Non Leak Cover" to 3.0
        )
        return (accessoryPrices[accessory] ?: 0.0) * quantity
    }
}