package com.example.betegnapl.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [PatientEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun patientDao(): PatientDao
}
