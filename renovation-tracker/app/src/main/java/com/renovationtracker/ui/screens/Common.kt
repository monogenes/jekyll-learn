package com.renovationtracker.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.util.Locale

/** A single-line labelled text field used across the edit dialogs. */
@Composable
fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    )
}

/** Wraps form fields in a scrollable column body for dialogs. */
@Composable
fun FormBody(content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(top = 4.dp)) { content() }
}

fun formatMoney(value: Double): String = "$" + String.format(Locale.US, "%,.2f", value)

fun parseDouble(text: String): Double = text.trim().replace(",", "").toDoubleOrNull() ?: 0.0

fun formatNumber(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString()
    else String.format(Locale.US, "%.2f", value)
