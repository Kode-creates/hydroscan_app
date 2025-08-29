package com.adamson.fhydroscan

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
    private lateinit var reportTableContent: LinearLayout
    private lateinit var reportPeriod: TextView
    private lateinit var totalQuantity: TextView
    private lateinit var totalRevenue: TextView
    private lateinit var calendarView: CalendarView
    private lateinit var reportTypeSpinner: Spinner

    // Dummy data structure
    data class SalesData(
        val date: Calendar,
        val uom: String,
        val quantity: Int,
        val revenue: Double,
        val waterType: String  // "Alkaline" or "Mineral"
    )

    // Generate dummy data for demonstration
    private val dummyData = generateDummyData()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sales)

        // Initialize views
        reportTableContent = findViewById(R.id.reportTableContent)
        reportPeriod = findViewById(R.id.reportPeriod)
        totalQuantity = findViewById(R.id.totalQuantity)
        totalRevenue = findViewById(R.id.totalRevenue)
        calendarView = findViewById(R.id.calendarView)
        reportTypeSpinner = findViewById(R.id.reportTypeSpinner)

        // Set up listeners
        setupReportTypeSpinner()
        setupCalendarListener()
        setupBottomNavigation()

        // Show initial report
        generateReport(Calendar.getInstance())
    }

    private fun setupReportTypeSpinner() {
        val reportTypes = arrayOf("Daily", "Weekly", "Monthly", "Yearly")
        val adapter = ArrayAdapter(this, R.layout.custom_spinner_item, reportTypes)
        adapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item)
        reportTypeSpinner.adapter = adapter
        
        // Set default selection to "Daily" (index 0)
        reportTypeSpinner.setSelection(0)
        
        // Force the spinner to show the selected item text
        reportTypeSpinner.post {
            reportTypeSpinner.setSelection(0)
        }
        
        reportTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                generateReport(Calendar.getInstance().apply { 
                    timeInMillis = calendarView.date 
                })
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }
    }

    private fun setupCalendarListener() {
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val selectedDate = Calendar.getInstance().apply {
                set(year, month, dayOfMonth)
            }
            generateReport(selectedDate)
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
                        waterType = waterTypes[random.nextInt(2)]
                    )
                )
            }
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        return data
    }

    private fun generateReport(selectedDate: Calendar) {
        val reportType = when (reportTypeSpinner.selectedItemPosition) {
            0 -> ReportType.DAILY
            1 -> ReportType.WEEKLY
            2 -> ReportType.MONTHLY
            3 -> ReportType.YEARLY
            else -> ReportType.DAILY
        }

        val filteredData = filterDataByDateRange(selectedDate, reportType)
        val groupedData = groupDataByUOM(filteredData)
        val mostOrderedTypeOverall = getMostOrderedType(filteredData)
        
        // Update period text
        reportPeriod.text = "Report for: ${getReportPeriodText(selectedDate, reportType)}"

        // Clear previous data
        reportTableContent.removeAllViews()

        // Add rows for each UOM
        var totalQty = 0
        var totalRev = 0.0

        groupedData.forEach { (uom, data) ->
            val quantity = data.sumOf { it.quantity }
            val revenue = data.sumOf { it.revenue }
            val mostOrderedTypeForUOM = getMostOrderedType(data)
            totalQty += quantity
            totalRev += revenue

            addReportRow(uom, quantity, revenue, mostOrderedTypeForUOM)
        }

        // Update totals
        totalQuantity.text = totalQty.toString()
        totalRevenue.text = String.format("₱%.2f", totalRev)
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

    private fun groupDataByUOM(data: List<SalesData>): Map<String, List<SalesData>> {
        return data.groupBy { it.uom }
    }

    private fun getMostOrderedType(data: List<SalesData>): String {
        return data.groupBy { it.waterType }
            .mapValues { it.value.sumOf { sale -> sale.quantity } }
            .maxByOrNull { it.value }?.key ?: "N/A"
    }

    private fun addReportRow(uom: String, quantity: Int, revenue: Double, mostOrderedType: String) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(8, 8, 8, 8)
        }

        // Add columns
        listOf(
            uom,
            quantity.toString(),
            String.format("₱%.2f", revenue),
            mostOrderedType
        ).forEach { text ->
            TextView(this).apply {
                this.text = text
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setTextColor(resources.getColor(R.color.black, null))
            }.also { row.addView(it) }
        }

        reportTableContent.addView(row)
    }

    private fun getReportPeriodText(date: Calendar, reportType: ReportType): String {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return when (reportType) {
            ReportType.DAILY -> dateFormat.format(date.time)
            ReportType.WEEKLY -> {
                val start = date.clone() as Calendar
                start.set(Calendar.DAY_OF_WEEK, start.firstDayOfWeek)
                val end = start.clone() as Calendar
                end.add(Calendar.DAY_OF_YEAR, 6)
                "${dateFormat.format(start.time)} - ${dateFormat.format(end.time)}"
            }
            ReportType.MONTHLY -> SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(date.time)
            ReportType.YEARLY -> SimpleDateFormat("yyyy", Locale.getDefault()).format(date.time)
        }
    }

    enum class ReportType {
        DAILY, WEEKLY, MONTHLY, YEARLY
    }
}