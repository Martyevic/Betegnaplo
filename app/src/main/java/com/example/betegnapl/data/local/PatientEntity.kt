package com.example.betegnapl.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "patients")
data class PatientEntity(
    @PrimaryKey val id: String,
    val name: String,
    val room: String,
    val illness: String,
    val massageDays: String,
    val massageCount: Int,
    val lastMassageTime: Long?,
)
