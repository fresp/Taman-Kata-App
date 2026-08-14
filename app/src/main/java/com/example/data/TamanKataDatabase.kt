package com.example.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Stage::class, LearningItem::class, SessionHistory::class, Story::class], version = 4, exportSchema = false)
abstract class TamanKataDatabase : RoomDatabase() {
    abstract fun tamanKataDao(): TamanKataDao
}
