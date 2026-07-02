package com.example.drugsalarm

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.drugsalarm.data.IntakeLog
import com.example.drugsalarm.data.Medicine
import com.example.drugsalarm.data.MedicineViewModel
import com.example.drugsalarm.ui.theme.DrugsAlarmTheme
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DrugsAlarmTheme {
                RequestPermissions()
                MedicineApp()
            }
        }
    }
}

@Composable
fun RequestPermissions() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { _ -> }
        
        LaunchedEffect(Unit) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicineApp(viewModel: MedicineViewModel = viewModel()) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showMedicineDialog by remember { mutableStateOf(false) }
    var medicineToEdit by remember { mutableStateOf<Medicine?>(null) }
    
    var showHistoryDialog by remember { mutableStateOf(false) }
    var logToEdit by remember { mutableStateOf<IntakeLog?>(null) }
    
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showClearMedicinesDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DrugsAlarm") },
                actions = {
                    if (selectedTab == 0) {
                        IconButton(onClick = { showClearMedicinesDialog = true }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear All Medicines", tint = MaterialTheme.colorScheme.error)
                        }
                    } else if (selectedTab == 1) {
                        IconButton(onClick = { showClearHistoryDialog = true }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear All History", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Medication, contentDescription = "Medicines") },
                    label = { Text("Medicines") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.History, contentDescription = "History") },
                    label = { Text("History") }
                )
            }
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(onClick = { 
                    medicineToEdit = null
                    showMedicineDialog = true 
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Medicine")
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            if (selectedTab == 0) {
                MedicineListScreen(
                    viewModel = viewModel,
                    onEdit = { 
                        medicineToEdit = it
                        showMedicineDialog = true
                    }
                )
            } else {
                HistoryScreen(
                    viewModel = viewModel,
                    onEdit = {
                        logToEdit = it
                        showHistoryDialog = true
                    }
                )
            }
        }

        if (showMedicineDialog) {
            MedicineDialog(
                medicine = medicineToEdit,
                onDismiss = { showMedicineDialog = false },
                onConfirm = { name, dosage, freq, time, qty ->
                    if (medicineToEdit == null) {
                        viewModel.addMedicine(name, dosage, freq, time, qty)
                    } else {
                        viewModel.updateMedicine(medicineToEdit!!.copy(
                            name = name,
                            dosage = dosage,
                            frequency = freq,
                            timeInMillis = time,
                            remainingQuantity = qty,
                            totalQuantity = qty
                        ))
                    }
                    showMedicineDialog = false
                }
            )
        }

        if (showHistoryDialog && logToEdit != null) {
            HistoryEditDialog(
                log = logToEdit!!,
                onDismiss = { showHistoryDialog = false },
                onConfirm = { updatedLog ->
                    viewModel.updateLog(updatedLog)
                    showHistoryDialog = false
                }
            )
        }
        
        if (showClearHistoryDialog) {
            AlertDialog(
                onDismissRequest = { showClearHistoryDialog = false },
                title = { Text("Clear All History") },
                text = { Text("Are you sure you want to delete all history logs? This cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.clearAllHistory()
                            showClearHistoryDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Clear All")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearHistoryDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showClearMedicinesDialog) {
            AlertDialog(
                onDismissRequest = { showClearMedicinesDialog = false },
                title = { Text("Clear All Medicines") },
                text = { Text("Are you sure you want to delete all medicines? All reminders will be cancelled. This cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.clearAllMedicines()
                            showClearMedicinesDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Clear All")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearMedicinesDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun MedicineListScreen(viewModel: MedicineViewModel, onEdit: (Medicine) -> Unit) {
    val medicines by viewModel.allMedicines.collectAsState(initial = emptyList())
    
    if (medicines.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No medicines tracked yet.", style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            items(medicines) { medicine ->
                MedicineItem(
                    medicine = medicine,
                    onTake = { viewModel.takeMedicine(medicine) },
                    onDelete = { viewModel.deleteMedicine(medicine) },
                    onEdit = { onEdit(medicine) }
                )
            }
        }
    }
}

@Composable
fun HistoryScreen(viewModel: MedicineViewModel, onEdit: (IntakeLog) -> Unit) {
    val logs by viewModel.allLogs.collectAsState(initial = emptyList())
    
    if (logs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No intake history recorded.", style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(logs) { log ->
                HistoryItem(
                    log = log,
                    onEdit = { onEdit(log) },
                    onDelete = { viewModel.deleteLog(log) }
                )
            }
        }
    }
}

@Composable
fun MedicineItem(medicine: Medicine, onTake: () -> Unit, onDelete: () -> Unit, onEdit: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = medicine.name, style = MaterialTheme.typography.headlineSmall)
                Text(text = "${medicine.dosage} • ${medicine.frequency}", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Next: ${SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(medicine.timeInMillis))}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "Inventory: ${medicine.remainingQuantity}/${medicine.totalQuantity} left",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (medicine.remainingQuantity <= 5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onTake,
                    enabled = medicine.remainingQuantity > 0,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text("Take")
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun HistoryItem(log: IntakeLog, onEdit: () -> Unit, onDelete: () -> Unit) {
    ListItem(
        headlineContent = { Text(log.medicineName) },
        supportingContent = {
            val dateStr = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(log.intakeTime))
            Text("$dateStr — ${log.status}")
        },
        leadingContent = {
            Icon(Icons.Default.Medication, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        trailingContent = {
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    )
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicineDialog(
    medicine: Medicine? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, Long, Int) -> Unit
) {
    var name by remember { mutableStateOf(medicine?.name ?: "") }
    var dosage by remember { mutableStateOf(medicine?.dosage ?: "") }
    var frequency by remember { mutableStateOf(medicine?.frequency ?: "Daily") }
    var quantity by remember { mutableStateOf(medicine?.remainingQuantity?.toString() ?: "30") }
    
    val frequencies = listOf(
        "1 hour","2 hour",
        "Daily", "Weekly", "Every 2 Weeks", "Monthly", "Once"
    )
    var expanded by remember { mutableStateOf(false) }

    val initialTime = if (medicine != null) {
        Calendar.getInstance().apply { timeInMillis = medicine.timeInMillis }
    } else {
        Calendar.getInstance()
    }
    
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedHour by remember { mutableIntStateOf(initialTime.get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableIntStateOf(initialTime.get(Calendar.MINUTE)) }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedHour,
            initialMinute = selectedMinute
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedHour = timePickerState.hour
                    selectedMinute = timePickerState.minute
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
            text = {
                TimePicker(state = timePickerState)
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (medicine == null) "Add Medication" else "Edit Medication") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Medicine Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = dosage, onValueChange = { dosage = it }, label = { Text("Dosage (e.g., 200mg)") }, modifier = Modifier.fillMaxWidth())
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = frequency,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Frequency") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        frequencies.forEach { freq ->
                            DropdownMenuItem(
                                text = { Text(freq) },
                                onClick = {
                                    frequency = freq
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(value = quantity, onValueChange = { if (it.all { char -> char.isDigit() }) quantity = it }, label = { Text("Current Stock (Pills)") }, modifier = Modifier.fillMaxWidth())
                
                Button(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Text("Reminder Time: ${String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute)}")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    val calendar = Calendar.getInstance()
                    calendar.set(Calendar.HOUR_OF_DAY, selectedHour)
                    calendar.set(Calendar.MINUTE, selectedMinute)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    
                    if (calendar.timeInMillis <= System.currentTimeMillis()) {
                        calendar.add(Calendar.DAY_OF_YEAR, 1)
                    }
                    
                    onConfirm(name, dosage, frequency, calendar.timeInMillis, quantity.toIntOrNull() ?: 0) 
                },
                enabled = name.isNotBlank() && (quantity.toIntOrNull() ?: 0) > 0
            ) {
                Text(if (medicine == null) "Add" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryEditDialog(
    log: IntakeLog,
    onDismiss: () -> Unit,
    onConfirm: (IntakeLog) -> Unit
) {
    var status by remember { mutableStateOf(log.status) }
    val statuses = listOf("Taken", "Missed", "Snoozed")
    var expanded by remember { mutableStateOf(false) }

    val calendar = Calendar.getInstance().apply { timeInMillis = log.intakeTime }
    var selectedDate by remember { mutableLongStateOf(log.intakeTime) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    var selectedHour by remember { mutableIntStateOf(calendar.get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableIntStateOf(calendar.get(Calendar.MINUTE)) }
    var showTimePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedDate = datePickerState.selectedDateMillis ?: selectedDate
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(initialHour = selectedHour, initialMinute = selectedMinute)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedHour = timePickerState.hour
                    selectedMinute = timePickerState.minute
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
            text = { TimePicker(state = timePickerState) }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit History Entry") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Medicine: ${log.medicineName}", style = MaterialTheme.typography.bodyLarge)
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = status,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Status") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        statuses.forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s) },
                                onClick = {
                                    status = s
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Button(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    Text("Date: ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(selectedDate))}")
                }

                Button(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    Text("Time: ${String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute)}")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val finalCalendar = Calendar.getInstance().apply {
                    timeInMillis = selectedDate
                    set(Calendar.HOUR_OF_DAY, selectedHour)
                    set(Calendar.MINUTE, selectedMinute)
                }
                onConfirm(log.copy(status = status, intakeTime = finalCalendar.timeInMillis))
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
