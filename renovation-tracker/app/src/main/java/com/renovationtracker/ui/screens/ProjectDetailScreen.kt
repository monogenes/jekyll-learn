package com.renovationtracker.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.renovationtracker.data.Equipment
import com.renovationtracker.data.Labor
import com.renovationtracker.data.PaintColor
import com.renovationtracker.data.Vendor
import com.renovationtracker.ui.AppViewModel

private val TABS = listOf("Paint", "Vendors", "Equipment", "Labor", "Summary")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    vm: AppViewModel,
    projectId: Long,
    onBack: () -> Unit,
) {
    val project by vm.project(projectId).collectAsStateWithLifecycle(initialValue = null)
    val paint by vm.paint(projectId).collectAsStateWithLifecycle(initialValue = emptyList())
    val vendors by vm.vendors(projectId).collectAsStateWithLifecycle(initialValue = emptyList())
    val equipment by vm.equipment(projectId).collectAsStateWithLifecycle(initialValue = emptyList())
    val labor by vm.labor(projectId).collectAsStateWithLifecycle(initialValue = emptyList())

    var tab by remember { mutableIntStateOf(0) }
    var menuOpen by remember { mutableStateOf(false) }

    // Editor dialog state (null = closed; non-null holds the record being edited, or a "new" marker).
    var showPaint by remember { mutableStateOf(false) }
    var editPaint by remember { mutableStateOf<PaintColor?>(null) }
    var showVendor by remember { mutableStateOf(false) }
    var editVendor by remember { mutableStateOf<Vendor?>(null) }
    var showEquip by remember { mutableStateOf(false) }
    var editEquip by remember { mutableStateOf<Equipment?>(null) }
    var showLabor by remember { mutableStateOf(false) }
    var editLabor by remember { mutableStateOf<Labor?>(null) }

    val exportCsv = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri -> uri?.let { vm.exportCsv(it, projectId) } }

    androidx.compose.material3.Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(project?.name ?: "Project") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Export this project to CSV") },
                            onClick = {
                                menuOpen = false
                                val safe = (project?.name ?: "project").replace(Regex("[^A-Za-z0-9-_]"), "_")
                                exportCsv.launch("$safe.csv")
                            },
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            if (tab != 4) {
                FloatingActionButton(onClick = {
                    when (tab) {
                        0 -> { editPaint = null; showPaint = true }
                        1 -> { editVendor = null; showVendor = true }
                        2 -> { editEquip = null; showEquip = true }
                        3 -> { editLabor = null; showLabor = true }
                    }
                }) { Icon(Icons.Default.Add, contentDescription = "Add") }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ScrollableTabRow(selectedTabIndex = tab, edgePadding = 8.dp) {
                TABS.forEachIndexed { i, title ->
                    Tab(selected = tab == i, onClick = { tab = i }, text = { Text(title) })
                }
            }
            when (tab) {
                0 -> PaintList(paint, onEdit = { editPaint = it; showPaint = true }, onDelete = { vm.deletePaint(it) })
                1 -> VendorList(vendors, onEdit = { editVendor = it; showVendor = true }, onDelete = { vm.deleteVendor(it) })
                2 -> EquipmentList(equipment, onEdit = { editEquip = it; showEquip = true }, onDelete = { vm.deleteEquipment(it) })
                3 -> LaborList(labor, onEdit = { editLabor = it; showLabor = true }, onDelete = { vm.deleteLabor(it) })
                4 -> SummaryTab(equipment, labor)
            }
        }
    }

    if (showPaint) PaintEditorDialog(projectId, editPaint, { showPaint = false }) { vm.savePaint(it); showPaint = false }
    if (showVendor) VendorEditorDialog(projectId, editVendor, { showVendor = false }) { vm.saveVendor(it); showVendor = false }
    if (showEquip) EquipmentEditorDialog(projectId, editEquip, { showEquip = false }) { vm.saveEquipment(it); showEquip = false }
    if (showLabor) LaborEditorDialog(projectId, editLabor, { showLabor = false }) { vm.saveLabor(it); showLabor = false }
}

