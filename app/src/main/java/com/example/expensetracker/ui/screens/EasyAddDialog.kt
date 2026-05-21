package com.example.expensetracker.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.expensetracker.model.ExpenseProfile
import com.example.expensetracker.model.MoneyTransaction
import com.example.expensetracker.model.TransactionType
import com.example.expensetracker.utils.CategoryIconUtils
import com.example.expensetracker.utils.ParsedTransactionDraft
import com.example.expensetracker.utils.formatDate
import com.example.expensetracker.utils.formatDisplayDateStr
import com.example.expensetracker.utils.parseQuickAdd
import com.example.expensetracker.utils.parseDateStart

/**
 * Easy Add dialog – local rule-based quick entry from text or voice.
 *
 * Flow:
 * 1. User types a natural-language note and taps "Understand".
 * 2. The local rule-based parser produces a [ParsedTransactionDraft].
 * 3. A confirmation card is shown with type / title / category / amount / date / note.
 * 4. Buttons: Edit Details (pre-fills the parent form), Confirm Add (saves), Cancel.
 */
@Composable
fun EasyAddDialog(
    profile: ExpenseProfile,
    currencyCode: String,
    onAddTransaction: (MoneyTransaction, onDone: () -> Unit) -> Unit,
    onAddCategory: (String) -> Unit,
    onEditDetails: (ParsedTransactionDraft) -> Unit,
    onDismiss: () -> Unit
) {
    var inputText    by rememberSaveable { mutableStateOf("") }
    var parseError   by rememberSaveable { mutableStateOf<String?>(null) }
    var draft        by remember { mutableStateOf<ParsedTransactionDraft?>(null) }
    var isSaving     by remember { mutableStateOf(false) }
    var saveError    by remember { mutableStateOf<String?>(null) }
    var voiceError   by remember { mutableStateOf<String?>(null) }
    var isListening  by remember { mutableStateOf(false) }
    var saveLocked   by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // ── Helper: run the parser and fill draft ────────────────────────────────
    fun runParser(text: String) {
        parseError = null
        saveError  = null
        draft      = null
        val today  = formatDate(System.currentTimeMillis())
        try {
            draft = parseQuickAdd(
                input               = text,
                defaultCurrency     = currencyCode,
                todayDateStr        = today,
                availableCategories = profile.categoryObjects
            )
        } catch (e: IllegalArgumentException) {
            parseError = e.message
        }
    }

    // ── Speech recognizer launcher ───────────────────────────────────────────
    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListening = false
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val recognized = matches?.firstOrNull()?.trim()
            if (!recognized.isNullOrBlank()) {
                inputText  = recognized
                voiceError = null
                runParser(recognized)      // auto-parse; verify card appears immediately
            } else {
                voiceError = "Could not understand. Please try again."
            }
        }
        // RESULT_CANCELED: user dismissed overlay — do nothing
    }

    // ── Permission launcher ──────────────────────────────────────────────────
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            isListening = true
            voiceError  = null
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE,            "en-IN")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-IN")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Say your expense or income")
            }
            try {
                speechLauncher.launch(intent)
            } catch (_: Exception) {
                isListening = false
                voiceError  = "Voice recognition is not available on this device."
            }
        } else {
            voiceError = "Microphone permission is required for voice input."
        }
    }

    // ── Function: initiate voice input ───────────────────────────────────────
    fun startVoice() {
        voiceError = null
        // Check if speech recognition is supported
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            voiceError = "Voice recognition is not available on this device."
            return
        }
        when {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                isListening = true
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE,            "en-IN")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-IN")
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Say your expense or income")
                }
                try {
                    speechLauncher.launch(intent)
                } catch (_: Exception) {
                    isListening = false
                    voiceError  = "Voice recognition is not available on this device."
                }
            }
            else -> permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Dialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // ── Header ──────────────────────────────────────────────────
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("✨", fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Easy Add",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF101828)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Type or speak a quick note and verify before saving.",
                    fontSize = 13.sp,
                    color = Color(0xFF667085)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // ── Input field ─────────────────────────────────────────────
                OutlinedTextField(
                    value = inputText,
                    onValueChange = {
                        inputText  = it
                        parseError = null
                        saveError  = null
                        voiceError = null
                        if (draft != null) draft = null  // reset on new input
                    },
                    label   = { Text("Quick note") },
                    placeholder = { Text("Example: food 60 morning") },
                    modifier    = Modifier.fillMaxWidth(),
                    shape       = RoundedCornerShape(16.dp),
                    singleLine  = true,
                    isError     = parseError != null,
                    supportingText = parseError?.let { { Text(it, color = Color(0xFFDC2626), fontSize = 12.sp) } }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // ── Voice button ─────────────────────────────────────────────
                OutlinedButton(
                    onClick  = { startVoice() },
                    enabled  = !isListening && !isSaving,
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(16.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF7C3AED)
                    )
                ) {
                    if (isListening) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(16.dp),
                            color       = Color(0xFF7C3AED),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Listening...", fontWeight = FontWeight.SemiBold)
                    } else {
                        Text("🎤  Speak", fontWeight = FontWeight.SemiBold)
                    }
                }

                // Voice error
                voiceError?.let { err ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text     = err,
                        fontSize = 12.sp,
                        color    = Color(0xFFDC2626),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFEF2F2), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ── Understand button ────────────────────────────────────────
                Button(
                    onClick = { runParser(inputText) },
                    modifier  = Modifier.fillMaxWidth(),
                    shape     = RoundedCornerShape(16.dp),
                    colors    = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2563EB),
                        contentColor   = Color.White
                    )
                ) {
                    Text("Understand", fontWeight = FontWeight.SemiBold)
                }

                // ── Confirmation card ────────────────────────────────────────
                draft?.let { d ->
                    Spacer(modifier = Modifier.height(18.dp))

                    HorizontalDivider(color = Color(0xFFE4E7EC))

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Please verify before saving",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF344054)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Confirm card details
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF5F9FF), RoundedCornerShape(16.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ConfirmRow(
                            label = "Type",
                            value = if (d.type == TransactionType.EXPENSE) "Expense" else "Income",
                            valueColor = if (d.type == TransactionType.EXPENSE)
                                Color(0xFFDC2626) else Color(0xFF059669)
                        )
                        ConfirmRow(label = "Title",    value = d.title)
                        val catIcon = profile.categoryObjects
                            .firstOrNull { it.name.equals(d.category, ignoreCase = true) }
                            ?.iconKey
                            .let { CategoryIconUtils.iconForCategory(d.category, it) }
                        ConfirmRow(
                            label = "Category",
                            value = "$catIcon  ${d.category}"
                        )
                        ConfirmRow(
                            label = "Amount",
                            value = "${d.currency} ${d.amount}",
                            valueColor = Color(0xFF101828)
                        )
                        ConfirmRow(label = "Date", value = formatDisplayDateStr(d.dateStr))
                        if (d.note.isNotBlank()) {
                            ConfirmRow(label = "Note", value = d.note, valueColor = Color(0xFF667085))
                        }
                    }

                    // Category-not-in-profile warning
                    val categoryExists = profile.categories
                        .any { it.equals(d.category, ignoreCase = true) }
                    if (!categoryExists) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "ℹ️ \"${d.category}\" is not in your categories. " +
                                    "It will be added automatically.",
                            fontSize = 12.sp,
                            color = Color(0xFF92400E),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFFFBEB), RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        )
                    }

                    saveError?.let { err ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = err,
                            fontSize = 12.sp,
                            color = Color(0xFFDC2626),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFEF2F2), RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ── Action buttons ───────────────────────────────────────
                    // Confirm Add (primary)
                    Button(
                        onClick = {
                            if (saveLocked || isSaving) return@Button
                            saveLocked = true
                            isSaving   = true
                            saveError  = null

                            // Ensure category exists in profile before saving
                            if (!categoryExists) {
                                onAddCategory(d.category)
                            }

                            val dateMillis = parseDateStart(d.dateStr)
                                ?: System.currentTimeMillis()

                            val tx = MoneyTransaction(
                                type            = d.type,
                                title           = d.title,
                                category        = d.category,
                                amount          = d.amount,
                                baseCurrency    = d.currency,
                                displayCurrency = d.currency,
                                dateMillis      = dateMillis,
                                note            = d.note
                            )

                            onAddTransaction(tx) {
                                isSaving   = false
                                saveLocked = false
                            }
                        },
                        enabled   = !isSaving,
                        modifier  = Modifier.fillMaxWidth(),
                        shape     = RoundedCornerShape(16.dp),
                        colors    = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF059669),
                            contentColor   = Color.White
                        )
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(18.dp),
                                color       = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Saving...", fontWeight = FontWeight.Bold)
                        } else {
                            Text("Confirm Add", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Edit Details / Cancel row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onEditDetails(d) },
                            enabled  = !isSaving,
                            modifier = Modifier.weight(1f),
                            shape    = RoundedCornerShape(14.dp),
                            colors   = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF2563EB)
                            )
                        ) {
                            Text("Edit Details")
                        }

                        TextButton(
                            onClick  = { if (!isSaving) onDismiss() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", color = Color(0xFF667085))
                        }
                    }
                }

                // ── Cancel when no draft yet ─────────────────────────────────
                if (draft == null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick  = onDismiss,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Cancel", color = Color(0xFF667085))
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helper composable for a label/value row inside the confirm card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ConfirmRow(
    label: String,
    value: String,
    valueColor: Color = Color(0xFF101828)
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text  = label,
            fontSize = 13.sp,
            color = Color(0xFF667085),
            modifier = Modifier.weight(0.38f)
        )
        Text(
            text  = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = valueColor,
            modifier = Modifier.weight(0.62f)
        )
    }
}
