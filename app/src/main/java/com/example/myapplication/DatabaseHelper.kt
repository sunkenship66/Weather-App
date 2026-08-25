package com.example.myapplication

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class RecordModel(
    val id: Int,
    val city: String,
    val temp: String,
    val condition: String
)

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, "WeatherDB", null, 1) {

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL("CREATE TABLE weather_records (id INTEGER PRIMARY KEY AUTOINCREMENT, city TEXT, temp TEXT, condition TEXT)")
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS weather_records")
        onCreate(db)
    }

    // CREATE
    fun insertRecord(city: String, temp: String, condition: String): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("city", city)
            put("temp", temp)
            put("condition", condition)
        }
        val result = db.insert("weather_records", null, values)
        return result != -1L
    }

    // READ
    fun getAllRecords(): ArrayList<RecordModel> {
        val list = ArrayList<RecordModel>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM weather_records ORDER BY id DESC", null)
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                val city = cursor.getString(cursor.getColumnIndexOrThrow("city"))
                val temp = cursor.getString(cursor.getColumnIndexOrThrow("temp"))
                val condition = cursor.getString(cursor.getColumnIndexOrThrow("condition"))
                list.add(RecordModel(id, city, temp, condition))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    // UPDATE
    fun updateRecord(id: Int, newTemp: String, newCondition: String): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("temp", newTemp)
            put("condition", newCondition)
        }
        return db.update("weather_records", values, "id=?", arrayOf(id.toString())) > 0
    }

    // DELETE
    fun deleteRecord(id: Int): Boolean {
        val db = writableDatabase
        return db.delete("weather_records", "id=?", arrayOf(id.toString())) > 0
    }
}