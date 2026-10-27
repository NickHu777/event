package com.hwt.event.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

data class EventItem(
    val id: String,
    val name: String,
    val createdAt: Long,
    val isPinned: Boolean,
    val totalCount: Int,
    val lastOccurredAt: Long?
)

data class Occurrence(
    val id: String,
    val eventId: String,
    val occurredAt: Long,
    val createdAt: Long,
    val localDate: String,
    val zoneId: String,
    val note: String
)

/** Local-only event and occurrence storage. Event history never leaves this device. */
class EventStore(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE events (
                id TEXT PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                is_pinned INTEGER NOT NULL DEFAULT 0
            )""".trimIndent()
        )
        db.execSQL(
            """CREATE TABLE occurrences (
                id TEXT PRIMARY KEY NOT NULL,
                event_id TEXT NOT NULL,
                occurred_at INTEGER NOT NULL,
                created_at INTEGER NOT NULL,
                local_date TEXT NOT NULL,
                zone_id TEXT NOT NULL,
                note TEXT NOT NULL DEFAULT '',
                FOREIGN KEY(event_id) REFERENCES events(id) ON DELETE CASCADE
            )""".trimIndent()
        )
        db.execSQL("CREATE INDEX index_occurrences_event_date ON occurrences(event_id, local_date)")
        db.execSQL("CREATE INDEX index_occurrences_event_time ON occurrences(event_id, occurred_at DESC)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Database migrations will be added before a released version changes the schema.
    }

    fun getEvents(): List<EventItem> {
        val result = mutableListOf<EventItem>()
        readableDatabase.rawQuery(
            """SELECT e.id, e.name, e.created_at, e.is_pinned,
                      COUNT(o.id) AS total_count, MAX(o.occurred_at) AS last_occurred_at
               FROM events e
               LEFT JOIN occurrences o ON o.event_id = e.id
               GROUP BY e.id""".trimIndent(),
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result += EventItem(
                    id = cursor.getString(0),
                    name = cursor.getString(1),
                    createdAt = cursor.getLong(2),
                    isPinned = cursor.getInt(3) != 0,
                    totalCount = cursor.getInt(4),
                    lastOccurredAt = if (cursor.isNull(5)) null else cursor.getLong(5)
                )
            }
        }
        return result
    }

    fun getEvent(eventId: String): EventItem? = getEvents().firstOrNull { it.id == eventId }

    fun createEvent(name: String): String {
        val cleanName = name.trim()
        require(cleanName.isNotEmpty()) { "事件名称不能为空" }
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        writableDatabase.insertOrThrow(
            "events",
            null,
            ContentValues().apply {
                put("id", id)
                put("name", cleanName)
                put("created_at", now)
                put("is_pinned", 0)
            }
        )
        return id
    }

    fun renameEvent(eventId: String, name: String) {
        val cleanName = name.trim()
        require(cleanName.isNotEmpty()) { "事件名称不能为空" }
        writableDatabase.update(
            "events",
            ContentValues().apply { put("name", cleanName) },
            "id = ?",
            arrayOf(eventId)
        )
    }

    fun setPinned(eventId: String, pinned: Boolean) {
        writableDatabase.update(
            "events",
            ContentValues().apply { put("is_pinned", if (pinned) 1 else 0) },
            "id = ?",
            arrayOf(eventId)
        )
    }

    fun deleteEvent(eventId: String) {
        writableDatabase.delete("events", "id = ?", arrayOf(eventId))
    }

    /** Creates exactly one occurrence for one tap. */
    fun recordOne(eventId: String, occurredAt: Long = System.currentTimeMillis()): String {
        val id = UUID.randomUUID().toString()
        val zone = ZoneId.systemDefault()
        val localDate = Instant.ofEpochMilli(occurredAt).atZone(zone).toLocalDate().toString()
        val now = System.currentTimeMillis()
        writableDatabase.insertOrThrow(
            "occurrences",
            null,
            ContentValues().apply {
                put("id", id)
                put("event_id", eventId)
                put("occurred_at", occurredAt)
                put("created_at", now)
                put("local_date", localDate)
                put("zone_id", zone.id)
                put("note", "")
            }
        )
        return id
    }

    fun getOccurrences(eventId: String): List<Occurrence> {
        val result = mutableListOf<Occurrence>()
        readableDatabase.query(
            "occurrences",
            arrayOf("id", "event_id", "occurred_at", "created_at", "local_date", "zone_id", "note"),
            "event_id = ?",
            arrayOf(eventId),
            null,
            null,
            "occurred_at DESC, created_at DESC"
        ).use { cursor ->
            while (cursor.moveToNext()) result += cursor.toOccurrence()
        }
        return result
    }

    fun updateOccurrence(occurrenceId: String, occurredAt: Long) {
        val zone = ZoneId.systemDefault()
        val localDate = Instant.ofEpochMilli(occurredAt).atZone(zone).toLocalDate().toString()
        writableDatabase.update(
            "occurrences",
            ContentValues().apply {
                put("occurred_at", occurredAt)
                put("local_date", localDate)
                put("zone_id", zone.id)
            },
            "id = ?",
            arrayOf(occurrenceId)
        )
    }

    fun deleteOccurrence(occurrenceId: String) {
        writableDatabase.delete("occurrences", "id = ?", arrayOf(occurrenceId))
    }

    fun exportBackup(): String {
        val root = JSONObject()
        root.put("format", "event-backup")
        root.put("formatVersion", BACKUP_VERSION)
        root.put("exportedAt", System.currentTimeMillis())

        val eventArray = JSONArray()
        readableDatabase.query("events", null, null, null, null, null, "created_at ASC").use { cursor ->
            while (cursor.moveToNext()) {
                eventArray.put(
                    JSONObject()
                        .put("id", cursor.getString(cursor.getColumnIndexOrThrow("id")))
                        .put("name", cursor.getString(cursor.getColumnIndexOrThrow("name")))
                        .put("createdAt", cursor.getLong(cursor.getColumnIndexOrThrow("created_at")))
                        .put("pinned", cursor.getInt(cursor.getColumnIndexOrThrow("is_pinned")) != 0)
                )
            }
        }
        val occurrenceArray = JSONArray()
        readableDatabase.query("occurrences", null, null, null, null, null, "occurred_at ASC").use { cursor ->
            while (cursor.moveToNext()) {
                occurrenceArray.put(
                    JSONObject()
                        .put("id", cursor.getString(cursor.getColumnIndexOrThrow("id")))
                        .put("eventId", cursor.getString(cursor.getColumnIndexOrThrow("event_id")))
                        .put("occurredAt", cursor.getLong(cursor.getColumnIndexOrThrow("occurred_at")))
                        .put("createdAt", cursor.getLong(cursor.getColumnIndexOrThrow("created_at")))
                        .put("localDate", cursor.getString(cursor.getColumnIndexOrThrow("local_date")))
                        .put("zoneId", cursor.getString(cursor.getColumnIndexOrThrow("zone_id")))
                        .put("note", cursor.getString(cursor.getColumnIndexOrThrow("note")))
                )
            }
        }
        root.put("events", eventArray)
        root.put("occurrences", occurrenceArray)
        return root.toString(2)
    }

    /** Merges a backup by stable IDs so importing the same file twice does not duplicate records. */
    fun importBackup(json: String): Pair<Int, Int> {
        val root = JSONObject(json)
        require(root.optString("format") == "event-backup") { "这不是 event 的备份文件" }
        require(root.optInt("formatVersion", 0) in 1..BACKUP_VERSION) { "备份格式版本不受支持" }
        val eventArray = root.optJSONArray("events") ?: JSONArray()
        val occurrenceArray = root.optJSONArray("occurrences") ?: JSONArray()

        val eventsToImport = (0 until eventArray.length()).map { index ->
            val item = eventArray.getJSONObject(index)
            val id = item.getString("id")
            val name = item.getString("name").trim()
            require(id.isNotBlank() && name.isNotBlank()) { "备份中包含无效事件" }
            ImportedEvent(id, name, item.optLong("createdAt", System.currentTimeMillis()), item.optBoolean("pinned"))
        }
        val importedIds = eventsToImport.map { it.id }.toSet()
        val occurrencesToImport = (0 until occurrenceArray.length()).map { index ->
            val item = occurrenceArray.getJSONObject(index)
            val occurredAt = item.getLong("occurredAt")
            val zoneId = item.optString("zoneId").takeIf { it.isNotBlank() } ?: ZoneId.systemDefault().id
            val localDate = item.optString("localDate").takeIf { it.isNotBlank() }
                ?: Instant.ofEpochMilli(occurredAt).atZone(ZoneId.of(zoneId)).toLocalDate().toString()
            ImportedOccurrence(
                id = item.getString("id"),
                eventId = item.getString("eventId"),
                occurredAt = occurredAt,
                createdAt = item.optLong("createdAt", occurredAt),
                localDate = localDate,
                zoneId = zoneId,
                note = item.optString("note")
            )
        }
        require(occurrencesToImport.all { it.eventId in importedIds }) { "备份中的记录缺少对应事件" }

        val db = writableDatabase
        db.beginTransaction()
        try {
            eventsToImport.forEach { event ->
                val values = ContentValues().apply {
                    put("id", event.id)
                    put("name", event.name)
                    put("created_at", event.createdAt)
                    put("is_pinned", if (event.pinned) 1 else 0)
                }
                val updated = db.update("events", values, "id = ?", arrayOf(event.id))
                if (updated == 0) db.insertOrThrow("events", null, values)
            }
            occurrencesToImport.forEach { occurrence ->
                val values = ContentValues().apply {
                    put("id", occurrence.id)
                    put("event_id", occurrence.eventId)
                    put("occurred_at", occurrence.occurredAt)
                    put("created_at", occurrence.createdAt)
                    put("local_date", occurrence.localDate)
                    put("zone_id", occurrence.zoneId)
                    put("note", occurrence.note)
                }
                val updated = db.update("occurrences", values, "id = ?", arrayOf(occurrence.id))
                if (updated == 0) db.insertOrThrow("occurrences", null, values)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return eventsToImport.size to occurrencesToImport.size
    }

    private fun android.database.Cursor.toOccurrence() = Occurrence(
        id = getString(0),
        eventId = getString(1),
        occurredAt = getLong(2),
        createdAt = getLong(3),
        localDate = getString(4),
        zoneId = getString(5),
        note = getString(6)
    )

    private data class ImportedEvent(val id: String, val name: String, val createdAt: Long, val pinned: Boolean)
    private data class ImportedOccurrence(
        val id: String,
        val eventId: String,
        val occurredAt: Long,
        val createdAt: Long,
        val localDate: String,
        val zoneId: String,
        val note: String
    )

    private companion object {
        const val DATABASE_NAME = "event.db"
        const val DATABASE_VERSION = 1
        const val BACKUP_VERSION = 1
    }
}
