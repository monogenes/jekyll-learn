package com.renovationtracker.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A renovation project — the top-level container everything else belongs to.
 * e.g. "Kitchen Remodel", "Master Bath".
 */
@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val address: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

/** A supplier / contractor / store used on a project. */
@Entity(
    tableName = "vendors",
    foreignKeys = [ForeignKey(
        entity = Project::class,
        parentColumns = ["id"],
        childColumns = ["projectId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("projectId")],
)
data class Vendor(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val name: String,
    val contact: String = "",
    val phone: String = "",
    val email: String = "",
    val website: String = "",
    val notes: String = "",
)

/** A paint / color selection: brand, code, sheen and where it goes. */
@Entity(
    tableName = "paint_colors",
    foreignKeys = [ForeignKey(
        entity = Project::class,
        parentColumns = ["id"],
        childColumns = ["projectId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("projectId")],
)
data class PaintColor(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val room: String = "",
    val brand: String = "",
    val colorName: String = "",
    val paintCode: String = "",
    val sheen: String = "",
    val hex: String = "",
    val notes: String = "",
)

/** A piece of equipment or a material line-item, with vendor + cost. */
@Entity(
    tableName = "equipment",
    foreignKeys = [ForeignKey(
        entity = Project::class,
        parentColumns = ["id"],
        childColumns = ["projectId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("projectId")],
)
data class Equipment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val name: String,
    val itemNumber: String = "",
    val vendorName: String = "",
    val unitCost: Double = 0.0,
    val quantity: Double = 1.0,
    val notes: String = "",
) {
    val total: Double get() = unitCost * quantity
}

/** A labor line-item. Cost is a flat amount, or hours * rate if flat is 0. */
@Entity(
    tableName = "labor",
    foreignKeys = [ForeignKey(
        entity = Project::class,
        parentColumns = ["id"],
        childColumns = ["projectId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("projectId")],
)
data class Labor(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val description: String,
    val vendorName: String = "",
    val hours: Double = 0.0,
    val rate: Double = 0.0,
    val flatCost: Double = 0.0,
    val notes: String = "",
) {
    val total: Double get() = if (flatCost > 0.0) flatCost else hours * rate
}
