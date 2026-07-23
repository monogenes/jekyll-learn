package com.renovationtracker.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.renovationtracker.data.Equipment
import com.renovationtracker.data.Labor
import com.renovationtracker.data.PaintColor
import com.renovationtracker.data.Project
import com.renovationtracker.data.Vendor

@Composable
private fun EditorScaffold(
    title: String,
    saveEnabled: Boolean,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    body: @Composable () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) { body() }
        },
        confirmButton = { TextButton(onClick = onSave, enabled = saveEnabled) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun ProjectEditorDialog(
    initial: Project?,
    onDismiss: () -> Unit,
    onSave: (Project) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var address by remember { mutableStateOf(initial?.address ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }

    EditorScaffold(
        title = if (initial == null) "New Project" else "Edit Project",
        saveEnabled = name.isNotBlank(),
        onDismiss = onDismiss,
        onSave = {
            onSave(
                (initial ?: Project(name = "")).copy(
                    name = name.trim(), address = address.trim(), description = description.trim(),
                ),
            )
        },
    ) {
        FormField("Project name *", name, { name = it })
        FormField("Address", address, { address = it })
        FormField("Description", description, { description = it }, singleLine = false)
    }
}

@Composable
fun PaintEditorDialog(
    projectId: Long,
    initial: PaintColor?,
    onDismiss: () -> Unit,
    onSave: (PaintColor) -> Unit,
) {
    var room by remember { mutableStateOf(initial?.room ?: "") }
    var brand by remember { mutableStateOf(initial?.brand ?: "") }
    var colorName by remember { mutableStateOf(initial?.colorName ?: "") }
    var paintCode by remember { mutableStateOf(initial?.paintCode ?: "") }
    var sheen by remember { mutableStateOf(initial?.sheen ?: "") }
    var hex by remember { mutableStateOf(initial?.hex ?: "") }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }

    EditorScaffold(
        title = if (initial == null) "New Paint / Color" else "Edit Paint / Color",
        saveEnabled = colorName.isNotBlank() || paintCode.isNotBlank(),
        onDismiss = onDismiss,
        onSave = {
            onSave(
                (initial ?: PaintColor(projectId = projectId)).copy(
                    projectId = projectId,
                    room = room.trim(), brand = brand.trim(), colorName = colorName.trim(),
                    paintCode = paintCode.trim(), sheen = sheen.trim(), hex = hex.trim(),
                    notes = notes.trim(),
                ),
            )
        },
    ) {
        FormField("Room / surface", room, { room = it })
        FormField("Brand", brand, { brand = it })
        FormField("Color name", colorName, { colorName = it })
        FormField("Paint code", paintCode, { paintCode = it })
        FormField("Sheen (flat, eggshell, ...)", sheen, { sheen = it })
        FormField("Hex (optional, e.g. #F2B84B)", hex, { hex = it })
        FormField("Notes", notes, { notes = it }, singleLine = false)
    }
}

@Composable
fun VendorEditorDialog(
    projectId: Long,
    initial: Vendor?,
    onDismiss: () -> Unit,
    onSave: (Vendor) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var contact by remember { mutableStateOf(initial?.contact ?: "") }
    var phone by remember { mutableStateOf(initial?.phone ?: "") }
    var email by remember { mutableStateOf(initial?.email ?: "") }
    var website by remember { mutableStateOf(initial?.website ?: "") }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }

    EditorScaffold(
        title = if (initial == null) "New Vendor" else "Edit Vendor",
        saveEnabled = name.isNotBlank(),
        onDismiss = onDismiss,
        onSave = {
            onSave(
                (initial ?: Vendor(projectId = projectId, name = "")).copy(
                    projectId = projectId,
                    name = name.trim(), contact = contact.trim(), phone = phone.trim(),
                    email = email.trim(), website = website.trim(), notes = notes.trim(),
                ),
            )
        },
    ) {
        FormField("Vendor name *", name, { name = it })
        FormField("Contact person", contact, { contact = it })
        FormField("Phone", phone, { phone = it }, keyboardType = KeyboardType.Phone)
        FormField("Email", email, { email = it }, keyboardType = KeyboardType.Email)
        FormField("Website", website, { website = it }, keyboardType = KeyboardType.Uri)
        FormField("Notes", notes, { notes = it }, singleLine = false)
    }
}

