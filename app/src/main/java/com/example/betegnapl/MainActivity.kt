package com.example.betegnapl

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale
import java.util.UUID

// --- ADATMODELL ---
data class Patient(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val room: String,
    val illness: String,
    val massageDays: Set<DayOfWeek>,
    val massageCount: Int = 0,
    val lastMassageTime: LocalDateTime? = null
)

// --- VIEWMODEL ---
class PatientViewModel : ViewModel() {
    private val _patients = mutableStateListOf<Patient>()
    val patients: List<Patient> get() = _patients

    fun addPatient(name: String, room: String, illness: String, days: Set<DayOfWeek>, initialCount: Int) {
        _patients.add(Patient(name = name, room = room, illness = illness, massageDays = days, massageCount = initialCount))
    }

    fun incrementMassage(patient: Patient) {
        val index = _patients.indexOfFirst { it.id == patient.id }
        if (index != -1) {
            _patients[index] = _patients[index].copy(
                massageCount = _patients[index].massageCount + 1,
                lastMassageTime = LocalDateTime.now()
            )
        }
    }

    fun deletePatient(patient: Patient) {
        _patients.removeIf { it.id == patient.id }
    }

    fun getLogicalToday(): DayOfWeek {
        val now = LocalDateTime.now()
        val logicalTime = if (now.toLocalTime().isBefore(LocalTime.of(6, 0))) now.minusDays(1) else now
        return logicalTime.dayOfWeek
    }

    fun isMassagedToday(patient: Patient): Boolean {
        if (patient.lastMassageTime == null) return false
        val now = LocalDateTime.now()
        val logicalStartOfToday = if (now.toLocalTime().isBefore(LocalTime.of(6, 0))) now.minusDays(1).with(LocalTime.of(6, 0)) else now.with(LocalTime.of(6, 0))
        return patient.lastMassageTime!!.isAfter(logicalStartOfToday)
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
                    AppNavigation()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(viewModel: PatientViewModel = viewModel()) {
    var showAllPatients by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    val todayName = LocalDate.now().dayOfWeek.getDisplayName(TextStyle.FULL, Locale("hu", "HU"))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (showAllPatients) "Összes beteg" else "Napi Masszázsok") },
                actions = {
                    TextButton(onClick = { showAllPatients = !showAllPatients }) {
                        Text(if (showAllPatients) "Vissza" else "Összes beteg")
                    }
                }
            )
        },
        bottomBar = {
            Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                Text(text = "Ma van: ${todayName.replaceFirstChar { it.uppercase() }}", fontSize = 14.sp, color = Color.DarkGray)
            }
        },
        floatingActionButton = {
            if (!showAllPatients) {
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Új beteg")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (showAllPatients) AllPatientsScreen(viewModel) else TodayPatientsScreen(viewModel)
        }
        if (showAddDialog) {
            AddPatientDialog(
                onDismiss = { showAddDialog = false },
                onSave = { n, r, i, d, c -> viewModel.addPatient(n, r, i, d, c); showAddDialog = false }
            )
        }
    }
}

@Composable
fun TodayPatientsScreen(viewModel: PatientViewModel) {
    val today = viewModel.getLogicalToday()
    val todayPatients = viewModel.patients.filter { it.massageDays.contains(today) }
    LazyColumn(modifier = Modifier.padding(16.dp)) {
        items(todayPatients.sortedBy { viewModel.isMassagedToday(it) }) { patient ->
            PatientItem(patient, viewModel.isMassagedToday(patient), { viewModel.incrementMassage(patient) }, { viewModel.deletePatient(patient) })
        }
    }
}

@Composable
fun AllPatientsScreen(viewModel: PatientViewModel) {
    LazyColumn(modifier = Modifier.padding(16.dp)) {
        items(viewModel.patients) { patient ->
            PatientItem(patient, false, {}, { viewModel.deletePatient(patient) }, true)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PatientItem(patient: Patient, isDone: Boolean, onClick: () -> Unit, onDelete: () -> Unit, isAll: Boolean = false) {
    var showDialog by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth().padding(8.dp).combinedClickable(onClick = { if (!isAll) onClick() else showDialog = true }, onLongClick = { showDialog = true })) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(text = "${patient.name} (${patient.room})", fontWeight = FontWeight.Bold, color = if (isDone) Color(0xFF4CAF50) else Color.Black)
            Text(text = "Eddig: ${patient.massageCount}", fontSize = 14.sp, color = Color.Gray)
        }
    }
    if (showDialog) PatientDetailsDialog(patient, { showDialog = false }, onDelete)
}

@Composable
fun PatientDetailsDialog(patient: Patient, onDismiss: () -> Unit, onDelete: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(patient.name) }, text = {
        Column {
            Text("Kórterem: ${patient.room}")
            Text("Betegség: ${patient.illness}")
            Text("Napok: ${patient.massageDays.joinToString { translateDay(it) }}")
            Text("Összes masszírozás: ${patient.massageCount}", fontWeight = FontWeight.Bold)
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("Bezárás") } }, dismissButton = { TextButton(onClick = onDelete) { Text("Törlés", color = Color.Red) } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPatientDialog(onDismiss: () -> Unit, onSave: (String, String, String, Set<DayOfWeek>, Int) -> Unit) {
    var name by remember { mutableStateOf("") }
    var room by remember { mutableStateOf("") }
    var illness by remember { mutableStateOf("") }
    var count by remember { mutableStateOf("0") }
    var selectedDays by remember { mutableStateOf(setOf<DayOfWeek>()) }
    val daysMap = mapOf(DayOfWeek.MONDAY to "H", DayOfWeek.TUESDAY to "K", DayOfWeek.WEDNESDAY to "Sze", DayOfWeek.THURSDAY to "Cs", DayOfWeek.FRIDAY to "P")

    AlertDialog(onDismissRequest = onDismiss, title = { Text("Új beteg") }, text = {
        Column {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Név") })
            OutlinedTextField(value = room, onValueChange = { room = it }, label = { Text("Kórterem") })
            OutlinedTextField(value = illness, onValueChange = { illness = it }, label = { Text("Betegség") })
            OutlinedTextField(value = count, onValueChange = { count = it }, label = { Text("Eddigi masszírozások") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            Row(modifier = Modifier.padding(top = 8.dp)) {
                daysMap.forEach { (day, label) ->
                    FilterChip(selected = selectedDays.contains(day), onClick = { selectedDays = if (selectedDays.contains(day)) selectedDays - day else selectedDays + day }, label = { Text(label) })
                }
            }
        }
    }, confirmButton = { Button(onClick = { onSave(name, room, illness, selectedDays, count.toIntOrNull() ?: 0) }) { Text("Mentés") } })
}

fun translateDay(day: DayOfWeek): String = when (day) {
    DayOfWeek.MONDAY -> "Hétfő"; DayOfWeek.TUESDAY -> "Kedd"; DayOfWeek.WEDNESDAY -> "Szerda"; DayOfWeek.THURSDAY -> "Csütörtök"; DayOfWeek.FRIDAY -> "Péntek"; else -> ""
}