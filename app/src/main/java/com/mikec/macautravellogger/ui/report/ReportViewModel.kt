package com.mikec.macautravellogger.ui.report

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.itextpdf.text.BaseColor
import com.itextpdf.text.Chunk
import com.itextpdf.text.Document
import com.itextpdf.text.Element
import com.itextpdf.text.Font
import com.itextpdf.text.PageSize
import com.itextpdf.text.Paragraph
import com.itextpdf.text.Phrase
import com.itextpdf.text.Rectangle
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter
import com.mikec.macautravellogger.data.local.DetectionMethod
import com.mikec.macautravellogger.data.local.TravelEntry
import com.mikec.macautravellogger.data.repository.TripRepository
import com.mikec.macautravellogger.util.DateUtils
import com.opencsv.CSVWriter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.time.LocalDateTime
import java.time.Month
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

data class MonthlySummary(
    val yearMonth: String = "",
    val tripCount: Int = 0,
    val uniqueDayCount: Int = 0,
    val totalHours: Double = 0.0,
    val compliancePercent: Float = 0f,
    val entries: List<TravelEntry> = emptyList()
)

sealed class ExportEvent {
    data class Success(val uri: Uri, val mimeType: String) : ExportEvent()
    data class Error(val message: String) : ExportEvent()
}

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class ReportViewModel @Inject constructor(
    private val repository: TripRepository,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        private const val FILE_PROVIDER_AUTHORITY = "com.mikec.macautravellogger.fileprovider"
    }

    private val _selectedYearMonth = MutableStateFlow(DateUtils.getCurrentYearMonth())
    val selectedYearMonth: StateFlow<String> = _selectedYearMonth.asStateFlow()

    val summary: StateFlow<MonthlySummary> = _selectedYearMonth
        .flatMapLatest { yearMonth ->
            repository.getByMonth(yearMonth).map { entries ->
                buildSummary(yearMonth, entries)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MonthlySummary()
        )

    private val _exportEvent = MutableSharedFlow<ExportEvent>()
    val exportEvent: SharedFlow<ExportEvent> = _exportEvent.asSharedFlow()

    fun selectMonth(yearMonth: String) {
        _selectedYearMonth.value = yearMonth
    }

    fun previousMonth() {
        _selectedYearMonth.value = DateUtils.previousMonth(_selectedYearMonth.value)
    }

    fun nextMonth() {
        val next = DateUtils.nextMonth(_selectedYearMonth.value)
        if (DateUtils.isCurrentOrPastMonth(next)) {
            _selectedYearMonth.value = next
        }
    }

    fun exportToCsv() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val s = summary.value
                val file = buildCsvFile(s, getOrCreateExportsDir())
                val uri = FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, file)
                _exportEvent.emit(ExportEvent.Success(uri, "text/csv"))
            } catch (e: Exception) {
                _exportEvent.emit(ExportEvent.Error("CSV export failed: ${e.message}"))
            }
        }
    }

    fun exportToPdf() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val s = summary.value
                val file = buildPdfFile(s, getOrCreateExportsDir())
                val uri = FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, file)
                _exportEvent.emit(ExportEvent.Success(uri, "application/pdf"))
            } catch (e: Exception) {
                _exportEvent.emit(ExportEvent.Error("PDF export failed: ${e.message}"))
            }
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun getOrCreateExportsDir(): File =
        File(context.cacheDir, "exports").also { it.mkdirs() }

    private fun formatMonthYear(yearMonth: String): String {
        val (year, month) = yearMonth.split("-")
        val name = Month.of(month.toInt()).getDisplayName(TextStyle.FULL, Locale.ENGLISH)
        return "$name $year"
    }

    private fun filenameMonthYear(yearMonth: String): String =
        formatMonthYear(yearMonth).replace(" ", "_")

    private fun generatedOn(): String =
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))

    // ── CSV ──────────────────────────────────────────────────────────────────

    private fun buildCsvFile(summary: MonthlySummary, dir: File): File {
        val file = File(dir, "MacauTravelLog_${filenameMonthYear(summary.yearMonth)}.csv")
        val completed = summary.entries.filter { it.checkOutTime != null }
        val autoDetected = summary.entries.count { it.detectionMethod == DetectionMethod.AUTO }
        val manual = summary.entries.count { it.detectionMethod == DetectionMethod.MANUAL }

        CSVWriter(FileWriter(file)).use { writer ->
            // Column headers
            writer.writeNext(arrayOf(
                "trip_id", "date", "check_in", "check_out",
                "duration_hours", "detection_method", "notes"
            ))
            // Trip rows
            for (entry in summary.entries) {
                writer.writeNext(arrayOf(
                    entry.id.toString(),
                    entry.date,
                    entry.checkInTime ?: "",
                    entry.checkOutTime ?: "",
                    entry.durationHours?.let { "%.2f".format(it) } ?: "",
                    entry.detectionMethod.name,
                    entry.notes ?: ""
                ))
            }
            // Blank separator before summary block
            writer.writeNext(arrayOf())
            // Summary block — key/value pairs
            writer.writeNext(arrayOf("month",               formatMonthYear(summary.yearMonth)))
            writer.writeNext(arrayOf("total_trips",         summary.entries.size.toString()))
            writer.writeNext(arrayOf("completed_trips",     completed.size.toString()))
            writer.writeNext(arrayOf("total_hours",         "%.2f".format(summary.totalHours)))
            writer.writeNext(arrayOf("work_days_in_macau",  summary.uniqueDayCount.toString()))
            writer.writeNext(arrayOf("auto_detected",       autoDetected.toString()))
            writer.writeNext(arrayOf("manual_entries",      manual.toString()))
            writer.writeNext(arrayOf("generated_by",        "MacauTravelLogger v1.0"))
            writer.writeNext(arrayOf("generated_on",        generatedOn()))
        }
        return file
    }

    // ── PDF ──────────────────────────────────────────────────────────────────

    private fun buildPdfFile(summary: MonthlySummary, dir: File): File {
        val monthYear = formatMonthYear(summary.yearMonth)
        val file = File(dir, "MacauTravelLog_${filenameMonthYear(summary.yearMonth)}.pdf")

        val titleFont   = Font(Font.FontFamily.HELVETICA, 20f, Font.BOLD)
        val headingFont = Font(Font.FontFamily.HELVETICA, 13f, Font.BOLD)
        val normalFont  = Font(Font.FontFamily.HELVETICA, 10f, Font.NORMAL)
        val boldFont    = Font(Font.FontFamily.HELVETICA, 10f, Font.BOLD)
        val smallFont   = Font(Font.FontFamily.HELVETICA, 9f,  Font.NORMAL, BaseColor(80, 80, 80))
        val tableHdrFont = Font(Font.FontFamily.HELVETICA, 9f, Font.BOLD)
        val tableCellFont = Font(Font.FontFamily.HELVETICA, 9f, Font.NORMAL)

        val document = Document(PageSize.A4, 40f, 40f, 50f, 40f)
        PdfWriter.getInstance(document, FileOutputStream(file))
        document.open()

        // ── Document header ──
        Paragraph("MacauTravelLogger", titleFont).also {
            it.setSpacingAfter(4f); document.add(it)
        }
        Paragraph("Monthly Report: $monthYear", headingFont).also {
            it.setSpacingAfter(2f); document.add(it)
        }
        Paragraph("Generated: ${generatedOn()}", smallFont).also {
            it.setSpacingAfter(16f); document.add(it)
        }

        // ── Summary section ──
        Paragraph("Summary", headingFont).also {
            it.setSpacingAfter(6f); document.add(it)
        }

        val completed    = summary.entries.count { it.checkOutTime != null }
        val autoDetected = summary.entries.count { it.detectionMethod == DetectionMethod.AUTO }
        val manual       = summary.entries.count { it.detectionMethod == DetectionMethod.MANUAL }

        val summaryTable = PdfPTable(2)
        summaryTable.widthPercentage = 55f
        summaryTable.setHorizontalAlignment(Element.ALIGN_LEFT)
        summaryTable.setWidths(floatArrayOf(2f, 3f))

        fun kvRow(key: String, value: String) {
            val keyCell = PdfPCell(Phrase(key, normalFont))
            keyCell.setBorder(Rectangle.NO_BORDER)
            keyCell.setPaddingBottom(4f)
            summaryTable.addCell(keyCell)

            val valCell = PdfPCell(Phrase(value, boldFont))
            valCell.setBorder(Rectangle.NO_BORDER)
            valCell.setPaddingBottom(4f)
            summaryTable.addCell(valCell)
        }

        kvRow("Month",            monthYear)
        kvRow("Total Trips",      summary.entries.size.toString())
        kvRow("Completed Trips",  completed.toString())
        kvRow("Total Hours",      "${"%.2f".format(summary.totalHours)} h")
        kvRow("Days in Macau",    summary.uniqueDayCount.toString())
        kvRow("Auto-detected",    autoDetected.toString())
        kvRow("Manual Entries",   manual.toString())
        kvRow("Compliance",       "${(summary.compliancePercent * 100).toInt()}%")
        document.add(summaryTable)
        document.add(Chunk.NEWLINE)

        // ── Trip table ──
        if (summary.entries.isNotEmpty()) {
            Paragraph("Trip Details", headingFont).also {
                it.setSpacingAfter(6f); document.add(it)
            }

            val tripTable = PdfPTable(7)
            tripTable.widthPercentage = 100f
            tripTable.setWidths(floatArrayOf(1f, 2f, 1.5f, 1.5f, 1.5f, 1.5f, 2.5f))

            val headerBg = BaseColor(220, 220, 220)
            fun headerCell(text: String): PdfPCell {
                val cell = PdfPCell(Phrase(text, tableHdrFont))
                cell.setBackgroundColor(headerBg)
                cell.setPaddingBottom(5f)
                return cell
            }
            fun dataCell(text: String): PdfPCell {
                val cell = PdfPCell(Phrase(text, tableCellFont))
                cell.setPaddingBottom(4f)
                return cell
            }

            tripTable.addCell(headerCell("ID"))
            tripTable.addCell(headerCell("Date"))
            tripTable.addCell(headerCell("Check In"))
            tripTable.addCell(headerCell("Check Out"))
            tripTable.addCell(headerCell("Duration"))
            tripTable.addCell(headerCell("Method"))
            tripTable.addCell(headerCell("Notes"))

            for (entry in summary.entries) {
                tripTable.addCell(dataCell(entry.id.toString()))
                tripTable.addCell(dataCell(entry.date))
                tripTable.addCell(dataCell(entry.checkInTime ?: "—"))
                tripTable.addCell(dataCell(entry.checkOutTime ?: "—"))
                tripTable.addCell(dataCell(
                    entry.durationHours?.let { "${"%.2f".format(it)}h" } ?: "—"
                ))
                tripTable.addCell(dataCell(entry.detectionMethod.name))
                tripTable.addCell(dataCell(entry.notes ?: ""))
            }
            document.add(tripTable)
        }

        // ── Footer ──
        document.add(Chunk.NEWLINE)
        document.add(Paragraph("Generated by MacauTravelLogger v1.0", smallFont))

        document.close()
        return file
    }

    // ── Summary builder ──────────────────────────────────────────────────────

    private fun buildSummary(yearMonth: String, entries: List<TravelEntry>): MonthlySummary {
        val completed = entries.filter { it.checkOutTime != null }
        val uniqueDays = completed.map { it.date }.toSet().size
        val totalHours = completed.sumOf { it.durationHours ?: 0.0 }
        val daysInMonth = DateUtils.getDaysInMonth(yearMonth)
        val compliancePct = if (daysInMonth > 0) uniqueDays.toFloat() / daysInMonth else 0f
        return MonthlySummary(
            yearMonth = yearMonth,
            tripCount = completed.size,
            uniqueDayCount = uniqueDays,
            totalHours = totalHours,
            compliancePercent = compliancePct,
            entries = entries
        )
    }
}
