package com.renovationtracker.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.renovationtracker.data.Project
import com.renovationtracker.ui.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListScreen(
    vm: AppViewModel,
    onOpenProject: (Long) -> Unit,
) {
    val projects by vm.projects.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Project?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }

    val exportCsv = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri -> uri?.let { vm.exportCsv(it, projectId = null) } }

    val backup = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri -> uri?.let { vm.backupDatabase(it) } }

    val restore = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let { vm.restoreDatabase(it) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reno Tracker") },
                actions = {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Export all to CSV") },
                            onClick = { menuOpen = false; exportCsv.launch("renovation-all.csv") },
                        )
                        DropdownMenuItem(
                            text = { Text("Backup database") },
                            onClick = { menuOpen = false; backup.launch("renovation-backup.db") },
                        )
                        DropdownMenuItem(
                            text = { Text("Restore database") },
                            onClick = { menuOpen = false; restore.launch(arrayOf("*/*")) },
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = null; showEditor = true }) {
                Icon(Icons.Default.Add, contentDescription = "New project")
            }
        },
    ) { padding ->
        if (projects.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    "No projects yet.\nTap + to start tracking a renovation.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(projects, key = { it.id }) { project ->
                    ProjectRow(
                        project = project,
                        onOpen = { onOpenProject(project.id) },
                        onEdit = { editing = project; showEditor = true },
                        onDelete = { vm.deleteProject(project) },
                    )
                }
            }
        }
    }

    if (showEditor) {
        ProjectEditorDialog(
            initial = editing,
            onDismiss = { showEditor = false },
            onSave = { vm.saveProject(it); showEditor = false },
        )
    }
}

@Composable
private fun ProjectRow(
    project: Project,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Box(Modifier.fillMaxWidth().padding(16.dp)) {
            Column(Modifier.fillMaxWidth().padding(end = 40.dp)) {
                Text(project.name, style = MaterialTheme.typography.titleMedium)
                if (project.address.isNotBlank()) {
                    Text(
                        project.address,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (project.description.isNotBlank()) {
                    Text(
                        project.description,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
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
