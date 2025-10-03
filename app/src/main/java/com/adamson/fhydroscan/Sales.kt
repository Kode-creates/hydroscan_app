package com.adamson.fhydroscan

import android.app.DatePickerDialog
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

class Sales : AppCompatActivity() {
    private lateinit var selectDateButton: Button
    private lateinit var selectedDateText: TextView
    private lateinit var exportButton: Button
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
    private var selectedPeriodType = "Week"
    private var selectedFromDate: Calendar = Calendar.getInstance()
    private var selectedToDate: Calendar = Calendar.getInstance()
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sales)

        // Initialize shared preferences
        sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)

        // Initialize views
        initializeViews()
        
        // Set up listeners
        setupSelectDateButton()
        setupExportButton()
        setupBottomNavigation()

        // Show initial report
        updateSelectedDateText()
        generateReport(Calendar.getInstance())
    }

    private fun initializeViews() {
        selectDateButton = findViewById(R.id.selectDateButton)
        selectedDateText = findViewById(R.id.selectedDateText)
        exportButton = findViewById(R.id.exportButton)
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


    private fun setupSelectDateButton() {
        selectDateButton.setOnClickListener {
            showCalendarSelectorDialog()
        }
    }

    private fun showCalendarSelectorDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_calendar_selector, null)
        
        val periodTypeSpinner = dialogView.findViewById<AutoCompleteTextView>(R.id.periodTypeSpinner)
        val calendarView = dialogView.findViewById<CalendarView>(R.id.calendarView)
        val monthYearSpinnerContainer = dialogView.findViewById<LinearLayout>(R.id.monthYearSpinnerContainer)
        val monthSpinner = dialogView.findViewById<AutoCompleteTextView>(R.id.monthSpinner)
        val yearSpinner = dialogView.findViewById<AutoCompleteTextView>(R.id.yearSpinner)
        val selectedDateDisplay = dialogView.findViewById<TextView>(R.id.selectedDateDisplay)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)
        val doneButton = dialogView.findViewById<Button>(R.id.doneButton)
        
        // Setup period type dropdown
        val periodTypes = arrayOf("Day", "Week", "Month", "Year")
        val periodAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, periodTypes)
        periodTypeSpinner.setAdapter(periodAdapter)
        periodTypeSpinner.setText(selectedPeriodType, false)
        
        // Setup month spinner
        val months = arrayOf("January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December")
        val monthAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, months)
        monthSpinner.setAdapter(monthAdapter)
        monthSpinner.setText(months[selectedDate.get(Calendar.MONTH)], false)
        
        // Setup year spinner
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val years = (currentYear - 5..currentYear + 5).map { it.toString() }.toTypedArray()
        val yearAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, years)
        yearSpinner.setAdapter(yearAdapter)
        yearSpinner.setText(selectedDate.get(Calendar.YEAR).toString(), false)
        
        // Set calendar to current selected date
        calendarView.date = selectedDate.timeInMillis
        
        // Show/hide appropriate views based on current selection
        updateDialogViews(selectedPeriodType, calendarView, monthYearSpinnerContainer, monthSpinner, yearSpinner)
        
        // Update selected date display when period type changes
        periodTypeSpinner.setOnItemClickListener { _, _, position, _ ->
            val tempPeriodType = periodTypes[position]
            updateDialogViews(tempPeriodType, calendarView, monthYearSpinnerContainer, monthSpinner, yearSpinner)
            updateSelectedDateDisplay(selectedDate, tempPeriodType, selectedDateDisplay)
        }
        
        // Update selected date display when calendar date changes
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val tempDate = Calendar.getInstance().apply {
                        set(year, month, dayOfMonth)
            }
            val currentPeriodType = periodTypeSpinner.text.toString()
            updateSelectedDateDisplay(tempDate, currentPeriodType, selectedDateDisplay)
        }
        
        // Update selected date display when month changes
        monthSpinner.setOnItemClickListener { _, _, position, _ ->
            val tempDate = Calendar.getInstance().apply {
                set(Calendar.YEAR, yearSpinner.text.toString().toInt())
                set(Calendar.MONTH, position)
                set(Calendar.DAY_OF_MONTH, 1)
            }
            val currentPeriodType = periodTypeSpinner.text.toString()
            updateSelectedDateDisplay(tempDate, currentPeriodType, selectedDateDisplay)
        }
        
        // Update selected date display when year changes
        yearSpinner.setOnItemClickListener { _, _, position, _ ->
            val tempDate = Calendar.getInstance().apply {
                set(Calendar.YEAR, years[position].toInt())
                set(Calendar.MONTH, monthSpinner.text.toString().let { monthName ->
                    months.indexOf(monthName)
                })
                set(Calendar.DAY_OF_MONTH, 1)
            }
            val currentPeriodType = periodTypeSpinner.text.toString()
            updateSelectedDateDisplay(tempDate, currentPeriodType, selectedDateDisplay)
        }
        
        // Show initial selected date display
        updateSelectedDateDisplay(selectedDate, selectedPeriodType, selectedDateDisplay)
        selectedDateDisplay.visibility = View.VISIBLE
        
        // Create and show dialog
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }
        
        doneButton.setOnClickListener {
            // Update selected date and period type
            selectedPeriodType = periodTypeSpinner.text.toString()
            
            // Update selected date based on period type
            selectedDate = when (selectedPeriodType) {
                "Month" -> {
                    Calendar.getInstance().apply {
                        set(Calendar.YEAR, yearSpinner.text.toString().toInt())
                        set(Calendar.MONTH, months.indexOf(monthSpinner.text.toString()))
                        set(Calendar.DAY_OF_MONTH, 1)
                    }
                }
                "Year" -> {
                    Calendar.getInstance().apply {
                        set(Calendar.YEAR, yearSpinner.text.toString().toInt())
                        set(Calendar.MONTH, 0)
                        set(Calendar.DAY_OF_MONTH, 1)
                    }
                }
                else -> {
                    Calendar.getInstance().apply {
                        timeInMillis = calendarView.date
                    }
                }
            }
            
            // Update the selected date text display
            updateSelectedDateText()
            
            // Generate report with new selection
                    generateReport(selectedDate)
            
            dialog.dismiss()
        }
        
        dialog.show()
    }

    private fun updateDialogViews(
        periodType: String,
        calendarView: CalendarView,
        monthYearSpinnerContainer: LinearLayout,
        monthSpinner: AutoCompleteTextView,
        yearSpinner: AutoCompleteTextView
    ) {
        when (periodType) {
            "Day", "Week" -> {
                calendarView.visibility = View.VISIBLE
                monthYearSpinnerContainer.visibility = View.GONE
            }
            "Month" -> {
                calendarView.visibility = View.GONE
                monthYearSpinnerContainer.visibility = View.VISIBLE
                monthSpinner.visibility = View.VISIBLE
                yearSpinner.visibility = View.VISIBLE
            }
            "Year" -> {
                calendarView.visibility = View.GONE
                monthYearSpinnerContainer.visibility = View.VISIBLE
                monthSpinner.visibility = View.GONE
                yearSpinner.visibility = View.VISIBLE
            }
        }
        
        // Force layout update to prevent overlapping
        calendarView.parent?.let { parent ->
            if (parent is ViewGroup) {
                parent.requestLayout()
            }
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

    private fun updateSelectedDateDisplay(date: Calendar, periodType: String, displayView: TextView) {
        val displayText = when (periodType) {
            "Day" -> {
                val dayFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
                "Selected: ${dayFormat.format(date.time)}"
            }
            "Week" -> {
                val start = date.clone() as Calendar
                start.set(Calendar.DAY_OF_WEEK, start.firstDayOfWeek)
                val end = start.clone() as Calendar
                end.add(Calendar.DAY_OF_YEAR, 6)
                val weekFormat = SimpleDateFormat("MMMM dd", Locale.getDefault())
                val yearFormat = SimpleDateFormat("yyyy", Locale.getDefault())
                "Selected: ${weekFormat.format(start.time)}-${weekFormat.format(end.time)}, ${yearFormat.format(date.time)}"
            }
            "Month" -> {
                val monthFormat = SimpleDateFormat("MMMM", Locale.getDefault())
                "Selected: ${monthFormat.format(date.time)}"
            }
            "Year" -> {
                val yearFormat = SimpleDateFormat("yyyy", Locale.getDefault())
                "Selected: ${yearFormat.format(date.time)}"
            }
            else -> "Selected: ${SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(date.time)}"
        }
        displayView.text = displayText
    }

    private fun updateSelectedDateText() {
        println("DEBUG: Updating selected date text - Period: $selectedPeriodType, Date: ${selectedDate.time}")
        val displayText = when (selectedPeriodType) {
            "Day" -> {
                val dayFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
                "${dayFormat.format(selectedDate.time)} (Day)"
            }
            "Week" -> {
                val start = selectedDate.clone() as Calendar
                start.set(Calendar.DAY_OF_WEEK, start.firstDayOfWeek)
                val end = start.clone() as Calendar
                end.add(Calendar.DAY_OF_YEAR, 6)
                val weekFormat = SimpleDateFormat("MMMM dd", Locale.getDefault())
                val yearFormat = SimpleDateFormat("yyyy", Locale.getDefault())
                "${weekFormat.format(start.time)}-${weekFormat.format(end.time)}, ${yearFormat.format(selectedDate.time)} (Week)"
            }
            "Month" -> {
                val monthFormat = SimpleDateFormat("MMMM", Locale.getDefault())
                "${monthFormat.format(selectedDate.time)} (Month)"
            }
            "Year" -> {
                val yearFormat = SimpleDateFormat("yyyy", Locale.getDefault())
                "${yearFormat.format(selectedDate.time)} (Year)"
            }
            else -> {
                val dayFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
                "${dayFormat.format(selectedDate.time)} (Day)"
            }
        }
        selectedDateText.text = displayText
        println("DEBUG: Set selected date text to: $displayText")
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
                        uom = listOf("20L", "10L")[random.nextInt(2)],
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
        val reportType = when (selectedPeriodType) {
            "Day" -> ReportType.DAILY
            "Week" -> ReportType.WEEKLY
            "Month" -> ReportType.MONTHLY
            "Year" -> ReportType.YEARLY
            else -> ReportType.WEEKLY
        }

        val filteredData = filterDataByDateRange(selectedDate, reportType)
        
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

        when (selectedPeriodType) {
            "Day" -> updateDaySpikeData(data)
            "Week" -> updateWeekSpikeData(data)
            "Month" -> updateMonthSpikeData(data)
            "Year" -> updateYearSpikeData(data)
            else -> updateDaySpikeData(data)
        }
    }

    private fun updateDaySpikeData(data: List<SalesData>) {
        // Group by hour for day view
        val qtyByHour = data.groupBy { it.date.get(Calendar.HOUR_OF_DAY) }
            .mapValues { it.value.sumOf { sale -> sale.quantity } }
        
        val uomByHour = data.groupBy { it.date.get(Calendar.HOUR_OF_DAY) }
            .mapValues { it.value.sumOf { sale -> sale.uom.replace("L", "").toIntOrNull() ?: 0 } }
        
        val revenueByHour = data.groupBy { it.date.get(Calendar.HOUR_OF_DAY) }
            .mapValues { it.value.sumOf { sale -> sale.revenue } }
        
        val maxQtyHour = qtyByHour.maxByOrNull { it.value }
        val maxUomHour = uomByHour.maxByOrNull { it.value }
        val maxRevenueHour = revenueByHour.maxByOrNull { it.value }
        
        val qtyTime = if (maxQtyHour != null) {
            val hour = maxQtyHour.key
            val timeFormat = SimpleDateFormat("ha", Locale.getDefault())
            val cal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, hour) }
            timeFormat.format(cal.time).lowercase()
        } else "N/A"
        
        val uomTime = if (maxUomHour != null) {
            val hour = maxUomHour.key
            val timeFormat = SimpleDateFormat("ha", Locale.getDefault())
            val cal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, hour) }
            timeFormat.format(cal.time).lowercase()
        } else "N/A"
        
        val revenueTime = if (maxRevenueHour != null) {
            val hour = maxRevenueHour.key
            val timeFormat = SimpleDateFormat("ha", Locale.getDefault())
            val cal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, hour) }
            timeFormat.format(cal.time).lowercase()
        } else "N/A"
        
        salesSpikeQty.text = "${maxQtyHour?.value ?: 0} Gallons ($qtyTime)"
        salesSpikeUom.text = "${maxUomHour?.value ?: 0}L ($uomTime)"
        salesSpikeRevenue.text = "₱${String.format("%.2f", maxRevenueHour?.value ?: 0.0)} ($revenueTime)"
    }

    private fun updateWeekSpikeData(data: List<SalesData>) {
        // Group by day of week for week view
        val qtyByDay = data.groupBy { it.date.get(Calendar.DAY_OF_WEEK) }
            .mapValues { it.value.sumOf { sale -> sale.quantity } }
        
        val uomByDay = data.groupBy { it.date.get(Calendar.DAY_OF_WEEK) }
            .mapValues { it.value.sumOf { sale -> sale.uom.replace("L", "").toIntOrNull() ?: 0 } }
        
        val revenueByDay = data.groupBy { it.date.get(Calendar.DAY_OF_WEEK) }
            .mapValues { it.value.sumOf { sale -> sale.revenue } }
        
        val maxQtyDay = qtyByDay.maxByOrNull { it.value }
        val maxUomDay = uomByDay.maxByOrNull { it.value }
        val maxRevenueDay = revenueByDay.maxByOrNull { it.value }
        
        val qtyDayName = if (maxQtyDay != null) {
            val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
            val cal = Calendar.getInstance().apply { set(Calendar.DAY_OF_WEEK, maxQtyDay.key) }
            dayFormat.format(cal.time)
        } else "N/A"
        
        val uomDayName = if (maxUomDay != null) {
            val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
            val cal = Calendar.getInstance().apply { set(Calendar.DAY_OF_WEEK, maxUomDay.key) }
            dayFormat.format(cal.time)
        } else "N/A"
        
        val revenueDayName = if (maxRevenueDay != null) {
            val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
            val cal = Calendar.getInstance().apply { set(Calendar.DAY_OF_WEEK, maxRevenueDay.key) }
            dayFormat.format(cal.time)
        } else "N/A"
        
        salesSpikeQty.text = "${maxQtyDay?.value ?: 0} Gallons ($qtyDayName)"
        salesSpikeUom.text = "${maxUomDay?.value ?: 0}L ($uomDayName)"
        salesSpikeRevenue.text = "₱${String.format("%.2f", maxRevenueDay?.value ?: 0.0)} ($revenueDayName)"
    }

    private fun updateMonthSpikeData(data: List<SalesData>) {
        // Group by week of month for month view
        val qtyByWeek = data.groupBy { 
            val weekOfMonth = (it.date.get(Calendar.DAY_OF_MONTH) - 1) / 7 + 1
            minOf(weekOfMonth, 4) // Cap at week 4
        }.mapValues { it.value.sumOf { sale -> sale.quantity } }
        
        val uomByWeek = data.groupBy { 
            val weekOfMonth = (it.date.get(Calendar.DAY_OF_MONTH) - 1) / 7 + 1
            minOf(weekOfMonth, 4) // Cap at week 4
        }.mapValues { it.value.sumOf { sale -> sale.uom.replace("L", "").toIntOrNull() ?: 0 } }
        
        val revenueByWeek = data.groupBy { 
            val weekOfMonth = (it.date.get(Calendar.DAY_OF_MONTH) - 1) / 7 + 1
            minOf(weekOfMonth, 4) // Cap at week 4
        }.mapValues { it.value.sumOf { sale -> sale.revenue } }
        
        val maxQtyWeek = qtyByWeek.maxByOrNull { it.value }
        val maxUomWeek = uomByWeek.maxByOrNull { it.value }
        val maxRevenueWeek = revenueByWeek.maxByOrNull { it.value }
        
        val qtyWeekName = if (maxQtyWeek != null) "Week ${maxQtyWeek.key}" else "N/A"
        val uomWeekName = if (maxUomWeek != null) "Week ${maxUomWeek.key}" else "N/A"
        val revenueWeekName = if (maxRevenueWeek != null) "Week ${maxRevenueWeek.key}" else "N/A"
        
        salesSpikeQty.text = "${maxQtyWeek?.value ?: 0} Gallons ($qtyWeekName)"
        salesSpikeUom.text = "${maxUomWeek?.value ?: 0}L ($uomWeekName)"
        salesSpikeRevenue.text = "₱${String.format("%.2f", maxRevenueWeek?.value ?: 0.0)} ($revenueWeekName)"
    }

    private fun updateYearSpikeData(data: List<SalesData>) {
        // Group by month for year view
        val qtyByMonth = data.groupBy { it.date.get(Calendar.MONTH) }
            .mapValues { it.value.sumOf { sale -> sale.quantity } }
        
        val uomByMonth = data.groupBy { it.date.get(Calendar.MONTH) }
            .mapValues { it.value.sumOf { sale -> sale.uom.replace("L", "").toIntOrNull() ?: 0 } }
        
        val revenueByMonth = data.groupBy { it.date.get(Calendar.MONTH) }
            .mapValues { it.value.sumOf { sale -> sale.revenue } }
        
        val maxQtyMonth = qtyByMonth.maxByOrNull { it.value }
        val maxUomMonth = uomByMonth.maxByOrNull { it.value }
        val maxRevenueMonth = revenueByMonth.maxByOrNull { it.value }
        
        val qtyMonthName = if (maxQtyMonth != null) {
            val monthFormat = SimpleDateFormat("MMMM", Locale.getDefault())
            val cal = Calendar.getInstance().apply { set(Calendar.MONTH, maxQtyMonth.key) }
            monthFormat.format(cal.time)
        } else "N/A"
        
        val uomMonthName = if (maxUomMonth != null) {
            val monthFormat = SimpleDateFormat("MMMM", Locale.getDefault())
            val cal = Calendar.getInstance().apply { set(Calendar.MONTH, maxUomMonth.key) }
            monthFormat.format(cal.time)
        } else "N/A"
        
        val revenueMonthName = if (maxRevenueMonth != null) {
            val monthFormat = SimpleDateFormat("MMMM", Locale.getDefault())
            val cal = Calendar.getInstance().apply { set(Calendar.MONTH, maxRevenueMonth.key) }
            monthFormat.format(cal.time)
        } else "N/A"
        
        salesSpikeQty.text = "${maxQtyMonth?.value ?: 0} Gallons ($qtyMonthName)"
        salesSpikeUom.text = "${maxUomMonth?.value ?: 0}L ($uomMonthName)"
        salesSpikeRevenue.text = "₱${String.format("%.2f", maxRevenueMonth?.value ?: 0.0)} ($revenueMonthName)"
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

    private fun setupExportButton() {
        exportButton.setOnClickListener {
            showExportOptionsDialog()
        }
    }

    private fun showExportOptionsDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_export_options, null)
        
        val pdfOption = dialogView.findViewById<LinearLayout>(R.id.pdfOption)
        val excelOption = dialogView.findViewById<LinearLayout>(R.id.excelOption)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)
        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        
        pdfOption.setOnClickListener {
            dialog.dismiss()
            exportToPDF()
        }
        
        excelOption.setOnClickListener {
            dialog.dismiss()
            showDateRangeDialog()
        }
        
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }

    private fun exportToPDF() {
        try {
            val fileName = "Sales_Report_${System.currentTimeMillis()}.pdf"
            val file = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
            
            val pdfWriter = PdfWriter(file)
            val pdfDocument = PdfDocument(pdfWriter)
            val document = Document(pdfDocument)
            
            // Add title
            document.add(Paragraph("Sales Report")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(20f)
                .setBold())
            
            // Add date range
            document.add(Paragraph("Period: ${selectedDateText.text}")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(12f)
                .setMarginBottom(20f))
            
            // Add statistics
            val statsData = arrayOf(
                arrayOf("Total Revenue", totalRevenue.text.toString()),
                arrayOf("Total Quantity", totalQuantity.text.toString()),
                arrayOf("Total UOM", totalUom.text.toString()),
                arrayOf("Most Ordered Water", mostOrderedWater.text.toString()),
                arrayOf("Top Customer", topCustomerName.text.toString())
            )
            
            val table = Table(2)
            table.setWidth(500f)
            
            for (row in statsData) {
                table.addCell(Paragraph(row[0]).setBold())
                table.addCell(Paragraph(row[1]))
            }
            
            document.add(table)
            
            // Add sales spike data
            document.add(Paragraph("Sales Spike")
                .setFontSize(16f)
                .setBold()
                .setMarginTop(20f))
            
            val spikeData = arrayOf(
                arrayOf("Highest QTY", salesSpikeQty.text.toString()),
                arrayOf("Highest UOM", salesSpikeUom.text.toString()),
                arrayOf("Highest Revenue", salesSpikeRevenue.text.toString())
            )
            
            val spikeTable = Table(2)
            spikeTable.setWidth(500f)
            
            for (row in spikeData) {
                spikeTable.addCell(Paragraph(row[0]).setBold())
                spikeTable.addCell(Paragraph(row[1]))
            }
            
            document.add(spikeTable)
            
            document.close()
            
            // Share the file
            shareFile(file, "application/pdf")
            
            Toast.makeText(this, "PDF exported successfully!", Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            Toast.makeText(this, "Error creating PDF: ${e.message}", Toast.LENGTH_LONG).show()
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
        
        fromDateButton.setOnClickListener {
            showDatePicker(selectedFromDate) { calendar ->
                selectedFromDate = calendar
                updateDateRangeText(selectedDateRangeText)
            }
        }
        
        toDateButton.setOnClickListener {
            showDatePicker(selectedToDate) { calendar ->
                selectedToDate = calendar
                updateDateRangeText(selectedDateRangeText)
            }
        }
        
        exportButton.setOnClickListener {
            dialog.dismiss()
            exportToCSV()
        }
        
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }

    private fun updateDateRangeText(textView: TextView) {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val fromDateStr = dateFormat.format(selectedFromDate.time)
        val toDateStr = dateFormat.format(selectedToDate.time)
        textView.text = "Exporting from $fromDateStr to $toDateStr"
        textView.visibility = View.VISIBLE
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
        datePickerDialog.show()
    }

    private fun exportToCSV() {
        try {
            val fileName = "Sales_Report_${System.currentTimeMillis()}.csv"
            val file = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
            
            val csvContent = StringBuilder()
            
            // Get current user ID
            val currentUserId = sharedPreferences.getString("current_user_id", "") ?: ""
            if (currentUserId.isEmpty()) {
                showToast("User not logged in")
                return
            }
            
            // Format dates for filtering
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val fromDateStr = dateFormat.format(selectedFromDate.time)
            val toDateStr = dateFormat.format(selectedToDate.time)
            
            // Filter data by date range
            val filteredData = dummyData.filter { sale ->
                val saleDateStr = dateFormat.format(sale.date.time)
                saleDateStr >= fromDateStr && saleDateStr <= toDateStr
            }.sortedBy { it.date }
            
            if (filteredData.isEmpty()) {
                showToast("No sales data found in selected date range")
                return
            }
            
            // Group data by date
            val dataByDate = filteredData.groupBy { 
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it.date.time)
            }
            
            // Export each date as a separate table
            for ((date, sales) in dataByDate.toSortedMap()) {
                exportDateToCsv(csvContent, date, sales)
            }
            
            // Write to file
            file.writeText(csvContent.toString())
            
            // Share the file
            shareFile(file, "text/csv")
            
            showToast("CSV file exported successfully!")
            
        } catch (e: Exception) {
            showToast("Error creating CSV file: ${e.message}")
        }
    }

    private fun exportDateToCsv(csvContent: StringBuilder, date: String, sales: List<SalesData>) {
        // Format display date as MM/DD/YYYY (matching Orders format)
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
        
        // Process sales data
        var totalRevenue = 0.0
        var totalUom = 0
        
        for (sale in sales) {
            // Map product names according to specifications (same as Orders)
            val productName = mapProductName(sale.waterType, sale.uom)
            
            // Determine UOM based on product type
            val uom = determineUom(productName, sale.uom)
            
            // Calculate correct price using database pricing
            val unitPrice = calculateCorrectPrice(sale.waterType, sale.uom)
            val totalPrice = unitPrice * sale.quantity
            
            // Calculate UOM value for total calculation (sum UOM values, ignore quantity)
            val uomValue = if (uom == "N/A") 0 else sale.uom.replace("L", "").toIntOrNull() ?: 0
            totalUom += uomValue
            
            // Add sales data row
            csvContent.appendLine("${sale.customerName},$productName,$uom,${sale.quantity},${String.format("%.2f", totalPrice)}")
            totalRevenue += totalPrice
        }
        
        csvContent.appendLine() // Empty line
        
        // Add Total UOM (merged columns) - ABOVE Total Revenue as requested
        csvContent.appendLine("Total UOM:,$totalUom")
        
        // Add Total Revenue (merged columns)
        csvContent.appendLine("Total Revenue:,${String.format("%.2f", totalRevenue)}")
        
        csvContent.appendLine() // Empty line between dates
    }

    private fun getCurrentReportType(): ReportType {
        return when (selectedPeriodType) {
            "Day" -> ReportType.DAILY
            "Week" -> ReportType.WEEKLY
            "Month" -> ReportType.MONTHLY
            "Year" -> ReportType.YEARLY
            else -> ReportType.WEEKLY
        }
    }

    private fun shareFile(file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            file
        )
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        startActivity(Intent.createChooser(intent, "Share Report"))
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    private fun mapProductName(waterType: String, uom: String): String {
        return when {
            // Water types
            waterType == "Mineral" -> "Mineral"
            waterType == "Alkaline" -> "Alkaline"
            
            // Gallon types (based on UOM and water type)
            uom == "20L" && waterType == "Mineral" -> "Round Gallon (New)"
            uom == "20L" && waterType == "Alkaline" -> "Round Gallon (New)"
            uom == "10L" && waterType == "Mineral" -> "Slim Gallon (New)"
            uom == "10L" && waterType == "Alkaline" -> "Slim Gallon (New)"
            
            // Fallback to water type if no specific match
            else -> waterType
        }
    }
    
    private fun determineUom(productName: String, originalUom: String): String {
        return when (productName) {
            "Mineral", "Alkaline" -> originalUom // 20L or 10L
            "Round Gallon (New)", "Slim Gallon (New)" -> "20L"
            else -> originalUom
        }
    }
    
    private fun calculateCorrectPrice(waterType: String, uom: String): Double {
        val prices = mapOf(
            "Alkaline" to mapOf(
                "20L" to 50.0,
                "10L" to 25.0
            ),
            "Mineral" to mapOf(
                "20L" to 30.0,
                "10L" to 15.0
            )
        )
        return prices[waterType]?.get(uom) ?: 0.0
    }

    enum class ReportType {
        DAILY, WEEKLY, MONTHLY, YEARLY
    }
}