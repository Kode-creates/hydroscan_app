package com.adamson.fhydroscan

import android.app.Dialog
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.adamson.fhydroscan.data.Order
import com.adamson.fhydroscan.data.OrderItem
import com.adamson.fhydroscan.database.OrderDatabaseHelper
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout

class Today : AppCompatActivity() {

    private val customerOrders = mutableListOf<CustomerOrder>()
    private val filteredOrders = mutableListOf<CustomerOrder>()
    private lateinit var ordersRecyclerView: RecyclerView
    private lateinit var orderAdapter: OrderRecyclerAdapter
    private lateinit var quickTotalQtyText: TextView
    private lateinit var quickRevenueText: TextView
    private lateinit var orderDatabaseHelper: OrderDatabaseHelper
    private lateinit var summaryHandle: LinearLayout
    private lateinit var tabLayout: TabLayout
    private lateinit var sharedPreferences: SharedPreferences
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
            "10L Slim" to 25.0,
            "20L Round" to 50.0
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

            // Initialize database helpers
            try {
                orderDatabaseHelper = OrderDatabaseHelper(this)
                sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)
                println("DEBUG: Database helpers initialized successfully")
            } catch (e: Exception) {
                println("DEBUG: Error initializing database helpers: ${e.message}")
                e.printStackTrace()
                throw e // Re-throw to be caught by outer try-catch
            }

            // Initialize views
            try {
                ordersRecyclerView = findViewById(R.id.ordersRecyclerView)
                quickTotalQtyText = findViewById(R.id.quickTotalQty)
                quickRevenueText = findViewById(R.id.quickRevenue)
                summaryHandle = findViewById(R.id.summaryHandle)
                tabLayout = findViewById(R.id.tabLayout)
                println("DEBUG: Views initialized successfully")
            } catch (e: Exception) {
                println("DEBUG: Error initializing views: ${e.message}")
                e.printStackTrace()
                throw e // Re-throw to be caught by outer try-catch
            }
            
            // Setup RecyclerView with OrderRecyclerAdapter
            try {
                orderAdapter = OrderRecyclerAdapter(filteredOrders) { order, position ->
                    showOrderActionMenu(order, position)
                }
                ordersRecyclerView.layoutManager = LinearLayoutManager(this)
                ordersRecyclerView.adapter = orderAdapter
                println("DEBUG: RecyclerView setup completed successfully")
            } catch (e: Exception) {
                println("DEBUG: Error setting up RecyclerView: ${e.message}")
                e.printStackTrace()
                throw e // Re-throw to be caught by outer try-catch
            }
            

            // Set up summary bottom sheet
            try {
                setupSummaryBottomSheet()
                println("DEBUG: Summary bottom sheet setup completed successfully")
            } catch (e: Exception) {
                println("DEBUG: Error setting up summary bottom sheet: ${e.message}")
                e.printStackTrace()
                throw e
            }

            // Load today's orders from database
            try {
                loadTodaysOrders()
                println("DEBUG: Load today's orders completed successfully")
            } catch (e: Exception) {
                println("DEBUG: Error loading today's orders: ${e.message}")
                e.printStackTrace()
                throw e
            }
            
            
            // Load orders for current tab to populate RecyclerView
            try {
                loadOrdersForCurrentTab()
                println("DEBUG: After loadOrdersForCurrentTab - filteredOrders.size: ${filteredOrders.size}")
                println("DEBUG: After loadOrdersForCurrentTab - adapter.itemCount: ${orderAdapter.itemCount}")
                
            } catch (e: Exception) {
                Toast.makeText(this, "Data loading error: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
            

            // Set up tab layout
            try {
                tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    currentTab = tab?.position ?: 0
                    loadOrdersForCurrentTab()
                    updateSummaryVisibility()
                }
                override fun onTabUnselected(tab: TabLayout.Tab?) {}
                override fun onTabReselected(tab: TabLayout.Tab?) {}
            })
            
            // Update tab labels with counts
            updateTabLabels()
            
            println("DEBUG: Tab layout setup completed successfully")
            } catch (e: Exception) {
                println("DEBUG: Error setting up tab layout: ${e.message}")
                e.printStackTrace()
                throw e
            }

            // Set up add order button
            findViewById<FloatingActionButton>(R.id.addOrderButton).setOnClickListener {
                showAddOrderDialog()
            }
            
            // Set up long press on add order button to clear all orders
            findViewById<FloatingActionButton>(R.id.addOrderButton).setOnLongClickListener {
                clearAllOrders()
                true
            }
            
            // Set up add order button click listener
            findViewById<FloatingActionButton>(R.id.addOrderButton).setOnClickListener {
                showAddOrderDialog()
            }

            updateSummary()
            
            // Ensure the first tab is selected and data is loaded
            tabLayout.selectTab(tabLayout.getTabAt(0))
            currentTab = 0
            
            // RecyclerView setup complete
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

    override fun onResume() {
        super.onResume()
        // Refresh today's orders when activity resumes
        loadTodaysOrders()
    }

    private fun loadTodaysOrders() {
        try {
            // Clear existing orders
            customerOrders.clear()
            
            // Load orders from database
            val orders = orderDatabaseHelper.getTodaysCustomerOrders()
            customerOrders.addAll(orders)
            
            println("DEBUG: loadTodaysOrders - Loaded ${customerOrders.size} orders from database")
            
            // Debug: Print all loaded orders
            customerOrders.forEachIndexed { index, order ->
                println("DEBUG: Order $index: ${order.name} - ${order.address} - ${order.product} - ${order.total}")
            }
            
            // Load orders for current tab
            loadOrdersForCurrentTab()
        } catch (e: Exception) {
            Toast.makeText(this, "Error loading today's orders: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }
    
    private fun clearAllOrders() {
        AlertDialog.Builder(this)
            .setTitle("Clear All Customer Data")
            .setMessage("Are you sure you want to delete ALL customer orders, cart items, and related data? This action cannot be undone.")
            .setPositiveButton("Delete All") { _, _ ->
                try {
                    println("DEBUG: Starting to clear all customer data...")
                    
                    // Clear from all database tables
                    val orderSuccess = orderDatabaseHelper.clearAllData()
                    val cartSuccess = clearCartData()
                    val inventorySuccess = clearInventoryData()
                    val pricingSuccess = clearPricingData()
                    
                    println("DEBUG: Clear results - Orders: $orderSuccess, Cart: $cartSuccess, Inventory: $inventorySuccess, Pricing: $pricingSuccess")
                    
                    if (orderSuccess && cartSuccess) {
                        // Clear from memory
                        customerOrders.clear()
                        filteredOrders.clear()
                        orderAdapter.updateOrders(filteredOrders)
                        updateSummary()
                        updateTabLabels()
                        Toast.makeText(this, "All customer data cleared successfully", Toast.LENGTH_SHORT).show()
                        println("DEBUG: All data cleared successfully")
                    } else {
                        Toast.makeText(this, "Some data clearing failed, but orders cleared", Toast.LENGTH_SHORT).show()
                        println("DEBUG: Some data clearing failed")
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Error clearing data: ${e.message}", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun clearCartData(): Boolean {
        return try {
            val cartHelper = com.adamson.fhydroscan.database.CartDatabaseHelper(this)
            // Clear all cart items
            val db = cartHelper.writableDatabase
            val deleted = db.delete("cart_items", null, null)
            db.close()
            println("DEBUG: clearCartData - Deleted $deleted cart items")
            true
        } catch (e: Exception) {
            println("DEBUG: clearCartData - Error: ${e.message}")
            e.printStackTrace()
            false
        }
    }
    
    
    private fun clearInventoryData(): Boolean {
        return try {
            val inventoryHelper = com.adamson.fhydroscan.database.InventoryDatabaseHelper(this)
            val db = inventoryHelper.writableDatabase
            val deleted = db.delete("inventory", null, null)
            db.close()
            println("DEBUG: clearInventoryData - Deleted $deleted inventory records")
            true
        } catch (e: Exception) {
            println("DEBUG: clearInventoryData - Error: ${e.message}")
            e.printStackTrace()
            false
        }
    }
    
    private fun clearPricingData(): Boolean {
        return try {
            val pricingHelper = com.adamson.fhydroscan.database.PricingDatabaseHelper(this)
            val db = pricingHelper.writableDatabase
            val deleted = db.delete("product_prices", null, null)
            db.close()
            println("DEBUG: clearPricingData - Deleted $deleted pricing records")
            true
        } catch (e: Exception) {
            println("DEBUG: clearPricingData - Error: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    private fun loadOrdersForCurrentTab() {
        val newFilteredOrders = when (currentTab) {
            0 -> customerOrders // All Orders
            1 -> customerOrders.filter { it.status == "Pending" } // Pendings
            2 -> customerOrders.filter { it.status == "Delivered" } // Delivered
            else -> customerOrders
        }
        
        // Sort orders: delivered orders go to bottom, new orders go to end
        val sortedOrders = newFilteredOrders.sortedWith(compareBy<CustomerOrder> { it.status == "Delivered" }
            .thenByDescending { it.name }) // New orders (by name) go to end
        
        filteredOrders.clear()
        filteredOrders.addAll(sortedOrders)
        
        // Update RecyclerView adapter
        orderAdapter.updateOrders(filteredOrders)
        
        updateSummary()
    }

    // TableLayout methods removed - using RecyclerView now

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
        
        // Calculate total UOM by adding the actual UOM values (20L + 10L + 20L = 50L)
        var totalUomValue = 0
        customerOrders.forEach { order ->
            // Parse each order's product string to extract UOM values
            val productLines = if (order.product.contains("\n")) {
                order.product.split("\n").filter { it.isNotBlank() }
            } else {
                order.product.split(", ").filter { it.isNotBlank() }
            }
            
            productLines.forEach { productLine ->
                // Extract UOM value (20L, 10L, etc.) and quantity
                val uomMatch = Regex("(\\d+)L").find(productLine)
                val quantityMatch = Regex("x(\\d+)").find(productLine)
                
                val uomValue = uomMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
                val quantity = quantityMatch?.groupValues?.get(1)?.toIntOrNull() ?: 1
                
                // Add UOM value multiplied by quantity
                totalUomValue += uomValue * quantity
            }
        }

        // Update bottom sheet TextViews
        totalQty.text = totalQtyValue.toString()
        alkalineQty.text = alkalineQtyValue.toString()
        mineralQty.text = mineralQtyValue.toString()
        totalUom.text = "${totalUomValue}L"
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
            "Mark as Paid & Delivered",
            "View Details"
        )

        AlertDialog.Builder(this)
            .setTitle(order.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> togglePaymentStatus(order, position)
                    1 -> toggleDeliveryStatus(order, position)
                    2 -> markAsPaidAndDelivered(order, position)
                    3 -> showOrderDetailsDialog(order, position)
                }
            }
            .show()
    }

    private fun togglePaymentStatus(order: CustomerOrder, position: Int) {
        val newPaidStatus = !order.isPaid
        val updatedOrder = order.copy(isPaid = newPaidStatus)
        
        // Update in the main list
        val mainIndex = customerOrders.indexOfFirst { 
            it.name == order.name && it.address == order.address
        }
        if (mainIndex != -1) {
            customerOrders[mainIndex] = updatedOrder
        }
        
        // Update in filtered list
        filteredOrders[position] = updatedOrder
        
        // Update RecyclerView
        orderAdapter.updateOrders(filteredOrders)
        
        // Update database
        updateOrderInDatabase(updatedOrder)
        
        Toast.makeText(this, "Payment status updated", Toast.LENGTH_SHORT).show()
        updateSummary()
        updateTabLabels()
    }

    private fun toggleDeliveryStatus(order: CustomerOrder, position: Int) {
        val newStatus = if (order.status == "Delivered") "Pending" else "Delivered"
        // Preserve the payment status when changing delivery status
        val updatedOrder = order.copy(status = newStatus, isPaid = order.isPaid)
        
        println("DEBUG: toggleDeliveryStatus - Original: isPaid=${order.isPaid}, status=${order.status}")
        println("DEBUG: toggleDeliveryStatus - Updated: isPaid=${updatedOrder.isPaid}, status=${updatedOrder.status}")
        
        // Update in the main list
        val mainIndex = customerOrders.indexOfFirst { 
            it.name == order.name && it.address == order.address
        }
        if (mainIndex != -1) {
            customerOrders[mainIndex] = updatedOrder
        }
        
        // Update in filtered list
        filteredOrders[position] = updatedOrder
        
        // Update RecyclerView
        orderAdapter.updateOrders(filteredOrders)
        
        // Update database
        updateOrderInDatabase(updatedOrder)
        
        // Update OrderHistory status
        val historyUpdated = orderDatabaseHelper.updateOrderHistoryStatus(updatedOrder)
        if (historyUpdated) {
            Toast.makeText(this, "Order delivered successfully", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Order delivered, but failed to update OrderHistory", Toast.LENGTH_SHORT).show()
        }
        
        updateSummary()
        updateTabLabels()
    }


    private fun markAsPaidAndDelivered(order: CustomerOrder, position: Int) {
        val updatedOrder = order.copy(isPaid = true, status = "Delivered")
        
        // Update in the main list
        val mainIndex = customerOrders.indexOfFirst { 
            it.name == order.name && it.address == order.address
        }
        if (mainIndex != -1) {
            customerOrders[mainIndex] = updatedOrder
        }
        
        // Update in filtered list
        filteredOrders[position] = updatedOrder
        
        // Update RecyclerView
        orderAdapter.updateOrders(filteredOrders)
        
        // Update database
        updateOrderInDatabase(updatedOrder)
        
        // Update OrderHistory status
        val historyUpdated = orderDatabaseHelper.updateOrderHistoryStatus(updatedOrder)
        if (historyUpdated) {
            Toast.makeText(this, "Order marked as paid and delivered", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Order marked as paid and delivered, but failed to update OrderHistory", Toast.LENGTH_SHORT).show()
        }
        
        updateSummary()
        updateTabLabels()
    }

    private fun updateOrderInDatabase(order: CustomerOrder) {
        try {
            println("DEBUG: updateOrderInDatabase - Updating order: ${order.name} - ${order.address}, isPaid: ${order.isPaid}, status: ${order.status}")
            
            // Find the original order to update by name and address (more reliable than product string)
            val originalOrder = customerOrders.find { 
                it.name == order.name && it.address == order.address
            }
            
            if (originalOrder != null) {
                println("DEBUG: updateOrderInDatabase - Found original order: isPaid: ${originalOrder.isPaid}, status: ${originalOrder.status}")
                
                // Try to update existing order in database
                val success = orderDatabaseHelper.updateCustomerOrder(order, originalOrder)
                if (!success) {
                    // If update failed, it might be because the order doesn't exist in database
                    // Try to add it as a new order instead
                    println("DEBUG: Update failed, trying to add order to database: ${order.name} - ${order.address}")
                    val addSuccess = orderDatabaseHelper.addCustomerOrder(order)
                    if (addSuccess) {
                        println("DEBUG: Successfully added order to database: ${order.name} - ${order.address}")
                    } else {
                        Toast.makeText(this, "Failed to update/add order in database", Toast.LENGTH_SHORT).show()
                        println("DEBUG: Failed to add order to database: ${order.name} - ${order.address}")
                    }
                } else {
                    println("DEBUG: Successfully updated order: ${order.name} - ${order.address}")
                }
            } else {
                Toast.makeText(this, "Order not found for update", Toast.LENGTH_SHORT).show()
                println("DEBUG: Order not found for update: ${order.name} - ${order.address}")
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
        
        // Populate items list
        populateItemsList(dialog, order)
        
        dialog.findViewById<TextView>(R.id.detailTotal).text = String.format("₱%.2f", order.total)
        dialog.findViewById<TextView>(R.id.detailPayment).text = if (order.isPaid) "Paid" else "Unpaid"
        dialog.findViewById<TextView>(R.id.detailStatus).text = order.status

        // Set up buttons
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

    private fun populateItemsList(dialog: Dialog, order: CustomerOrder) {
        val itemsContainer = dialog.findViewById<LinearLayout>(R.id.itemsListContainer)
        itemsContainer.removeAllViews()
        
        println("DEBUG: populateItemsList - Order product: ${order.product}")
        
        // Parse the product string to create individual items
        // Handle both comma-separated and newline-separated formats
        val productLines = if (order.product.contains("\n")) {
            order.product.split("\n").filter { it.isNotBlank() }
        } else {
            order.product.split(", ").filter { it.isNotBlank() }
        }
        
        println("DEBUG: populateItemsList - Product lines: $productLines")
        
        for (productLine in productLines) {
            val itemView = LayoutInflater.from(this).inflate(R.layout.item_order_detail_item, null)
            
            // Parse the product line to extract details
            val parts = productLine.trim().split(" ")
            
            // Extract quantity (format: x2, x3, etc.) - improved regex to handle various formats
            val quantityMatch = Regex("x(\\d+)").find(productLine)
            val quantity = quantityMatch?.groupValues?.get(1)?.toIntOrNull() ?: 1
            
            println("DEBUG: populateItemsList - Product line: $productLine, Quantity: $quantity")
            
            // Extract water type (A, M, or NR)
            val waterType = when {
                productLine.contains(" A ") || productLine.contains(" A") -> "Alkaline"
                productLine.contains(" M ") || productLine.contains(" M") -> "Mineral"
                productLine.contains(" NR ") || productLine.contains(" NR") -> "No Refill"
                else -> "Mineral"
            }
            
            // Extract UOM (20L, 10L, etc.) and determine if it's Slim or Round
            val size = parts.find { it.endsWith("L") } ?: "20L"
            val gallonType = when {
                productLine.contains("Slim") -> "Slim"
                productLine.contains("Round") -> "Round"
                else -> "Unknown"
            }
            
            // Build full UOM for pricing calculation
            val fullUom = when {
                gallonType != "Unknown" -> "$size $gallonType"
                else -> "$size Slim" // Default to Slim if not specified
            }
            
            // Build product name with proper Slim/Round indication
            val productName = when {
                productLine.contains("Cap Cover") || productLine.contains("Cover") -> {
                    productLine.replace(" x$quantity", "")
                }
                gallonType != "Unknown" -> "${size} ${gallonType} ${waterType}"
                else -> "${size} ${waterType}"
            }
            
            // Calculate price
            val price = when {
                productLine.contains("Cap Cover") || productLine.contains("Cover") -> {
                    val accessoryPrices = mapOf(
                        "Small Cap Cover" to 10.0,
                        "Big Cap Cover" to 25.0,
                        "Round Cap Cover" to 5.0,
                        "Non Leak Cover" to 3.0
                    )
                    val accessoryName = productLine.replace(" x$quantity", "")
                    (accessoryPrices[accessoryName] ?: 0.0) * quantity
                }
                else -> {
                    val waterTypeCode = when (waterType) {
                        "Alkaline" -> "A"
                        "Mineral" -> "M"
                        "No Refill" -> "NR"
                        else -> "M"
                    }
                    calculatePrice(waterTypeCode, fullUom, quantity)
                }
            }
            
            println("DEBUG: populateItemsList - Final product name: $productName, Price: $price")
            
            // Set item details
            itemView.findViewById<TextView>(R.id.itemName).text = productName
            itemView.findViewById<TextView>(R.id.itemQuantity).text = "Qty: $quantity"
            itemView.findViewById<TextView>(R.id.itemPrice).text = "₱${String.format("%.2f", price)}"
            
            val details = buildString {
                if (waterType != "No Refill") {
                    append("Water Type: $waterType")
                    if (gallonType != "Unknown") {
                        append(" • Gallon Type: $gallonType")
                    }
                    if (size.isNotEmpty()) {
                        append(" • UOM: $size")
                    }
                } else {
                    append("Type: No Refill")
                    if (gallonType != "Unknown") {
                        append(" • Gallon Type: $gallonType")
                    }
                    if (size.isNotEmpty()) {
                        append(" • UOM: $size")
                    }
                }
            }
            itemView.findViewById<TextView>(R.id.itemDetails).text = details
            
            itemsContainer.addView(itemView)
        }
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
            val storedPin = sharedPreferences.getString("admin_pin", "0000") ?: "0000"
            if (enteredPin == storedPin) {
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


    private fun deleteOrder(order: CustomerOrder, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Delete Order")
            .setMessage("Are you sure you want to delete this order?\n\nThis action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                try {
                    println("DEBUG: deleteOrder - Attempting to delete order: ${order.name} - ${order.address}")
                    val success = orderDatabaseHelper.deleteCustomerOrder(order)
                    println("DEBUG: deleteOrder - Database deletion result: $success")
                    
                    if (success) {
                        // Remove from main list
                        val removedCount = customerOrders.removeAll { 
                            it.name == order.name && it.address == order.address
                        }
                        println("DEBUG: deleteOrder - Removed from main list: $removedCount items")
                        
                        // Remove from RecyclerView
                        orderAdapter.removeOrder(position)
                        println("DEBUG: deleteOrder - Removed from RecyclerView at position: $position")
                        
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
        // Handle both single items and multiple items (newline-separated)
        // Count the number of gallons (not just quantities)
        val totalQty = customerOrders.sumOf { order ->
            val productLines = if (order.product.contains("\n")) {
                order.product.split("\n").filter { it.isNotBlank() }
            } else {
                listOf(order.product)
            }
            
            productLines.sumOf { productLine ->
                // Count gallons - look for any water type (A, M, NR) or gallon indicators
                when {
                    productLine.contains(" A ") || productLine.contains(" A") || 
                    productLine.contains(" M ") || productLine.contains(" M") ||
                    productLine.contains(" NR ") || productLine.contains(" NR") ||
                    productLine.contains("Slim") || productLine.contains("Round") -> {
                        val quantityMatch = Regex("x(\\d+)").find(productLine)
                        quantityMatch?.groupValues?.get(1)?.toIntOrNull() ?: 1
                    }
                    else -> 0
                }
            }
        }
        
        // Calculate Alkaline and Mineral quantities
        var alkalineQty = 0
        var mineralQty = 0
        
        customerOrders.forEach { order ->
            val productLines = if (order.product.contains("\n")) {
                order.product.split("\n").filter { it.isNotBlank() }
            } else {
                listOf(order.product)
            }
            
            productLines.forEach { productLine ->
                val quantityMatch = Regex("x(\\d+)").find(productLine)
                val quantity = quantityMatch?.groupValues?.get(1)?.toIntOrNull() ?: 1
                
                when {
                    productLine.contains(" A ") || productLine.contains(" A") -> alkalineQty += quantity
                    productLine.contains(" M ") || productLine.contains(" M") -> mineralQty += quantity
                    productLine.contains(" NR ") || productLine.contains(" NR") -> {
                        // No Refill gallons count as Mineral for summary purposes
                        mineralQty += quantity
                    }
                }
            }
        }
        
        // Update quick summary
        quickTotalQtyText.text = totalQty.toString()
        quickRevenueText.text = String.format("₱%.2f", totalRevenue)
        
        // Update panel summary TextViews if they exist (for landscape layout)
        try {
            val quickTotalQtyPanel = findViewById<TextView>(R.id.quickTotalQtyPanel)
            val quickRevenuePanel = findViewById<TextView>(R.id.quickRevenuePanel)
            
            quickTotalQtyPanel?.text = totalQty.toString()
            quickRevenuePanel?.text = String.format("₱%.2f", totalRevenue)
        } catch (e: Exception) {
            // Panel views don't exist in current layout, ignore
        }
        
        // Update Alkaline and Mineral summary if the views exist
        try {
            val alkalineSummaryText = findViewById<TextView>(R.id.alkalineSummaryText)
            val mineralSummaryText = findViewById<TextView>(R.id.mineralSummaryText)
            val pendingCount = findViewById<TextView>(R.id.pendingCount)
            val deliveredCount = findViewById<TextView>(R.id.deliveredCount)
            val paidCount = findViewById<TextView>(R.id.paidCount)
            
            alkalineSummaryText?.text = alkalineQty.toString()
            mineralSummaryText?.text = mineralQty.toString()
            pendingCount?.text = customerOrders.count { it.status == "Pending" }.toString()
            deliveredCount?.text = customerOrders.count { it.status == "Delivered" }.toString()
            paidCount?.text = customerOrders.count { it.isPaid }.toString()
        } catch (e: Exception) {
            // Views might not exist in all layouts
            println("DEBUG: Summary views not found: ${e.message}")
        }
    }
    
    // TableLayout methods removed - using RecyclerView now

    private fun addCustomerOrderToDatabase(order: CustomerOrder) {
        try {
            println("DEBUG: addCustomerOrderToDatabase - Order total: ${order.total}")
            val success = orderDatabaseHelper.addCustomerOrder(order)
            println("DEBUG: addCustomerOrderToDatabase - Success: $success")
            
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
        
        // Set dialog window properties for better sizing
        val window = dialog.window
        window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.95).toInt(), // 95% of screen width
            android.view.WindowManager.LayoutParams.WRAP_CONTENT
        )

        // Initialize views
        val productTypeSpinner = dialog.findViewById<Spinner>(R.id.productTypeSpinner)
        val uomSpinner = dialog.findViewById<Spinner>(R.id.uomSpinner)
        val waterTypeGroup = dialog.findViewById<RadioGroup>(R.id.waterTypeGroup)
        val mineralRadio = dialog.findViewById<RadioButton>(R.id.mineralRadio)
        val alkalineRadio = dialog.findViewById<RadioButton>(R.id.alkalineRadio)
        val noRefillRadio = dialog.findViewById<RadioButton>(R.id.noRefillRadio)
        val accessoriesRow = dialog.findViewById<LinearLayout>(R.id.accessoriesRow)
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
                    accessoriesRow.visibility = View.GONE
                    
                    // Set UOM options for Water (all options)
                    val uomOptions = listOf("20L Slim", "10L Slim", "20L Round")
                    val uomAdapter = ArrayAdapter(this@Today, android.R.layout.simple_spinner_item, uomOptions)
                    uomAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    uomSpinner.adapter = uomAdapter
                    uomSpinner.setSelection(0)
                }
                "New Gallon" -> {
                    println("DEBUG: Setting New Gallon fields visible")
                    uomSpinner.visibility = View.VISIBLE
                    waterTypeGroup.visibility = View.VISIBLE
                    noRefillRadio.visibility = View.VISIBLE
                    accessoriesRow.visibility = View.GONE
                    
                    // Set UOM options for New Gallon (only 20L Slim and 20L Round)
                    val uomOptions = listOf("20L Slim", "20L Round")
                    val uomAdapter = ArrayAdapter(this@Today, android.R.layout.simple_spinner_item, uomOptions)
                    uomAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    uomSpinner.adapter = uomAdapter
                    uomSpinner.setSelection(0)
                }
                "Accessories" -> {
                    println("DEBUG: Setting Accessories fields visible")
                    uomSpinner.visibility = View.GONE
                    waterTypeGroup.visibility = View.GONE
                    noRefillRadio.visibility = View.GONE
                    accessoriesRow.visibility = View.VISIBLE
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
                    calculatePrice(waterTypeCode, uom, quantity, productType)
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
                    calculatePrice(waterTypeCode, uom, quantity, productType)
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
                        calculatePrice(waterTypeCode, uom, item.quantity, item.productType)
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
                        calculatePrice(waterTypeCode, uom, item.quantity, item.productType)
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
            
            // Only update UOM options for Water product type
            val currentProductType = productTypeSpinner.selectedItem.toString()
            if (currentProductType == "Water") {
                val availableUOMs = when (selectedWaterType) {
                    "Alkaline" -> listOf("20L Slim", "10L Slim")
                    "Mineral" -> listOf("20L Slim", "10L Slim", "20L Round")
                    "No Refill" -> listOf("20L Slim", "10L Slim", "20L Round")
                    else -> listOf("20L Slim", "10L Slim", "20L Round")
                }
                
                val uomAdapter = ArrayAdapter(this@Today, android.R.layout.simple_spinner_item, availableUOMs)
                uomAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                uomSpinner.adapter = uomAdapter
                
                // Set the first available UOM as selected
                uomSpinner.setSelection(0)
            }
            
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
                        
                        // Set UOM options for Water (all options)
                        val uomOptions = listOf("20L Slim", "10L Slim", "20L Round")
                        val uomAdapter = ArrayAdapter(this@Today, android.R.layout.simple_spinner_item, uomOptions)
                        uomAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        itemUomSpinner.adapter = uomAdapter
                        itemUomSpinner.setSelection(0)
                    }
                    "New Gallon" -> {
                        itemUomSpinner.visibility = View.VISIBLE
                        itemWaterTypeGroup.visibility = View.VISIBLE
                        itemNoRefillRadio.visibility = View.VISIBLE
                        itemAccessoriesRow.visibility = View.GONE
                        
                        // Set UOM options for New Gallon (only 20L Slim and 20L Round)
                        val uomOptions = listOf("20L Slim", "20L Round")
                        val uomAdapter = ArrayAdapter(this@Today, android.R.layout.simple_spinner_item, uomOptions)
                        uomAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        itemUomSpinner.adapter = uomAdapter
                        itemUomSpinner.setSelection(0)
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
                if (quantity < 30) {
                    quantity++
                    itemQuantityDisplay.text = quantity.toString()
                    // Update the order item data
                    val existingItem = additionalItems.find { it.view == itemView }
                    existingItem?.quantity = quantity
                    updateTotalPrice()
                }
            }
            
            // Update available UOM options based on water type selection
            itemWaterTypeGroup.setOnCheckedChangeListener { _, checkedId ->
                val selectedWaterType = when (checkedId) {
                    R.id.itemAlkalineRadio -> "Alkaline"
                    R.id.itemMineralRadio -> "Mineral"
                    R.id.itemNoRefillRadio -> "No Refill"
                    else -> "Mineral"
                }
                
                // Only update UOM options for Water product type
                val currentProductType = itemProductTypeSpinner.selectedItem.toString()
                if (currentProductType == "Water") {
                    val availableUOMs = when (selectedWaterType) {
                        "Alkaline" -> listOf("20L Slim", "10L Slim")
                        "Mineral" -> listOf("20L Slim", "10L Slim", "20L Round")
                        "No Refill" -> listOf("20L Slim", "10L Slim", "20L Round")
                        else -> listOf("20L Slim", "10L Slim", "20L Round")
                    }
                    
                    val itemUomAdapter = ArrayAdapter(this@Today, android.R.layout.simple_spinner_item, availableUOMs)
                    itemUomAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    itemUomSpinner.adapter = itemUomAdapter
                    
                    // Set the first available UOM as selected
                    itemUomSpinner.setSelection(0)
                }
                
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
                            val price = calculatePrice(waterTypeCode, uom, quantity, productType)
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
                            val price = calculatePrice(waterTypeCode, uom, quantity, productType)
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
                                val price = calculatePrice(waterTypeCode, uom, item.quantity, item.productType)
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
                                val price = calculatePrice(waterTypeCode, uom, item.quantity, item.productType)
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
                    
                    println("DEBUG: Add Order - Final totalPrice: $totalPrice")
                    println("DEBUG: Add Order - Product: $product")
                    
                    val order = CustomerOrder(
                        name = name,
                        address = address,
                        product = product,
                        total = totalPrice,
                        isPaid = isPaid,
                        status = "Pending", // New orders start as pending
                        isFromCustomerInterface = false, // Admin-created orders
                        userId = "" // Admin-created orders don't have userId
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

    private fun calculatePrice(waterType: String, uom: String, quantity: Int, productType: String = "Water"): Double {
        val result = when (productType) {
            "Water" -> {
                // Water pricing based on UOM and water type
                when (waterType) {
                    "A" -> { // Alkaline
                        val price = when (uom) {
                            "20L Slim" -> 50.0
                            "20L Round" -> 50.0
                            "10L Slim" -> 25.0
                            else -> 0.0
                        }
                        println("DEBUG: calculatePrice - Water Alkaline: $uom = $price, quantity = $quantity")
                        price * quantity
                    }
                    "M" -> { // Mineral
                        val price = when (uom) {
                            "20L Slim" -> 30.0
                            "20L Round" -> 30.0
                            "10L Slim" -> 15.0
                            else -> 0.0
                        }
                        println("DEBUG: calculatePrice - Water Mineral: $uom = $price, quantity = $quantity")
                        price * quantity
                    }
                    else -> {
                        println("DEBUG: calculatePrice - Unknown water type for Water: $waterType")
                        0.0
                    }
                }
            }
            "New Gallon" -> {
                // New Gallon pricing - always ₱200 regardless of water type
                val price = when (uom) {
                    "20L Slim" -> 200.0
                    "20L Round" -> 200.0
                    else -> 0.0
                }
                println("DEBUG: calculatePrice - New Gallon: $uom = $price, quantity = $quantity")
                price * quantity
            }
            else -> {
                println("DEBUG: calculatePrice - Unknown product type: $productType")
                0.0
            }
        }
        println("DEBUG: calculatePrice - Final result: $result")
        return result
    }

    private fun calculateAccessoryPrice(accessory: String, quantity: Int): Double {
        val accessoryPrices = mapOf(
            "Small Cap Cover" to 10.0,
            "Big Cap Cover" to 25.0,
            "Round Cap Cover" to 5.0,
            "Non Leak Cover" to 3.0
        )
        val price = accessoryPrices[accessory] ?: 0.0
        val total = price * quantity
        println("DEBUG: calculateAccessoryPrice - Accessory: '$accessory', Price: $price, Quantity: $quantity, Total: $total")
        println("DEBUG: calculateAccessoryPrice - Available accessories: ${accessoryPrices.keys}")
        return total
    }
}