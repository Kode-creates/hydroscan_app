package com.adamson.fhydroscan

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

class Sales : AppCompatActivity() {
    private lateinit var reportTypeSpinner: AutoCompleteTextView
    private lateinit var dateRangeText: TextView
    private lateinit var calendarButton: Button
    private lateinit var lineGraphPlaceholder: View
    private lateinit var salesSpikeQty: TextView
    private lateinit var salesSpikeUom: TextView
    private lateinit var salesSpikeRevenue: TextView
    private lateinit var topCustomerName: TextView
    private lateinit var totalRevenue: TextView
    private lateinit var unpaidInfo: TextView
    private lateinit var totalQuantity: TextView
    private lateinit var totalUom: TextView
    private lateinit var mostOrderedWater: TextView

    // Dummy data structure
    data class SalesData(
        val date: Calendar,
        val uom: String,
        val quantity: Int,
        val revenue: Double,
        val waterType: String,  // "Alkaline" or "Mineral"
        val customerName: String
    )

    // Generate dummy data for demonstration
    private val dummyData = generateDummyData()
    private var selectedDate = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sales)

        // Initialize views
        initializeViews()
        
        // Set up listeners
        setupReportTypeSpinner()
        setupCalendarButton()
        setupBottomNavigation()

        // Show initial report
        generateReport(Calendar.getInstance())
    }

    private fun initializeViews() {
        reportTypeSpinner = findViewById(R.id.reportTypeSpinner)
        dateRangeText = findViewById(R.id.dateRangeText)
        calendarButton = findViewById(R.id.calendarButton)
        lineGraphPlaceholder = findViewById(R.id.lineGraphPlaceholder)
        salesSpikeQty = findViewById(R.id.salesSpikeQty)
        salesSpikeUom = findViewById(R.id.salesSpikeUom)
        salesSpikeRevenue = findViewById(R.id.salesSpikeRevenue)
        topCustomerName = findViewById(R.id.topCustomerName)
        totalRevenue = findViewById(R.id.totalRevenue)
        unpaidInfo = findViewById(R.id.unpaidInfo)
        totalQuantity = findViewById(R.id.totalQuantity)
        totalUom = findViewById(R.id.totalUom)
        mostOrderedWater = findViewById(R.id.mostOrderedWater)
    }

    private fun setupReportTypeSpinner() {
        val reportTypes = arrayOf("Daily", "Weekly", "Monthly", "Yearly")
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, reportTypes)
        reportTypeSpinner.setAdapter(adapter)
        reportTypeSpinner.setText("Weekly", false) // Set default selection
        
        reportTypeSpinner.setOnItemClickListener { _, _, position, _ ->
            generateReport(selectedDate)
        }
    }

    private fun setupCalendarButton() {
        calendarButton.setOnClickListener {
            val datePickerDialog = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    selectedDate = Calendar.getInstance().apply {
                        set(year, month, dayOfMonth)
                    }
                    generateReport(selectedDate)
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
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

    private fun generateDummyData(): List<SalesData> {
        val data = mutableListOf<SalesData>()
        val calendar = Calendar.getInstance()
        val random = Random()
        val waterTypes = listOf("Alkaline", "Mineral")
        val customerNames = listOf("Michelle Obama", "John Smith", "Sarah Johnson", "Mike Wilson", "Lisa Brown")

        // Generate data for the last 365 days
        repeat(365) {
            // Generate 2-4 entries per day
            repeat(random.nextInt(3) + 2) {
                data.add(
                    SalesData(
                        date = calendar.clone() as Calendar,
                        uom = listOf("20L", "10L", "7L", "6L")[random.nextInt(4)],
                        quantity = random.nextInt(10) + 1,
                        revenue = (random.nextInt(1000) + 500).toDouble(),
                        waterType = waterTypes[random.nextInt(2)],
                        customerName = customerNames[random.nextInt(customerNames.size)]
                    )
                )
            }
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        return data
    }

    private fun generateReport(selectedDate: Calendar) {
        val reportType = when (reportTypeSpinner.text.toString()) {
            "Daily" -> ReportType.DAILY
            "Weekly" -> ReportType.WEEKLY
            "Monthly" -> ReportType.MONTHLY
            "Yearly" -> ReportType.YEARLY
            else -> ReportType.WEEKLY
        }

        val filteredData = filterDataByDateRange(selectedDate, reportType)
        
        // Update date range display
        updateDateRangeDisplay(selectedDate, reportType)
        
        // Calculate and display statistics
        updateStatistics(filteredData)
        
        // Calculate and display sales spike data
        updateSalesSpikeData(filteredData)
        
        // Calculate and display top customer
        updateTopCustomer(filteredData)
    }

    private fun filterDataByDateRange(selectedDate: Calendar, reportType: ReportType): List<SalesData> {
        val startDate = Calendar.getInstance().apply {
            timeInMillis = selectedDate.timeInMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        val endDate = startDate.clone() as Calendar
        
        when (reportType) {
            ReportType.DAILY -> {
                endDate.add(Calendar.DAY_OF_YEAR, 1)
            }
            ReportType.WEEKLY -> {
                startDate.set(Calendar.DAY_OF_WEEK, startDate.firstDayOfWeek)
                endDate.timeInMillis = startDate.timeInMillis
                endDate.add(Calendar.WEEK_OF_YEAR, 1)
            }
            ReportType.MONTHLY -> {
                startDate.set(Calendar.DAY_OF_MONTH, 1)
                endDate.timeInMillis = startDate.timeInMillis
                endDate.add(Calendar.MONTH, 1)
            }
            ReportType.YEARLY -> {
                startDate.set(Calendar.DAY_OF_YEAR, 1)
                endDate.timeInMillis = startDate.timeInMillis
                endDate.add(Calendar.YEAR, 1)
            }
        }

        return dummyData.filter { 
            it.date.timeInMillis >= startDate.timeInMillis && 
            it.date.timeInMillis < endDate.timeInMillis 
        }
    }

    private fun updateDateRangeDisplay(date: Calendar, reportType: ReportType) {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val dateRange = when (reportType) {
            ReportType.DAILY -> dateFormat.format(date.time)
            ReportType.WEEKLY -> {
                val start = date.clone() as Calendar
                start.set(Calendar.DAY_OF_WEEK, start.firstDayOfWeek)
                val end = start.clone() as Calendar
                end.add(Calendar.DAY_OF_YEAR, 6)
                "${dateFormat.format(start.time)} - ${dateFormat.format(end.time)}"
            }
            ReportType.MONTHLY -> {
                val start = date.clone() as Calendar
                start.set(Calendar.DAY_OF_MONTH, 1)
                val end = start.clone() as Calendar
                end.add(Calendar.MONTH, 1)
                end.add(Calendar.DAY_OF_YEAR, -1)
                "${dateFormat.format(start.time)} - ${dateFormat.format(end.time)}"
            }
            ReportType.YEARLY -> {
                val start = date.clone() as Calendar
                start.set(Calendar.DAY_OF_YEAR, 1)
                val end = start.clone() as Calendar
                end.add(Calendar.YEAR, 1)
                end.add(Calendar.DAY_OF_YEAR, -1)
                "${dateFormat.format(start.time)} - ${dateFormat.format(end.time)}"
            }
        }
        dateRangeText.text = dateRange
    }

    private fun updateStatistics(data: List<SalesData>) {
        val totalRev = data.sumOf { it.revenue }
        val totalQty = data.sumOf { it.quantity }
        val totalUomValue = data.sumOf { it.uom.replace("L", "").toIntOrNull() ?: 0 }
        val mostOrdered = data.groupBy { it.waterType }
            .mapValues { it.value.sumOf { sale -> sale.quantity } }
            .maxByOrNull { it.value }?.key ?: "N/A"
        
        // Calculate unpaid (simplified - assume 10% of orders are unpaid)
        val unpaidCount = (data.size * 0.1).toInt()
        val unpaidCustomers = data.take(unpaidCount).map { it.customerName }.distinct()
        
        totalRevenue.text = String.format("₱%.2f", totalRev)
        totalQuantity.text = totalQty.toString()
        totalUom.text = "${totalUomValue}L"
        mostOrderedWater.text = mostOrdered
        
        if (unpaidCount > 0) {
            unpaidInfo.text = "$unpaidCount qty, ${unpaidCustomers.joinToString(", ")}"
        } else {
            unpaidInfo.text = "0 qty, No customers"
        }
    }

    private fun updateSalesSpikeData(data: List<SalesData>) {
        if (data.isEmpty()) {
            salesSpikeQty.text = "0 Gallons (N/A)"
            salesSpikeUom.text = "0L (N/A)"
            salesSpikeRevenue.text = "₱0.00 (N/A)"
            return
        }

        // Find highest quantity day
        val qtyByDay = data.groupBy { 
            val cal = it.date.clone() as Calendar
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal
        }.mapValues { it.value.sumOf { sale -> sale.quantity } }
        
        val maxQtyDay = qtyByDay.maxByOrNull { it.value }
        val qtyDayName = if (maxQtyDay != null) {
            val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
            dayFormat.format(maxQtyDay.key.time)
        } else "N/A"
        
        // Find highest UOM day
        val uomByDay = data.groupBy { 
            val cal = it.date.clone() as Calendar
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal
        }.mapValues { it.value.sumOf { sale -> sale.uom.replace("L", "").toIntOrNull() ?: 0 } }
        
        val maxUomDay = uomByDay.maxByOrNull { it.value }
        val uomDayName = if (maxUomDay != null) {
            val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
            dayFormat.format(maxUomDay.key.time)
        } else "N/A"
        
        // Find highest revenue day
        val revenueByDay = data.groupBy { 
            val cal = it.date.clone() as Calendar
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal
        }.mapValues { it.value.sumOf { sale -> sale.revenue } }
        
        val maxRevenueDay = revenueByDay.maxByOrNull { it.value }
        val revenueDayName = if (maxRevenueDay != null) {
            val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
            dayFormat.format(maxRevenueDay.key.time)
        } else "N/A"
        
        salesSpikeQty.text = "${maxQtyDay?.value ?: 0} Gallons ($qtyDayName)"
        salesSpikeUom.text = "${maxUomDay?.value ?: 0}L ($uomDayName)"
        salesSpikeRevenue.text = "₱${String.format("%.2f", maxRevenueDay?.value ?: 0.0)} ($revenueDayName)"
    }

    private fun updateTopCustomer(data: List<SalesData>) {
        if (data.isEmpty()) {
            topCustomerName.text = "No Data"
            return
        }

        val customerTotals = data.groupBy { it.customerName }
            .mapValues { it.value.sumOf { sale -> sale.quantity } }
        
        val topCustomer = customerTotals.maxByOrNull { it.value }
        topCustomerName.text = topCustomer?.key ?: "No Data"
    }

    enum class ReportType {
        DAILY, WEEKLY, MONTHLY, YEARLY
    }
}