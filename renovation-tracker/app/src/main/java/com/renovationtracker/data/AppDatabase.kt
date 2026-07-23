package com.renovationtracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Project::class, Vendor::class, PaintColor::class, Equipment::class, Labor::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun vendorDao(): VendorDao
    abstract fun paintColorDao(): PaintColorDao
    abstract fun equipmentDao(): EquipmentDao
    abstract fun laborDao(): LaborDao

    companion object {
        const val DB_NAME = "renovation_tracker.db"

        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context).also { instance = it }
            }

        private fun build(context: Context): AppDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DB_NAME,
            ).build()
    }
}
