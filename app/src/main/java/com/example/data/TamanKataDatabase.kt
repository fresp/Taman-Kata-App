package com.example.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Stage::class, LearningItem::class, SessionHistory::class], version = 3, exportSchema = false)
abstract class TamanKataDatabase : RoomDatabase() {
    abstract fun tamanKataDao(): TamanKataDao
}
