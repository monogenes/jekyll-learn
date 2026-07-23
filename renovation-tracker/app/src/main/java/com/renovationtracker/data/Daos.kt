package com.renovationtracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<Project>>

    @Query("SELECT * FROM projects WHERE id = :id")
    fun observe(id: Long): Flow<Project?>

    @Query("SELECT * FROM projects ORDER BY name COLLATE NOCASE")
    suspend fun getAll(): List<Project>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(project: Project): Long

    @Update
    suspend fun update(project: Project)

    @Delete
    suspend fun delete(project: Project)

    /** Wipe all projects; child rows cascade-delete. Used by restore. */
    @Query("DELETE FROM projects")
    suspend fun deleteAll()
}

@Dao
interface VendorDao {
    @Query("SELECT * FROM vendors WHERE projectId = :projectId ORDER BY name COLLATE NOCASE")
    fun observeForProject(projectId: Long): Flow<List<Vendor>>

    @Query("SELECT * FROM vendors WHERE projectId = :projectId ORDER BY name COLLATE NOCASE")
    suspend fun getForProject(projectId: Long): List<Vendor>

    @Query("SELECT * FROM vendors ORDER BY name COLLATE NOCASE")
    suspend fun getAll(): List<Vendor>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(vendor: Vendor): Long

    @Delete
    suspend fun delete(vendor: Vendor)
}

@Dao
interface PaintColorDao {
    @Query("SELECT * FROM paint_colors WHERE projectId = :projectId ORDER BY room COLLATE NOCASE, colorName COLLATE NOCASE")
    fun observeForProject(projectId: Long): Flow<List<PaintColor>>

    @Query("SELECT * FROM paint_colors WHERE projectId = :projectId ORDER BY room COLLATE NOCASE, colorName COLLATE NOCASE")
    suspend fun getForProject(projectId: Long): List<PaintColor>

    @Query("SELECT * FROM paint_colors ORDER BY colorName COLLATE NOCASE")
    suspend fun getAll(): List<PaintColor>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(paintColor: PaintColor): Long

    @Delete
    suspend fun delete(paintColor: PaintColor)
}

@Dao
interface EquipmentDao {
    @Query("SELECT * FROM equipment WHERE projectId = :projectId ORDER BY name COLLATE NOCASE")
    fun observeForProject(projectId: Long): Flow<List<Equipment>>

    @Query("SELECT * FROM equipment WHERE projectId = :projectId ORDER BY name COLLATE NOCASE")
    suspend fun getForProject(projectId: Long): List<Equipment>

    @Query("SELECT * FROM equipment ORDER BY name COLLATE NOCASE")
    suspend fun getAll(): List<Equipment>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(equipment: Equipment): Long

    @Delete
    suspend fun delete(equipment: Equipment)
}

@Dao
interface LaborDao {
    @Query("SELECT * FROM labor WHERE projectId = :projectId ORDER BY description COLLATE NOCASE")
    fun observeForProject(projectId: Long): Flow<List<Labor>>

    @Query("SELECT * FROM labor WHERE projectId = :projectId ORDER BY description COLLATE NOCASE")
    suspend fun getForProject(projectId: Long): List<Labor>

    @Query("SELECT * FROM labor ORDER BY description COLLATE NOCASE")
    suspend fun getAll(): List<Labor>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(labor: Labor): Long

    @Delete
    suspend fun delete(labor: Labor)
}
