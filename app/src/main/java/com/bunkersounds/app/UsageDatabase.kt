package com.bunkersounds.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class UsageDatabase(context: Context) : SQLiteOpenHelper(context, "bunker_usage.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) { db.execSQL("CREATE TABLE sessions (id INTEGER PRIMARY KEY AUTOINCREMENT, mode TEXT NOT NULL, started_at INTEGER NOT NULL, ended_at INTEGER)") }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
    fun start(mode: String): Long = writableDatabase.insert("sessions", null, ContentValues().apply { put("mode", mode); put("started_at", System.currentTimeMillis()) })
    fun stop(id: Long) { writableDatabase.update("sessions", ContentValues().apply { put("ended_at", System.currentTimeMillis()) }, "id=?", arrayOf(id.toString())) }
    fun summary(): UsageSummary { val c = readableDatabase.rawQuery("SELECT COUNT(*), COALESCE(SUM(CASE WHEN ended_at IS NULL THEN 0 ELSE ended_at-started_at END),0), (SELECT mode FROM sessions GROUP BY mode ORDER BY COUNT(*) DESC LIMIT 1) FROM sessions", null); c.use { if (it.moveToFirst()) return UsageSummary(it.getInt(0), it.getLong(1), it.getString(2) ?: "None") }; return UsageSummary(0, 0, "None") }
}

data class UsageSummary(val sessions: Int, val durationMs: Long, val favorite: String)
