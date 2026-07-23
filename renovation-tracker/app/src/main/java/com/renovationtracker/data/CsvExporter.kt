package com.renovationtracker.data

/** Builds a single CSV document containing every table as a labelled section. */
object CsvExporter {

    fun build(snapshot: DataSnapshot): String = buildString {
        val projectNames = snapshot.projects.associate { it.id to it.name }
        fun name(id: Long) = projectNames[id] ?: "(unknown)"

        section("PROJECTS")
        row("Name", "Description", "Address")
        snapshot.projects.forEach { row(it.name, it.description, it.address) }
        appendLine()

        section("PAINT & COLORS")
        row("Project", "Room", "Brand", "Color Name", "Paint Code", "Sheen", "Hex", "Notes")
        snapshot.paint.forEach {
            row(name(it.projectId), it.room, it.brand, it.colorName, it.paintCode, it.sheen, it.hex, it.notes)
        }
        appendLine()

        section("VENDORS")
        row("Project", "Name", "Contact", "Phone", "Email", "Website", "Notes")
        snapshot.vendors.forEach {
            row(name(it.projectId), it.name, it.contact, it.phone, it.email, it.website, it.notes)
        }
        appendLine()

        section("EQUIPMENT & MATERIALS")
        row("Project", "Item", "Item Number", "Vendor", "Unit Cost", "Qty", "Line Total", "Notes")
        snapshot.equipment.forEach {
            row(
                name(it.projectId), it.name, it.itemNumber, it.vendorName,
                money(it.unitCost), num(it.quantity), money(it.total), it.notes,
            )
        }
        appendLine()

        section("LABOR")
        row("Project", "Description", "Vendor", "Hours", "Rate", "Flat Cost", "Line Total", "Notes")
        snapshot.labor.forEach {
            row(
                name(it.projectId), it.description, it.vendorName,
                num(it.hours), money(it.rate), money(it.flatCost), money(it.total), it.notes,
            )
        }
        appendLine()

        section("COST SUMMARY")
        row("Project", "Materials Total", "Labor Total", "Grand Total")
        snapshot.projects.forEach { p ->
            val mat = snapshot.equipment.filter { it.projectId == p.id }.sumOf { it.total }
            val lab = snapshot.labor.filter { it.projectId == p.id }.sumOf { it.total }
            row(p.name, money(mat), money(lab), money(mat + lab))
        }
    }

    private fun StringBuilder.section(title: String) = appendLine(escape(title))

    private fun StringBuilder.row(vararg cells: String) =
        appendLine(cells.joinToString(",") { escape(it) })

    private fun escape(value: String): String {
        val needsQuote = value.contains(',') || value.contains('"') ||
            value.contains('\n') || value.contains('\r')
        val body = value.replace("\"", "\"\"")
        return if (needsQuote) "\"$body\"" else body
    }

    private fun money(v: Double): String = String.format("%.2f", v)

    private fun num(v: Double): String =
        if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()
}
