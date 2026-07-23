package com.renovationtracker.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.renovationtracker.data.CsvExporter
import com.renovationtracker.data.BackupManager
import com.renovationtracker.data.Equipment
import com.renovationtracker.data.Labor
import com.renovationtracker.data.PaintColor
import com.renovationtracker.data.Project
import com.renovationtracker.data.Repository
import com.renovationtracker.data.Vendor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repository(app)

    val projects: StateFlow<List<Project>> =
        repo.observeProjects().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun project(id: Long): Flow<Project?> = repo.observeProject(id)
    fun vendors(projectId: Long): Flow<List<Vendor>> = repo.observeVendors(projectId)
    fun paint(projectId: Long): Flow<List<PaintColor>> = repo.observePaint(projectId)
    fun equipment(projectId: Long): Flow<List<Equipment>> = repo.observeEquipment(projectId)
    fun labor(projectId: Long): Flow<List<Labor>> = repo.observeLabor(projectId)

    // --- Mutations ---
    // New projects insert; edits must UPDATE — a REPLACE-insert would cascade-delete
    // the project's paint/vendor/equipment/labor rows.
    fun saveProject(p: Project) = launch {
        if (p.id == 0L) repo.upsertProject(p) else repo.updateProject(p)
    }
    fun deleteProject(p: Project) = launch { repo.deleteProject(p) }
    fun saveVendor(v: Vendor) = launch { repo.upsertVendor(v) }
    fun deleteVendor(v: Vendor) = launch { repo.deleteVendor(v) }
    fun savePaint(p: PaintColor) = launch { repo.upsertPaint(p) }
    fun deletePaint(p: PaintColor) = launch { repo.deletePaint(p) }
    fun saveEquipment(e: Equipment) = launch { repo.upsertEquipment(e) }
    fun deleteEquipment(e: Equipment) = launch { repo.deleteEquipment(e) }
    fun saveLabor(l: Labor) = launch { repo.upsertLabor(l) }
    fun deleteLabor(l: Labor) = launch { repo.deleteLabor(l) }

    // --- One-shot user feedback (snackbar) ---
    private val _messages = MutableStateFlow<String?>(null)
    val messages: StateFlow<String?> = _messages
    fun clearMessage() { _messages.value = null }

    /** Export every table (optionally scoped to one project) to a CSV at [uri]. */
    fun exportCsv(uri: Uri, projectId: Long?) = launch {
        runCatching {
            val snapshot = repo.snapshot(projectId)
            val csv = CsvExporter.build(snapshot)
            withContext(Dispatchers.IO) {
                getApplication<Application>().contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(csv.toByteArray(Charsets.UTF_8))
                } ?: error("Could not open file")
            }
        }.onSuccess { _messages.value = "CSV exported" }
            .onFailure { _messages.value = "Export failed: ${it.message}" }
    }

    /** Copy the whole database to [uri] as a portable backup file. */
    fun backupDatabase(uri: Uri) = launch {
        runCatching {
            withContext(Dispatchers.IO) {
                getApplication<Application>().contentResolver.openOutputStream(uri)?.use { out ->
                    BackupManager.exportDatabase(getApplication(), out)
                } ?: error("Could not open file")
            }
        }.onSuccess { _messages.value = "Backup saved" }
            .onFailure { _messages.value = "Backup failed: ${it.message}" }
    }

    /** Replace all data with a previously saved backup at [uri]. */
    fun restoreDatabase(uri: Uri) = launch {
        runCatching {
            val input = withContext(Dispatchers.IO) {
                getApplication<Application>().contentResolver.openInputStream(uri)
            } ?: error("Could not open file")
            input.use { repo.restoreFromBackup(it) }
        }.onSuccess { _messages.value = "Backup restored" }
            .onFailure { _messages.value = "Restore failed: ${it.message}" }
    }

    private fun launch(block: suspend () -> Unit) = viewModelScope.launch { block() }
}
