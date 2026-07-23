package com.renovationtracker.data

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import java.io.File
import java.io.InputStream
import java.io.OutputStream

/**
 * Full-database backup (a portable copy of the SQLite file) and restore
 * (reads a backup file's rows so the live database can be repopulated in place).
 */
object BackupManager {

    /** Write the current database to [output] (a file the user picked via the share sheet). */
    fun exportDatabase(context: Context, output: OutputStream) {
        // Flush the write-ahead log into the main db file so the copy is complete.
        val db = AppDatabase.get(context)
        db.query("PRAGMA wal_checkpoint(FULL)", null).use { it.moveToFirst() }

        val dbFile = context.getDatabasePath(AppDatabase.DB_NAME)
        dbFile.inputStream().use { input -> input.copyTo(output) }
        output.flush()
    }

    /**
     * Read every row out of a backup `.db` file supplied as [input].
     * The file is copied to a temp location, opened read-only, and parsed —
     * the live database is never touched here (the repository writes the result).
     */
    fun readBackup(context: Context, input: InputStream): DataSnapshot {
        val temp = File(context.cacheDir, "restore_temp.db")
        try {
            temp.outputStream().use { out -> input.copyTo(out) }
            val src = SQLiteDatabase.openDatabase(temp.path, null, SQLiteDatabase.OPEN_READONLY)
            src.use { db ->
                return DataSnapshot(
                    projects = db.readAll("projects") { c ->
                        Project(
                            id = c.long("id"),
                            name = c.string("name"),
                            description = c.string("description"),
                            address = c.string("address"),
                            createdAt = c.long("createdAt"),
                        )
                    },
                    vendors = db.readAll("vendors") { c ->
                        Vendor(
                            id = c.long("id"), projectId = c.long("projectId"), name = c.string("name"),
                            contact = c.string("contact"), phone = c.string("phone"), email = c.string("email"),
                            website = c.string("website"), notes = c.string("notes"),
                        )
                    },
                    paint = db.readAll("paint_colors") { c ->
                        PaintColor(
                            id = c.long("id"), projectId = c.long("projectId"), room = c.string("room"),
                            brand = c.string("brand"), colorName = c.string("colorName"),
                            paintCode = c.string("paintCode"), sheen = c.string("sheen"),
                            hex = c.string("hex"), notes = c.string("notes"),
                        )
                    },
                    equipment = db.readAll("equipment") { c ->
                        Equipment(
                            id = c.long("id"), projectId = c.long("projectId"), name = c.string("name"),
                            itemNumber = c.string("itemNumber"), vendorName = c.string("vendorName"),
                            unitCost = c.double("unitCost"), quantity = c.double("quantity"), notes = c.string("notes"),
                        )
                    },
                    labor = db.readAll("labor") { c ->
                        Labor(
                            id = c.long("id"), projectId = c.long("projectId"), description = c.string("description"),
                            vendorName = c.string("vendorName"), hours = c.double("hours"), rate = c.double("rate"),
                            flatCost = c.double("flatCost"), notes = c.string("notes"),
                        )
                    },
                )
            }
        } finally {
            temp.delete()
        }
    }

    private fun <T> SQLiteDatabase.readAll(table: String, map: (Cursor) -> T): List<T> {
        val out = ArrayList<T>()
        rawQuery("SELECT * FROM $table", null).use { c ->
            while (c.moveToNext()) out.add(map(c))
        }
        return out
    }

    private fun Cursor.long(name: String) = getLong(getColumnIndexOrThrow(name))
    private fun Cursor.double(name: String) = getDouble(getColumnIndexOrThrow(name))
    private fun Cursor.string(name: String) = getString(getColumnIndexOrThrow(name)) ?: ""
}