// --- List item shell with edit/delete overflow ---
@Composable
private fun EntityCard(
    title: String,
    subtitle: String?,
    trailing: String?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth().clickable(onClick = onEdit)) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
            Column(Modifier.fillMaxWidth().padding(end = 40.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                if (!subtitle.isNullOrBlank()) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall)
                }
                if (!trailing.isNullOrBlank()) {
                    Text(
                        trailing,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Box(Modifier.align(Alignment.TopEnd)) {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options")
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Edit") }, onClick = { menu = false; onEdit() })
                    DropdownMenuItem(text = { Text("Delete") }, onClick = { menu = false; onDelete() })
                }
            }
        }
    }
}

@Composable
private fun <T> EntityLazyList(items: List<T>, empty: String, row: @Composable (T) -> Unit) {
    if (items.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(empty, style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) { items(items) { row(it) } }
    }
}

@Composable
private fun PaintList(items: List<PaintColor>, onEdit: (PaintColor) -> Unit, onDelete: (PaintColor) -> Unit) {
    EntityLazyList(items, "No paint or colors yet. Tap + to add one.") { p ->
        val title = listOf(p.colorName, p.paintCode).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "Paint" }
        val sub = listOfNotNull(
            p.room.takeIf { it.isNotBlank() },
            p.brand.takeIf { it.isNotBlank() },
            p.sheen.takeIf { it.isNotBlank() },
        ).joinToString(" • ")
        EntityCard(title, sub.ifBlank { null }, p.notes.takeIf { it.isNotBlank() }, { onEdit(p) }, { onDelete(p) })
    }
}

@Composable
private fun VendorList(items: List<Vendor>, onEdit: (Vendor) -> Unit, onDelete: (Vendor) -> Unit) {
    EntityLazyList(items, "No vendors yet. Tap + to add one.") { v ->
        val sub = listOfNotNull(
            v.contact.takeIf { it.isNotBlank() },
            v.phone.takeIf { it.isNotBlank() },
            v.email.takeIf { it.isNotBlank() },
            v.website.takeIf { it.isNotBlank() },
        ).joinToString(" • ")
        EntityCard(v.name, sub.ifBlank { null }, v.notes.takeIf { it.isNotBlank() }, { onEdit(v) }, { onDelete(v) })
    }
}

@Composable
private fun EquipmentList(items: List<Equipment>, onEdit: (Equipment) -> Unit, onDelete: (Equipment) -> Unit) {
    EntityLazyList(items, "No equipment or materials yet. Tap + to add one.") { e ->
        val sub = listOfNotNull(
            e.itemNumber.takeIf { it.isNotBlank() }?.let { "#$it" },
            e.vendorName.takeIf { it.isNotBlank() },
            "${formatNumber(e.quantity)} × ${formatMoney(e.unitCost)}",
        ).joinToString(" • ")
        EntityCard(e.name, sub, formatMoney(e.total), { onEdit(e) }, { onDelete(e) })
    }
}

@Composable
private fun LaborList(items: List<Labor>, onEdit: (Labor) -> Unit, onDelete: (Labor) -> Unit) {
    EntityLazyList(items, "No labor entries yet. Tap + to add one.") { l ->
        val basis = if (l.flatCost > 0.0) "flat" else "${formatNumber(l.hours)} hr × ${formatMoney(l.rate)}"
        val sub = listOfNotNull(l.vendorName.takeIf { it.isNotBlank() }, basis).joinToString(" • ")
        EntityCard(l.description, sub, formatMoney(l.total), { onEdit(l) }, { onDelete(l) })
    }
}

@Composable
private fun SummaryTab(equipment: List<Equipment>, labor: List<Labor>) {
    val materials = equipment.sumOf { it.total }
    val laborTotal = labor.sumOf { it.total }
    val grand = materials + laborTotal
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SummaryRow("Materials & equipment", formatMoney(materials))
        SummaryRow("Labor", formatMoney(laborTotal))
        HorizontalDivider()
        SummaryRow("Grand total", formatMoney(grand), emphasize = true)
        Text(
            "${equipment.size} equipment/material items • ${labor.size} labor entries",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun SummaryRow(label: String, value: String, emphasize: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            label,
            style = if (emphasize) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
        )
        Text(
            value,
            style = if (emphasize) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}
