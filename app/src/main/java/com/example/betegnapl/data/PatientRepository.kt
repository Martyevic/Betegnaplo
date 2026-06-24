package com.example.betegnapl.data

import com.example.betegnapl.Patient
import com.example.betegnapl.data.local.PatientDao
import com.example.betegnapl.data.local.PatientEntity
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PatientRepository(private val patientDao: PatientDao) {

    fun observePatients(): Flow<List<Patient>> =
        patientDao.observeAll().map { entities -> entities.map { it.toPatient() } }

    suspend fun insert(patient: Patient) {
        patientDao.upsert(patient.toEntity())
    }

    suspend fun update(patient: Patient) {
        patientDao.upsert(patient.toEntity())
    }

    suspend fun delete(patient: Patient) {
        patientDao.delete(patient.toEntity())
    }

    private fun PatientEntity.toPatient() = Patient(
        id = id,
        name = name,
        room = room,
        illness = illness,
        massageDays = massageDays.toDayOfWeekSet(),
        massageCount = massageCount,
        lastMassageTime = lastMassageTime?.toLocalDateTime(),
    )

    private fun Patient.toEntity() = PatientEntity(
        id = id,
        name = name,
        room = room,
        illness = illness,
        massageDays = massageDays.toStorageString(),
        massageCount = massageCount,
        lastMassageTime = lastMassageTime?.toEpochMilli(),
    )

    private fun String.toDayOfWeekSet(): Set<DayOfWeek> =
        if (isBlank()) emptySet()
        else split(",").map { DayOfWeek.valueOf(it) }.toSet()

    private fun Set<DayOfWeek>.toStorageString(): String =
        joinToString(",") { it.name }

    private fun LocalDateTime.toEpochMilli(): Long =
        atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun Long.toLocalDateTime(): LocalDateTime =
        Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDateTime()
}
