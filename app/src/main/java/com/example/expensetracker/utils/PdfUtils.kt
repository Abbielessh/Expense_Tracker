package com.example.expensetracker.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.expensetracker.model.ExpenseAppData
import com.example.expensetracker.model.ExpenseProfile
import com.example.expensetracker.model.MoneyTransaction
import com.example.expensetracker.model.TransactionType
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class PdfSplitType(val label: String) {
    NONE("Full Report"),
    DAY("Day-wise Split"),
    WEEK("Week-wise Split"),
    MONTH("Month-wise Split")
}

fun createExpensePdf(
    context: Context,
    appData: ExpenseAppData,
    profile: ExpenseProfile,
    splitType: PdfSplitType = PdfSplitType.NONE
): ByteArray {
    val document   = PdfDocument()
    val pageWidth  = 595
    val pageHeight = 842
    val margin     = 36
    var pageNumber = 1

    // ── Color palette ──────────────────────────────────────────────────────────
    val colorPrimary = AndroidColor.rgb(37,  99, 235)   // #2563EB blue
    val colorGreen   = AndroidColor.rgb(5,  150, 105)   // #059669 green
    val colorRed     = AndroidColor.rgb(220,  38,  38)  // #DC2626 red
    val colorGray    = AndroidColor.rgb(102, 112, 133)  // #667085
    val colorDark    = AndroidColor.rgb(16,   24,  40)  // #101828
    val colorLightBg = AndroidColor.rgb(237, 244, 255)  // light blue tint
    val colorAccent  = AndroidColor.rgb(16,  185, 129)  // #10B981 teal

    // ── Shared paints ──────────────────────────────────────────────────────────
    val titlePaint = Paint().apply {
        color = AndroidColor.WHITE; textSize = 20f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val sectionHeaderPaint = Paint().apply {
        color = colorPrimary; textSize = 12f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val normalPaint   = Paint().apply { color = AndroidColor.rgb(52, 64, 84); textSize = 11f }
    val smallPaint    = Paint().apply { color = colorGray; textSize = 10f }
    val rectFillPaint = Paint().apply { style = Paint.Style.FILL }
    val dividerPaint  = Paint().apply {
        color = AndroidColor.rgb(220, 225, 235); strokeWidth = 0.8f; style = Paint.Style.STROKE
    }

    // ── Page / canvas state ────────────────────────────────────────────────────
    fun newPage(): Pair<PdfDocument.Page, Canvas> {
        val info = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        val p    = document.startPage(info)
        return p to p.canvas
    }

    var pair   = newPage()
    var page   = pair.first
    var canvas = pair.second
    var y      = 0

    // ── Layout helpers ─────────────────────────────────────────────────────────
    fun fillRect(l: Float, t: Float, r: Float, b: Float, color: Int, radius: Float = 4f) {
        rectFillPaint.color = color
        canvas.drawRoundRect(RectF(l, t, r, b), radius, radius, rectFillPaint)
    }

    fun hLine(yPos: Int) {
        canvas.drawLine(
            margin.toFloat(), yPos.toFloat(),
            (pageWidth - margin).toFloat(), yPos.toFloat(), dividerPaint
        )
    }

    fun continuedHeader() {
        fillRect(0f, 0f, pageWidth.toFloat(), 28f, colorPrimary, 0f)
        val p  = Paint().apply { color = AndroidColor.WHITE; textSize = 10f }
        val pg = Paint().apply { color = AndroidColor.WHITE; textSize = 9f }
        canvas.drawText("Expense Tracker Report — ${profile.name}  (continued)", margin.toFloat(), 18f, p)
        canvas.drawText("Page $pageNumber", (pageWidth - margin - 36).toFloat(), 18f, pg)
        y = 36
    }

    fun finishPage() {
        val fp = Paint().apply { color = colorGray; textSize = 9f }
        canvas.drawText(
            "Expense Tracker  •  Page $pageNumber  •  ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).format(Date())}",
            margin.toFloat(), (pageHeight - 14).toFloat(), fp
        )
        document.finishPage(page)
        pageNumber++
    }

    fun ensureSpace(needed: Int) {
        if (y + needed > pageHeight - margin - 30) {
            finishPage()
            pair   = newPage()
            page   = pair.first
            canvas = pair.second
            continuedHeader()
        }
    }

    // ── Draw one transaction row ───────────────────────────────────────────────
    fun drawTxRow(tx: MoneyTransaction, index: Int, currencyCode: String) {
        val hasNote = tx.note.isNotBlank()
        val rowH    = if (hasNote) 52 else 40
        ensureSpace(rowH + 4)
        val startY = y

        // Alternating row background
        if (index % 2 == 1) {
            fillRect(
                (margin - 2).toFloat(), (startY - 3).toFloat(),
                (pageWidth - margin + 2).toFloat(), (startY + rowH - 4).toFloat(),
                AndroidColor.rgb(248, 250, 255), 3f
            )
        }

        // Index + date
        val numDatePaint = Paint().apply { color = colorGray; textSize = 9.5f }
        canvas.drawText("$index.  ${formatDisplayDate(tx.dateMillis)}", margin.toFloat(), (startY + 11).toFloat(), numDatePaint)

        // Amount (right side)
        val sign     = if (tx.type == TransactionType.EXPENSE) "−" else "+"
        val amtColor = if (tx.type == TransactionType.EXPENSE) colorRed else colorGreen
        val amtPaint = Paint().apply {
            color = amtColor; textSize = 11.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(
            "$sign ${formatMoney(tx.amount, currencyCode)}",
            (pageWidth - margin - 105).toFloat(), (startY + 11).toFloat(), amtPaint
        )

        // Title
        val titleTrunc = if (tx.title.length > 40) tx.title.take(40) + "…" else tx.title
        canvas.drawText(titleTrunc, (margin + 6).toFloat(), (startY + 26).toFloat(), normalPaint)

        // Category
        val catPaint = Paint().apply { color = colorAccent; textSize = 9.5f }
        canvas.drawText("• ${tx.category}", (margin + 6).toFloat(), (startY + 38).toFloat(), catPaint)

        // Note
        if (hasNote) {
            val noteStr = if (tx.note.length > 70) tx.note.take(70) + "…" else tx.note
            canvas.drawText("  Note: $noteStr", (margin + 6).toFloat(), (startY + 50).toFloat(), smallPaint)
        }

        y = startY + rowH
    }

    // ── Group header (used by split modes) ────────────────────────────────────
    fun drawGroupHeader(label: String) {
        ensureSpace(26)
        fillRect(
            margin.toFloat(), (y - 2).toFloat(),
            (pageWidth - margin).toFloat(), (y + 18).toFloat(),
            AndroidColor.rgb(220, 235, 255), 4f
        )
        canvas.drawText(label, (margin + 8).toFloat(), (y + 13).toFloat(), sectionHeaderPaint)
        y += 24
    }

    // ── Group total footer (used by split modes) ──────────────────────────────
    fun drawGroupTotal(expense: Double, income: Double, currencyCode: String, label: String) {
        ensureSpace(24)
        fillRect(
            margin.toFloat(), (y - 2).toFloat(),
            (pageWidth - margin).toFloat(), (y + 16).toFloat(),
            AndroidColor.rgb(255, 242, 242), 3f
        )
        val ep = Paint().apply {
            color = colorRed; textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(
            "$label Expense: ${formatMoney(expense, currencyCode)}",
            (margin + 6).toFloat(), (y + 11).toFloat(), ep
        )
        if (income > 0) {
            val ip = Paint().apply {
                color = colorGreen; textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText(
                "Income: ${formatMoney(income, currencyCode)}",
                (margin + 240).toFloat(), (y + 11).toFloat(), ip
            )
        }
        y += 22
        hLine(y); y += 10
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PAGE 1 — header banner
    // ═══════════════════════════════════════════════════════════════════════════
    val bannerH = 72
    fillRect(0f, 0f, pageWidth.toFloat(), bannerH.toFloat(), colorPrimary, 0f)
    canvas.drawText("Expense Tracker", margin.toFloat(), 30f, titlePaint)
    val bannerSub = Paint().apply { color = AndroidColor.rgb(186, 210, 255); textSize = 11f }
    canvas.drawText("Financial Report  •  Profile: ${profile.name}", margin.toFloat(), 48f, bannerSub)
    val bannerRight = Paint().apply { color = AndroidColor.rgb(186, 210, 255); textSize = 10f }
    canvas.drawText(
        "Generated: ${SimpleDateFormat("dd/MM/yyyy  HH:mm", Locale.US).format(Date())}",
        (pageWidth - margin - 140).toFloat(), 48f, bannerRight
    )
    canvas.drawText(
        "Currency: ${appData.currencyCode}  |  ${splitType.label}",
        (pageWidth - margin - 140).toFloat(), 62f, bannerRight
    )
    y = bannerH + 14

    // ── Summary section ────────────────────────────────────────────────────────
    val totalExpense = profile.transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    val totalIncome  = profile.transactions.filter { it.type == TransactionType.INCOME  }.sumOf { it.amount }
    val balance      = totalIncome - totalExpense

    fillRect(margin.toFloat(), (y - 2).toFloat(), (pageWidth - margin).toFloat(), (y + 18).toFloat(), colorLightBg, 6f)
    canvas.drawText("Summary", (margin + 6).toFloat(), (y + 13).toFloat(), sectionHeaderPaint)
    y += 26

    // Three summary cards
    val cw = (pageWidth - margin * 2 - 16) / 3
    val ch = 54

    // Income card
    fillRect(margin.toFloat(), y.toFloat(), (margin + cw).toFloat(), (y + ch).toFloat(), AndroidColor.rgb(236, 253, 245), 8f)
    val incLabel = Paint().apply { color = colorGreen; textSize = 9f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
    val incVal   = Paint().apply { color = colorGreen; textSize = 12f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
    canvas.drawText("INCOME",                                    (margin + 8).toFloat(), (y + 16).toFloat(), incLabel)
    canvas.drawText(formatMoney(totalIncome, appData.currencyCode), (margin + 8).toFloat(), (y + 34).toFloat(), incVal)
    canvas.drawText(
        "${profile.transactions.count { it.type == TransactionType.INCOME }} entries",
        (margin + 8).toFloat(), (y + 50).toFloat(), smallPaint
    )

    // Expense card
    val x2 = margin + cw + 8
    fillRect(x2.toFloat(), y.toFloat(), (x2 + cw).toFloat(), (y + ch).toFloat(), AndroidColor.rgb(254, 242, 242), 8f)
    val expLabel = Paint().apply { color = colorRed; textSize = 9f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
    val expVal   = Paint().apply { color = colorRed; textSize = 12f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
    canvas.drawText("EXPENSE",                                    (x2 + 8).toFloat(), (y + 16).toFloat(), expLabel)
    canvas.drawText(formatMoney(totalExpense, appData.currencyCode), (x2 + 8).toFloat(), (y + 34).toFloat(), expVal)
    canvas.drawText(
        "${profile.transactions.count { it.type == TransactionType.EXPENSE }} entries",
        (x2 + 8).toFloat(), (y + 50).toFloat(), smallPaint
    )

    // Balance card
    val x3       = margin + (cw + 8) * 2
    val balColor = if (balance >= 0) colorPrimary else colorRed
    fillRect(x3.toFloat(), y.toFloat(), (x3 + cw).toFloat(), (y + ch).toFloat(), AndroidColor.rgb(239, 246, 255), 8f)
    val balLabel = Paint().apply { color = balColor; textSize = 9f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
    val balVal   = Paint().apply { color = balColor; textSize = 12f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
    canvas.drawText("BALANCE",                               (x3 + 8).toFloat(), (y + 16).toFloat(), balLabel)
    canvas.drawText(formatMoney(balance, appData.currencyCode), (x3 + 8).toFloat(), (y + 34).toFloat(), balVal)
    canvas.drawText("${profile.transactions.size} total", (x3 + 8).toFloat(), (y + 50).toFloat(), smallPaint)

    y += ch + 14

    // ── Category breakdown ─────────────────────────────────────────────────────
    fillRect(margin.toFloat(), (y - 2).toFloat(), (pageWidth - margin).toFloat(), (y + 18).toFloat(), colorLightBg, 6f)
    canvas.drawText("Category Breakdown  (Expenses)", (margin + 6).toFloat(), (y + 13).toFloat(), sectionHeaderPaint)
    y += 26

    val categoryTotals = profile.transactions
        .filter { it.type == TransactionType.EXPENSE }
        .groupBy { it.category }
        .mapValues { e -> e.value.sumOf { it.amount } }
        .toList().sortedByDescending { it.second }

    if (categoryTotals.isEmpty()) {
        canvas.drawText("No expense categories found.", margin.toFloat(), y.toFloat(), smallPaint)
        y += 16
    } else {
        categoryTotals.forEach { (category, amount) ->
            ensureSpace(18)
            val pct = if (totalExpense > 0)
                String.format(Locale.US, "%.1f", amount / totalExpense * 100) else "0.0"
            canvas.drawText("• $category", margin.toFloat(), y.toFloat(), normalPaint)
            val ap = Paint().apply {
                color = colorRed; textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText(
                "${formatMoney(amount, appData.currencyCode)}  ($pct%)",
                (pageWidth - margin - 130).toFloat(), y.toFloat(), ap
            )
            y += 16
        }
    }

    y += 6; hLine(y); y += 12

    // ── Transaction details ────────────────────────────────────────────────────
    ensureSpace(24)
    fillRect(margin.toFloat(), (y - 2).toFloat(), (pageWidth - margin).toFloat(), (y + 18).toFloat(), colorLightBg, 6f)
    canvas.drawText("Transaction Details", (margin + 6).toFloat(), (y + 13).toFloat(), sectionHeaderPaint)
    y += 26

    val sortedTx = profile.transactions.sortedByDescending { it.dateMillis }

    if (sortedTx.isEmpty()) {
        canvas.drawText("No transactions found.", margin.toFloat(), y.toFloat(), smallPaint)
        y += 16
    } else {
        when (splitType) {
            PdfSplitType.NONE -> {
                sortedTx.forEachIndexed { idx, tx ->
                    drawTxRow(tx, idx + 1, appData.currencyCode)
                }
            }

            PdfSplitType.DAY -> {
                val keyFmt  = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val dispFmt = SimpleDateFormat("dd/MM/yyyy, EEE", Locale.US)
                sortedTx
                    .groupBy { keyFmt.format(Date(it.dateMillis)) }
                    .entries.sortedByDescending { it.key }
                    .forEach { (dayKey, txList) ->
                        val header = try { dispFmt.format(keyFmt.parse(dayKey)!!) } catch (_: Exception) { dayKey }
                        drawGroupHeader("📅  $header")
                        var exp = 0.0; var inc = 0.0
                        txList.forEachIndexed { idx, tx ->
                            drawTxRow(tx, idx + 1, appData.currencyCode)
                            if (tx.type == TransactionType.EXPENSE) exp += tx.amount else inc += tx.amount
                        }
                        drawGroupTotal(exp, inc, appData.currencyCode, "Day")
                    }
            }

            PdfSplitType.WEEK -> {
                val cal     = Calendar.getInstance()
                val dispFmt = SimpleDateFormat("dd/MM/yyyy", Locale.US)
                sortedTx
                    .groupBy { tx ->
                        cal.timeInMillis = tx.dateMillis
                        "${cal.get(Calendar.YEAR)}-W${cal.get(Calendar.WEEK_OF_YEAR).toString().padStart(2, '0')}"
                    }
                    .entries.sortedByDescending { it.key }
                    .forEach { (_, txList) ->
                        val from  = txList.minByOrNull { it.dateMillis }?.dateMillis
                        val to    = txList.maxByOrNull { it.dateMillis }?.dateMillis
                        val range = if (from != null && to != null)
                            "${dispFmt.format(Date(from))} – ${dispFmt.format(Date(to))}" else "?"
                        drawGroupHeader("📅  Week: $range")
                        var exp = 0.0; var inc = 0.0
                        txList.forEachIndexed { idx, tx ->
                            drawTxRow(tx, idx + 1, appData.currencyCode)
                            if (tx.type == TransactionType.EXPENSE) exp += tx.amount else inc += tx.amount
                        }
                        drawGroupTotal(exp, inc, appData.currencyCode, "Week")
                    }
            }

            PdfSplitType.MONTH -> {
                val keyFmt  = SimpleDateFormat("yyyy-MM",   Locale.US)
                val dispFmt = SimpleDateFormat("MMMM yyyy", Locale.US)
                sortedTx
                    .groupBy { keyFmt.format(Date(it.dateMillis)) }
                    .entries.sortedByDescending { it.key }
                    .forEach { (monthKey, txList) ->
                        val header = try {
                            dispFmt.format(
                                SimpleDateFormat("yyyy-MM-dd", Locale.US).parse("$monthKey-01")!!
                            )
                        } catch (_: Exception) { monthKey }
                        drawGroupHeader("📅  $header")
                        var exp = 0.0; var inc = 0.0
                        txList.forEachIndexed { idx, tx ->
                            drawTxRow(tx, idx + 1, appData.currencyCode)
                            if (tx.type == TransactionType.EXPENSE) exp += tx.amount else inc += tx.amount
                        }
                        drawGroupTotal(exp, inc, appData.currencyCode, "Month")
                    }
            }
        }
    }

    finishPage()
    val out = ByteArrayOutputStream()
    document.writeTo(out)
    document.close()
    return out.toByteArray()
}
