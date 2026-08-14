package com.example

import android.app.Application
import androidx.room.Room
import com.example.data.ConsentPreferences
import com.example.data.TamanKataDatabase
import com.example.data.TamanKataRepository

class TamanKataApp : Application() {
    lateinit var database: TamanKataDatabase
        private set
    lateinit var consentPreferences: ConsentPreferences
        private set
    lateinit var repository: TamanKataRepository
        private set

    override fun onCreate() {
        super.onCreate()
        consentPreferences = ConsentPreferences(this)
        database = Room.databaseBuilder(
            this,
            TamanKataDatabase::class.java,
            "taman_kata_db"
        )
        .fallbackToDestructiveMigration()
        .build()
        repository = TamanKataRepository(database.tamanKataDao(), consentPreferences)
    }
}
