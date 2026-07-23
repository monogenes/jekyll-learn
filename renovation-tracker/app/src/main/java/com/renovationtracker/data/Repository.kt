package com.renovationtracker.data

import android.content.Context
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.InputStream

/** Single entry point the UI layer uses to read and mutate data. */
class Repository(private val appContext: Context) {
    private val db = AppDatabase.get(appContext)
    private val projectDao = db.projectDao()
    private val vendorDao = db.vendorDao()
    private val paintDao = db.paintColorDao()
    private val equipmentDao = db.equipmentDao()
    private val laborDao = db.laborDao()

    // Projects
    fun observeProjects(): Flow<List<Project>> = projectDao.observeAll()
    fun observeProject(id: Long): Flow<Project?> = projectDao.observe(id)
    suspend fun upsertProject(p: Project): Long = projectDao.upsert(p)
    suspend fun updateProject(p: Project) = projectDao.update(p)
    suspend fun deleteProject(p: Project) = projectDao.delete(p)

    // Vendors
    fun observeVendors(projectId: Long): Flow<List<Vendor>> = vendorDao.observeForProject(projectId)
    suspend fun upsertVendor(v: Vendor): Long = vendorDao.upsert(v)
    suspend fun deleteVendor(v: Vendor) = vendorDao.delete(v)

    // Paint / colors
    fun observePaint(projectId: Long): Flow<List<PaintColor>> = paintDao.observeForProject(projectId)
    suspend fun upsertPaint(p: PaintColor): Long = paintDao.upsert(p)
    suspend fun deletePaint(p: PaintColor) = paintDao.delete(p)

    // Equipment / materials
    fun observeEquipment(projectId: Long): Flow<List<Equipment>> = equipmentDao.observeForProject(projectId)
    suspend fun upsertEquipment(e: Equipment): Long = equipmentDao.upsert(e)
    suspend fun deleteEquipment(e: Equipment) = equipmentDao.delete(e)

    // Labor
    fun observeLabor(projectId: Long): Flow<List<Labor>> = laborDao.observeForProject(projectId)
    suspend fun upsertLabor(l: Labor): Long = laborDao.upsert(l)
    suspend fun deleteLabor(l: Labor) = laborDao.delete(l)

    /**
     * Replace all data with the contents of a backup `.db` supplied as [input].
     * Runs in a single transaction so a failure leaves the current data intact.
     * Row IDs are preserved; child rows cascade-clear when projects are wiped.
     */
    suspend fun restoreFromBackup(input: InputStream) {
        val snapshot = withContext(Dispatchers.IO) { BackupManager.readBackup(appContext, input) }
        db.withTransaction {
            projectDao.deleteAll()
            snapshot.projects.forEach { projectDao.upsert(it) }
            snapshot.vendors.forEach { vendorDao.upsert(it) }
            snapshot.paint.forEach { paintDao.upsert(it) }
            snapshot.equipment.forEach { equipmentDao.upsert(it) }
            snapshot.labor.forEach { laborDao.upsert(it) }
        }
    }

    /** Gather every record for a project (or all projects) for CSV export. */
    suspend fun snapshot(projectId: Long?): DataSnapshot {
        val projects = projectDao.getAll().let { all ->
            if (projectId == null) all else all.filter { it.id == projectId }
        }
        val ids = projects.map { it.id }.toSet()
        fun <T> filterByProject(list: List<T>, pid: (T) -> Long): List<T> =
            if (projectId == null) list else list.filter { pid(it) in ids }

        return DataSnapshot(
            projects = projects,
            vendors = filterByProject(vendorDao.getAll()) { it.projectId },
            paint = filterByProject(paintDao.getAll()) { it.projectId },
            equipment = filterByProject(equipmentDao.getAll()) { it.projectId },
            labor = filterByProject(laborDao.getAll()) { it.projectId },
        )
    }
}

data class DataSnapshot(
    val projects: List<Project>,
    val vendors: List<Vendor>,
    val paint: List<PaintColor>,
    val equipment: List<Equipment>,
    val labor: List<Labor>,
)
