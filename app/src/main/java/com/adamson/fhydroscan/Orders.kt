package com.adamson.fhydroscan

import android.app.DatePickerDialog
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.adamson.fhydroscan.data.DateOrderSummary
import com.adamson.fhydroscan.database.OrderDatabaseHelper
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class Orders : AppCompatActivity() {

    private lateinit var ordersRecyclerView: RecyclerView
    private lateinit var dateCardAdapter: DateCardAdapter
    private lateinit var orderDatabaseHelper: OrderDatabaseHelper
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var emptyOrdersText: TextView
    private lateinit var exportButton: Button
    private lateinit var monthLabel: TextView
    private val dateSummaries = mutableListOf<DateOrderSummary>()
    private var selectedFromDate: Calendar = Calendar.getInstance()
    private var selectedToDate: Calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_orders)

        // Initialize database and shared preferences
        orderDatabaseHelper = OrderDatabaseHelper(this)
        sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)

        // Initialize views
        ordersRecyclerView = findViewById(R.id.ordersRecyclerView)
        emptyOrdersText = findViewById(R.id.emptyOrdersText)
        exportButton = findViewById(R.id.exportButton)
        monthLabel = findViewById(R.id.monthLabel)

        // Setup RecyclerView
        setupRecyclerView()
        
        // Setup export button
        setupExportButton()
        
        // Check for new day and process previous day's data
        checkAndProcessNewDay()
        
        // Load order history
        loadOrderHistory()

        setupBottomNavigation()
    }

    override fun onResume() {
        super.onResume()
        // Check for new day and process data when app resumes
        checkAndProcessNewDay()
        // Refresh the display
        loadOrderHistory()
    }

    private fun setupRecyclerView() {
        dateCardAdapter = DateCardAdapter(dateSummaries) { dateSummary ->
            // Navigate to PreviousOrders activity with selected date
            val intent = Intent(this, PreviousOrders::class.java)
            intent.putExtra("selected_date", dateSummary.date)
            startActivity(intent)
        }
        
        ordersRecyclerView.layoutManager = LinearLayoutManager(this)
        ordersRecyclerView.adapter = dateCardAdapter
    }

    private fun loadOrderHistory() {
        try {
            // Get current user ID
            val currentUserId = sharedPreferences.getString("current_user_id", "") ?: ""
            if (currentUserId.isEmpty()) {
                showEmptyState("User not logged in")
                return
            }

            // First try to load from daily summaries
            val summaries = orderDatabaseHelper.getDailySummaries(currentUserId)
            
            if (summaries.isNotEmpty()) {
                // Use stored daily summaries
                println("DEBUG: Loaded ${summaries.size} daily summaries from database")
                dateCardAdapter.updateDateSummaries(summaries)
                updateMonthLabel(summaries)
                hideEmptyState()
            } else {
                // Fallback to creating summaries from raw order data
                val orderDates = orderDatabaseHelper.getOrderDates(currentUserId)
                println("DEBUG: Found ${orderDates.size} dates with orders")

                if (orderDates.isEmpty()) {
                    showEmptyState("No order history available")
                    return
                }

                // Create DateOrderSummary for each date
                val newSummaries = mutableListOf<DateOrderSummary>()
                for (date in orderDates) {
                    val summary = createDateOrderSummary(currentUserId, date)
                    if (summary != null) {
                        newSummaries.add(summary)
                        // Store the summary for future use
                        storeDailySummary(summary)
                    }
                }

                // Update adapter
                dateCardAdapter.updateDateSummaries(newSummaries)
                
                // Update month label based on data
                updateMonthLabel(newSummaries)
                
                // Show/hide empty state
                if (newSummaries.isEmpty()) {
                    showEmptyState("No order history available")
                } else {
                    hideEmptyState()
                }
            }

        } catch (e: Exception) {
            println("DEBUG: Error loading order history: ${e.message}")
            e.printStackTrace()
            showEmptyState("Error loading order history")
        }
    }

    private fun createDateOrderSummary(userId: String, date: String): DateOrderSummary? {
        return try {
            // Get orders for this date
            val orders = orderDatabaseHelper.getOrdersByDate(userId, date)
            if (orders.isEmpty()) return null

            // Convert to CustomerOrder format for calculations
            val customerOrders = orders.map { order ->
                val productString = order.items.joinToString("\n") { item ->
                    "${item.uom} ${item.waterType} x${item.quantity}"
                }
                
                CustomerOrder(
                    name = order.items.firstOrNull()?.customerName ?: "Unknown",
                    address = order.items.firstOrNull()?.customerAddress ?: "",
                    product = productString,
                    total = order.totalAmount,
                    isPaid = order.status == com.adamson.fhydroscan.data.OrderStatus.DELIVERED,
                    status = when (order.status) {
                        com.adamson.fhydroscan.data.OrderStatus.DELIVERED -> "Delivered"
                        com.adamson.fhydroscan.data.OrderStatus.PROCESSING -> "Pending"
                        com.adamson.fhydroscan.data.OrderStatus.CANCELLED -> "Cancelled"
                    },
                    isFromCustomerInterface = false,
                    userId = order.userId
                )
            }

            // Calculate totals
            val totalUom = calculateTotalUom(customerOrders)
            val totalSales = customerOrders.sumOf { it.total }
            val totalOrders = customerOrders.size

            // Format display date
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val displayFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val displayDate = try {
                val parsedDate = dateFormat.parse(date)
                displayFormat.format(parsedDate!!)
            } catch (e: Exception) {
                date
            }

            DateOrderSummary(
                date = date,
                displayDate = displayDate,
                totalUom = totalUom,
                totalSales = totalSales,
                totalOrders = totalOrders,
                orders = customerOrders
            )

        } catch (e: Exception) {
            println("DEBUG: Error creating date summary for $date: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    private fun calculateTotalUom(customerOrders: List<CustomerOrder>): Int {
        var totalUomValue = 0
        
        customerOrders.forEach { order ->
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
        
        return totalUomValue
    }

    private fun showEmptyState(message: String) {
        emptyOrdersText.text = message
        emptyOrdersText.visibility = View.VISIBLE
        ordersRecyclerView.visibility = View.GONE
    }

    private fun hideEmptyState() {
        emptyOrdersText.visibility = View.GONE
        ordersRecyclerView.visibility = View.VISIBLE
    }

    private fun updateMonthLabel(summaries: List<DateOrderSummary>) {
        if (summaries.isEmpty()) {
            monthLabel.text = "No Orders"
            return
        }

        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val displayFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            
            // Get all unique months from the summaries
            val months = summaries.mapNotNull { summary ->
                try {
                    val parsedDate = dateFormat.parse(summary.date)
                    displayFormat.format(parsedDate!!)
                } catch (e: Exception) {
                    null
                }
            }.distinct().sorted()
            
            when {
                months.isEmpty() -> {
                    // Fallback to current month
                    val currentMonth = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())
                    monthLabel.text = currentMonth
                }
                months.size == 1 -> {
                    // Single month
                    monthLabel.text = months.first()
                }
                else -> {
                    // Multiple months - show range
                    val firstMonth = months.first()
                    val lastMonth = months.last()
                    monthLabel.text = if (firstMonth == lastMonth) {
                        firstMonth
                    } else {
                        "$firstMonth - $lastMonth"
                    }
                }
            }
        } catch (e: Exception) {
            // Fallback to current month if any error occurs
            val currentMonth = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())
            monthLabel.text = currentMonth
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

    private fun setupExportButton() {
        exportButton.setOnClickListener {
            showDateRangeDialog()
        }
    }

    private fun showDateRangeDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_date_range_export, null)
        
        val fromDateButton = dialogView.findViewById<Button>(R.id.fromDateButton)
        val toDateButton = dialogView.findViewById<Button>(R.id.toDateButton)
        val selectedDateRangeText = dialogView.findViewById<TextView>(R.id.selectedDateRangeText)
        val exportButton = dialogView.findViewById<Button>(R.id.exportButton)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)
        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        
        // Set initial dates (last 30 days)
        selectedToDate = Calendar.getInstance()
        selectedFromDate = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -30)
        }
        
        updateDateRangeText(selectedDateRangeText)
        updateButtonTexts(fromDateButton, toDateButton)
        
        fromDateButton.setOnClickListener {
            showDatePicker(selectedFromDate) { calendar ->
                selectedFromDate = calendar
                updateDateRangeText(selectedDateRangeText)
                updateButtonTexts(fromDateButton, toDateButton)
            }
        }
        
        toDateButton.setOnClickListener {
            showDatePicker(selectedToDate) { calendar ->
                selectedToDate = calendar
                updateDateRangeText(selectedDateRangeText)
                updateButtonTexts(fromDateButton, toDateButton)
            }
        }
        
        exportButton.setOnClickListener {
            dialog.dismiss()
            exportToExcel()
        }
        
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }

    private fun updateDateRangeText(textView: TextView) {
        try {
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val fromDateStr = dateFormat.format(selectedFromDate.time)
            val toDateStr = dateFormat.format(selectedToDate.time)
            textView.text = "Exporting from $fromDateStr to $toDateStr"
            textView.visibility = View.VISIBLE
        } catch (e: Exception) {
            // Hide the text if there's an error formatting dates
            textView.visibility = View.GONE
        }
    }
    
    private fun updateButtonTexts(fromButton: Button, toButton: Button) {
        try {
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val fromDateStr = dateFormat.format(selectedFromDate.time)
            val toDateStr = dateFormat.format(selectedToDate.time)
            
            fromButton.text = "From: $fromDateStr"
            toButton.text = "To: $toDateStr"
        } catch (e: Exception) {
            // Fallback to default text if there's an error
            fromButton.text = "Select Start Date"
            toButton.text = "Select End Date"
        }
    }

    private fun showDatePicker(initialDate: Calendar, onDateSelected: (Calendar) -> Unit) {
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedDate = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }
                onDateSelected(selectedDate)
            },
            initialDate.get(Calendar.YEAR),
            initialDate.get(Calendar.MONTH),
            initialDate.get(Calendar.DAY_OF_MONTH)
        )
        // Restrict to current date and earlier
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    private fun exportToExcel() {
        try {
            val fileName = "Orders_Export_${System.currentTimeMillis()}.csv"
            val file = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
            
            val csvContent = StringBuilder()
            
            // Get current user ID
            val currentUserId = sharedPreferences.getString("current_user_id", "") ?: ""
            if (currentUserId.isEmpty()) {
                showToast("User not logged in")
                return
            }
            
            // Format dates for database query
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val fromDateStr = dateFormat.format(selectedFromDate.time)
            val toDateStr = dateFormat.format(selectedToDate.time)
            
            // Get all dates in range
            val allDates = orderDatabaseHelper.getOrderDates(currentUserId)
                .filter { date ->
                    date >= fromDateStr && date <= toDateStr
                }
                .sorted()
            
            if (allDates.isEmpty()) {
                showToast("No orders found in selected date range")
                return
            }
            
            // Export each date as a separate table
            for (date in allDates) {
                val orders = orderDatabaseHelper.getOrdersByDate(currentUserId, date)
                if (orders.isNotEmpty()) {
                    exportDateToCsv(csvContent, date, orders)
                }
            }
            
            // Write to file
            file.writeText(csvContent.toString())
            
            // Share the file
            shareFile(file, "text/csv")
            
            showToast("Excel file exported successfully!")
            
        } catch (e: Exception) {
            println("DEBUG: Error exporting to Excel: ${e.message}")
            e.printStackTrace()
            showToast("Error creating Excel file: ${e.message}")
        }
    }

    private fun exportDateToCsv(csvContent: StringBuilder, date: String, orders: List<com.adamson.fhydroscan.data.Order>) {
        // Format display date as MM/DD/YYYY
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val displayFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        val displayDate = try {
            val parsedDate = dateFormat.parse(date)
            displayFormat.format(parsedDate!!)
        } catch (e: Exception) {
            date
        }
        
        // Add DATE header (merged columns - represented as single cell)
        csvContent.appendLine("DATE:,$displayDate")
        csvContent.appendLine() // Empty line
        
        // Add column headers
        csvContent.appendLine("CUSTOMER,PRODUCT,UOM,QTY,TOTAL")
        
        // Process order data
        var totalRevenue = 0.0
        var totalUom = 0
        
        for (order in orders) {
            for (item in order.items) {
                // Map product names according to specifications
                val productName = mapProductName(item.waterType, item.uom, item.productName)
                
                // Determine UOM based on product type
                val uom = determineUom(productName, item.uom)
                
                // Calculate UOM value for total calculation
                val uomValue = if (uom == "N/A") 0 else item.uom.replace("L", "").toIntOrNull() ?: 0
                totalUom += uomValue * item.quantity
                
                // Add order data row
                csvContent.appendLine("${item.customerName},$productName,$uom,${item.quantity},${String.format("%.2f", item.price)}")
                totalRevenue += item.price
            }
        }
        
        csvContent.appendLine() // Empty line
        
        // Add Total Revenue (merged columns)
        csvContent.appendLine("Total Revenue:,${String.format("%.2f", totalRevenue)}")
        
        // Add Total UOM (merged columns)
        csvContent.appendLine("Total UOM:,$totalUom")
        
        csvContent.appendLine() // Empty line between dates
    }
    
    private fun mapProductName(waterType: String, uom: String, originalProductName: String): String {
        return when {
            // Water types
            waterType == "Mineral" -> "Mineral"
            waterType == "Alkaline" -> "Alkaline"
            
            // Gallon types (based on UOM and water type)
            uom == "20L" && waterType == "Mineral" -> "Round Gallon (New)"
            uom == "20L" && waterType == "Alkaline" -> "Round Gallon (New)"
            uom == "10L" && waterType == "Mineral" -> "Slim Gallon (New)"
            uom == "10L" && waterType == "Alkaline" -> "Slim Gallon (New)"
            
            // Accessories (based on original product name)
            originalProductName.contains("Big Cap Cover", ignoreCase = true) -> "Big cap Cover"
            originalProductName.contains("Small Cap Cover", ignoreCase = true) -> "Small cap Cover"
            originalProductName.contains("Round Cap Cover", ignoreCase = true) -> "Round Cover"
            originalProductName.contains("Non Leak Cover", ignoreCase = true) -> "Non-leak Cover"
            
            // Fallback to water type if no specific match
            else -> waterType
        }
    }
    
    private fun determineUom(productName: String, originalUom: String): String {
        return when (productName) {
            "Mineral", "Alkaline" -> originalUom // 20L or 10L
            "Round Gallon (New)", "Slim Gallon (New)" -> "20L"
            "Big cap Cover", "Small cap Cover", "Round Cover", "Non-leak Cover" -> "N/A"
            else -> originalUom
        }
    }

    private fun shareFile(file: File, mimeType: String) {
        try {
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )
            
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            startActivity(Intent.createChooser(shareIntent, "Share Excel File"))
            
        } catch (e: Exception) {
            println("DEBUG: Error sharing file: ${e.message}")
            e.printStackTrace()
            showToast("Error sharing file: ${e.message}")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // Store daily summary to database
    private fun storeDailySummary(summary: DateOrderSummary) {
        try {
            val currentUserId = sharedPreferences.getString("current_user_id", "") ?: ""
            if (currentUserId.isEmpty()) {
                println("DEBUG: No user ID found for storing daily summary")
                return
            }

            val success = orderDatabaseHelper.storeDailySummary(
                currentUserId,
                summary.date,
                summary.displayDate,
                summary.totalUom,
                summary.totalSales,
                summary.totalOrders
            )

            if (success) {
                println("DEBUG: Daily summary stored successfully for ${summary.date}")
            } else {
                println("DEBUG: Failed to store daily summary for ${summary.date}")
            }

        } catch (e: Exception) {
            println("DEBUG: Error storing daily summary: ${e.message}")
            e.printStackTrace()
        }
    }

    // Process and store daily order data for a specific date
    fun processDailyOrderData(date: String) {
        try {
            val currentUserId = sharedPreferences.getString("current_user_id", "") ?: ""
            if (currentUserId.isEmpty()) {
                println("DEBUG: No user ID found for daily data processing")
                return
            }

            // Check if summary already exists
            if (orderDatabaseHelper.hasDailySummary(currentUserId, date)) {
                println("DEBUG: Daily summary already exists for $date")
                return
            }

            // Get all orders for the specified date
            val orders = orderDatabaseHelper.getOrdersByDate(currentUserId, date)
            if (orders.isEmpty()) {
                println("DEBUG: No orders found for date: $date")
                return
            }

            // Create daily summary
            val summary = createDateOrderSummary(currentUserId, date)
            if (summary != null) {
                // Store daily summary
                storeDailySummary(summary)
                println("DEBUG: Daily summary created for $date: ${summary.totalOrders} orders, ₱${summary.totalSales}, ${summary.totalUom}L")
                
                // Refresh the order history display
                loadOrderHistory()
            }

        } catch (e: Exception) {
            println("DEBUG: Error processing daily order data for $date: ${e.message}")
            e.printStackTrace()
        }
    }

    // Process today's orders and prepare for storage
    fun processTodaysOrders() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        processDailyOrderData(today)
    }

    // Check if it's a new day and process previous day's data
    fun checkAndProcessNewDay() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val lastProcessedDate = sharedPreferences.getString("last_processed_date", "")
        
        if (lastProcessedDate != today) {
            // Process previous day's data if it exists
            if (!lastProcessedDate.isNullOrEmpty()) {
                processDailyOrderData(lastProcessedDate)
            }
            
            // Update last processed date
            sharedPreferences.edit()
                .putString("last_processed_date", today)
                .apply()
        }
    }

    // Manual method to process all pending daily data
    fun processAllPendingDailyData() {
        try {
            val currentUserId = sharedPreferences.getString("current_user_id", "") ?: ""
            if (currentUserId.isEmpty()) {
                println("DEBUG: No user ID found for processing pending data")
                return
            }

            // Get all dates with orders that don't have summaries yet
            val orderDates = orderDatabaseHelper.getOrderDates(currentUserId)
            val processedDates = mutableListOf<String>()

            for (date in orderDates) {
                if (!orderDatabaseHelper.hasDailySummary(currentUserId, date)) {
                    processDailyOrderData(date)
                    processedDates.add(date)
                }
            }

            if (processedDates.isNotEmpty()) {
                println("DEBUG: Processed ${processedDates.size} pending daily summaries")
                showToast("Processed ${processedDates.size} daily summaries")
            } else {
                println("DEBUG: No pending daily data to process")
                showToast("All daily data is up to date")
            }

        } catch (e: Exception) {
            println("DEBUG: Error processing pending daily data: ${e.message}")
            e.printStackTrace()
            showToast("Error processing daily data")
        }
    }
}