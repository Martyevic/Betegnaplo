package com.example.betegnapl

import android.app.Application
import androidx.room.Room
import com.example.betegnapl.data.PatientRepository
import com.example.betegnapl.data.local.AppDatabase

class BetegnaplApplication : Application() {

    private val database by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "betegnaplo.db",
        ).build()
    }

    val patientRepository by lazy { PatientRepository(database.patientDao()) }
}
