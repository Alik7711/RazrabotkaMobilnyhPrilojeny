package ru.example.lab09

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class Task(val id: Int, val title: String, val done: Boolean, val priority: Int)
data class PriorityCount(val priority: Int, val count: Int)

class TasksDb(context: Context) : SQLiteOpenHelper(context, "tasks.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE tasks (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                done INTEGER NOT NULL DEFAULT 0,
                priority INTEGER NOT NULL DEFAULT 1
            )
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Миграция для ЛР 10
    }

    fun add(title: String, priority: Int) {
        val values = ContentValues().apply {
            put("title", title)
            put("priority", priority)
            put("done", 0)
        }
        writableDatabase.insert("tasks", null, values)
    }

    fun updateDone(id: Int, done: Boolean) {
        val values = ContentValues().apply {
            put("done", if (done) 1 else 0)
        }
        writableDatabase.update("tasks", values, "id = ?", arrayOf(id.toString()))
    }

    fun delete(id: Int) {
        writableDatabase.delete("tasks", "id = ?", arrayOf(id.toString()))
    }

    fun read(doneFilter: Boolean?, priorityFilter: Int?): List<Task> {
        var query = "SELECT id, title, done, priority FROM tasks WHERE 1=1"
        val args = mutableListOf<String>()

        if (doneFilter != null) {
            query += " AND done = ?"
            args.add(if (doneFilter) "1" else "0")
        }
        if (priorityFilter != null) {
            query += " AND priority = ?"
            args.add(priorityFilter.toString())
        }

        query += " ORDER BY title COLLATE NOCASE ASC"

        val list = mutableListOf<Task>()
        readableDatabase.rawQuery(query, args.toTypedArray()).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(
                    Task(
                        id = cursor.getInt(0),
                        title = cursor.getString(1),
                        done = cursor.getInt(2) == 1,
                        priority = cursor.getInt(3)
                    )
                )
            }
        }
        return list
    }

    fun countsByPriority(): List<PriorityCount> {
        val list = mutableListOf<PriorityCount>()
        readableDatabase.rawQuery("SELECT priority, COUNT(*) FROM tasks GROUP BY priority", null).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(PriorityCount(cursor.getInt(0), cursor.getInt(1)))
            }
        }
        return list
    }
}