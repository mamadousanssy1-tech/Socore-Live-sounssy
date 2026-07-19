package com.example.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.data.model.Favorite
import com.example.data.model.NotificationLog

@Database(entities = [Favorite::class, NotificationLog::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun footballDao(): FootballDao
}
