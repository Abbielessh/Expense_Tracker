package com.example.expensetracker.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.expensetracker.model.ExpenseAppData
import com.example.expensetracker.model.ExpenseProfile
import com.example.expensetracker.model.TransactionType
import java.io.ByteArrayOutputStream

fun createExpensePdf(
    context: Context,
    appData: ExpenseAppData,
    profile: ExpenseProfile
): ByteArray {
    val document = PdfDocument()
    val pageWidth = 595
    val pageHeight = 842
    val margin = 36
    var pageNumber = 1

    fun newPage(): Pair<PdfDocument.Page, Canvas> {
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        val page = document.startPage(pageInfo)
        return page to page.canvas
    }

    val titlePaint = Paint().apply {
        color = AndroidColor.rgb(37, 99, 235)
        textSize = 22f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    val headerPaint = Paint().apply {
        color = AndroidColor.rgb(16, 24, 40)
        textSize = 15f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    val normalPaint = Paint().apply {
        color = AndroidColor.rgb(52, 64, 84)
        textSize = 12f
    }

    val smallPaint = Paint().apply {
        color = AndroidColor.rgb(102, 112, 133)
        textSize = 10f
    }

    var pair = newPage()
    var page = pair.first
    var canvas = pair.second
    var y = 50

    fun drawLine(text: String, paint: Paint = normalPaint, gap: Int = 18) {
        canvas.drawText(text, margin.toFloat(), y.toFloat(), paint)
        y += gap
    }

    fun finishCurrentPage() {
        document.finishPage(page)
        pageNumber++
    }

    fun ensureSpace(requiredSpace: Int) {
        if (y + requiredSpace > pageHeight - margin) {
            finishCurrentPage()
            val newPair = newPage()
            page = newPair.first
            canvas = newPair.second
            y = 50
        }
    }

    val totalExpense = profile.transactions
        .filter { it.type == TransactionType.EXPENSE }
        .sumOf { it.amount }

    val totalIncome = profile.transactions
        .filter { it.type == TransactionType.INCOME }
        .sumOf { it.amount }

    val balance = totalIncome - totalExpense

    canvas.drawText("Expense Tracker Report", margin.toFloat(), y.toFloat(), titlePaint)
    y += 28

    drawLine("Profile: ${profile.name}", headerPaint)
    drawLine("Currency: ${appData.currencyCode}", normalPaint)
    drawLine("Generated Date: ${formatDate(System.currentTimeMillis())}", normalPaint)
    y += 8

    drawLine("Summary", headerPaint)
    drawLine("Total Income: ${formatMoney(totalIncome, appData.currencyCode)}")
    drawLine("Total Expense: ${formatMoney(totalExpense, appData.currencyCode)}")
    drawLine("Balance: ${formatMoney(balance, appData.currencyCode)}")
    y += 10

    drawLine("Category Totals", headerPaint)

    val categoryTotals = profile.transactions
        .filter { it.type == TransactionType.EXPENSE }
        .groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { it.amount } }
        .toList()
        .sortedByDescending { it.second }

    if (categoryTotals.isEmpty()) {
        drawLine("No expense categories found.", normalPaint)
    } else {
        categoryTotals.forEach { pairTotal ->
            ensureSpace(22)
            drawLine(
                "${pairTotal.first}: ${formatMoney(pairTotal.second, appData.currencyCode)}",
                normalPaint
            )
        }
    }

    y += 10

    drawLine("Transactions", headerPaint)

    val sortedTransactions = profile.transactions.sortedByDescending { it.dateMillis }

    if (sortedTransactions.isEmpty()) {
        drawLine("No transactions found.", normalPaint)
    } else {
        sortedTransactions.forEachIndexed { index, transaction ->
            ensureSpace(54)

            val sign = if (transaction.type == TransactionType.EXPENSE) "-" else "+"
            val line1 = "${index + 1}. ${formatDate(transaction.dateMillis)} | ${transaction.title}"
            val line2 = "${transaction.category} | $sign ${formatMoney(transaction.amount, appData.currencyCode)}"

            drawLine(line1, normalPaint, 15)
            drawLine(line2, smallPaint, 15)

            if (transaction.note.isNotBlank()) {
                val safeNote = if (transaction.note.length > 80) {
                    transaction.note.take(80) + "..."
                } else {
                    transaction.note
                }
                drawLine("Note: $safeNote", smallPaint, 15)
            }

            y += 4
        }
    }

    finishCurrentPage()

    val outputStream = ByteArrayOutputStream()
    document.writeTo(outputStream)
    document.close()

    return outputStream.toByteArray()
}