@Composable
fun EquipmentEditorDialog(
    projectId: Long,
    initial: Equipment?,
    onDismiss: () -> Unit,
    onSave: (Equipment) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var itemNumber by remember { mutableStateOf(initial?.itemNumber ?: "") }
    var vendorName by remember { mutableStateOf(initial?.vendorName ?: "") }
    var unitCost by remember { mutableStateOf(initial?.unitCost?.let { if (it == 0.0) "" else it.toString() } ?: "") }
    var quantity by remember { mutableStateOf(initial?.quantity?.let { formatNumber(it) } ?: "1") }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }

    EditorScaffold(
        title = if (initial == null) "New Equipment / Material" else "Edit Equipment / Material",
        saveEnabled = name.isNotBlank(),
        onDismiss = onDismiss,
        onSave = {
            onSave(
                (initial ?: Equipment(projectId = projectId, name = "")).copy(
                    projectId = projectId,
                    name = name.trim(), itemNumber = itemNumber.trim(), vendorName = vendorName.trim(),
                    unitCost = parseDouble(unitCost),
                    quantity = parseDouble(quantity).let { if (it <= 0.0) 1.0 else it },
                    notes = notes.trim(),
                ),
            )
        },
    ) {
        FormField("Item name *", name, { name = it })
        FormField("Item / model number", itemNumber, { itemNumber = it })
        FormField("Vendor", vendorName, { vendorName = it })
        FormField("Unit cost", unitCost, { unitCost = it }, keyboardType = KeyboardType.Decimal)
        FormField("Quantity", quantity, { quantity = it }, keyboardType = KeyboardType.Decimal)
        FormField("Notes", notes, { notes = it }, singleLine = false)
    }
}

@Composable
fun LaborEditorDialog(
    projectId: Long,
    initial: Labor?,
    onDismiss: () -> Unit,
    onSave: (Labor) -> Unit,
) {
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var vendorName by remember { mutableStateOf(initial?.vendorName ?: "") }
    var hours by remember { mutableStateOf(initial?.hours?.let { if (it == 0.0) "" else formatNumber(it) } ?: "") }
    var rate by remember { mutableStateOf(initial?.rate?.let { if (it == 0.0) "" else it.toString() } ?: "") }
    var flatCost by remember { mutableStateOf(initial?.flatCost?.let { if (it == 0.0) "" else it.toString() } ?: "") }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }

    EditorScaffold(
        title = if (initial == null) "New Labor" else "Edit Labor",
        saveEnabled = description.isNotBlank(),
        onDismiss = onDismiss,
        onSave = {
            onSave(
                (initial ?: Labor(projectId = projectId, description = "")).copy(
                    projectId = projectId,
                    description = description.trim(), vendorName = vendorName.trim(),
                    hours = parseDouble(hours), rate = parseDouble(rate), flatCost = parseDouble(flatCost),
                    notes = notes.trim(),
                ),
            )
        },
    ) {
        FormField("Description *", description, { description = it })
        FormField("Contractor / vendor", vendorName, { vendorName = it })
        FormField("Hours", hours, { hours = it }, keyboardType = KeyboardType.Decimal)
        FormField("Hourly rate", rate, { rate = it }, keyboardType = KeyboardType.Decimal)
        FormField("Flat cost (overrides hours × rate)", flatCost, { flatCost = it }, keyboardType = KeyboardType.Decimal)
        FormField("Notes", notes, { notes = it }, singleLine = false)
    }
}
