package com.example.drugsalarm

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.drugsalarm.data.*
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
        LaunchedEffect(Unit) { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }
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
                title = { Text("DrugsAlarm", fontWeight = FontWeight.Bold) },
                actions = {
                    if (selectedTab == 0) {
                        IconButton(onClick = { showClearMedicinesDialog = true }) {
                            Icon(Icons.Default.DeleteForever, "Clear All Medicines", tint = MaterialTheme.colorScheme.error)
                        }
                    } else if (selectedTab == 1) {
                        IconButton(onClick = { showClearHistoryDialog = true }) {
                            Icon(Icons.Default.DeleteForever, "Clear All History", tint = MaterialTheme.colorScheme.error)
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
                    icon = { Icon(Icons.Default.Medication, "Medicines") },
                    label = { Text("Thuốc") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.History, "History") },
                    label = { Text("Lịch sử") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.BarChart, "Stats") },
                    label = { Text("Thống kê") }
                )
            }
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(onClick = { 
                    medicineToEdit = null
                    showMedicineDialog = true 
                }) {
                    Icon(Icons.Default.Add, "Add Medicine")
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when (selectedTab) {
                0 -> MedicineListScreen(viewModel) { medicine ->
                    medicineToEdit = medicine
                    showMedicineDialog = true
                }
                1 -> HistoryScreen(viewModel) { log ->
                    logToEdit = log
                    showHistoryDialog = true
                }
                2 -> StatsScreen(viewModel)
            }
        }

        if (showMedicineDialog) {
            MedicineDialog(medicineToEdit, { showMedicineDialog = false }) { n, d, f, t, q ->
                if (medicineToEdit == null) viewModel.addMedicine(n, d, f, t, q)
                else viewModel.updateMedicine(medicineToEdit!!.copy(name = n, dosage = d, frequency = f, timeInMillis = t, remainingQuantity = q, totalQuantity = q))
                showMedicineDialog = false
            }
        }

        if (showHistoryDialog && logToEdit != null) {
            HistoryEditDialog(logToEdit!!, { showHistoryDialog = false }) { updatedLog ->
                viewModel.updateLog(updatedLog)
                showHistoryDialog = false
            }
        }
        
        if (showClearHistoryDialog) {
            ConfirmDeleteDialog("Xóa toàn bộ lịch sử?", "Hành động này không thể hoàn tác.", { showClearHistoryDialog = false }) {
                viewModel.clearAllHistory()
                showClearHistoryDialog = false
            }
        }

        if (showClearMedicinesDialog) {
            ConfirmDeleteDialog("Xóa toàn bộ thuốc?", "Tất cả lời nhắc sẽ bị hủy bỏ.", { showClearMedicinesDialog = false }) {
                viewModel.clearAllMedicines()
                showClearMedicinesDialog = false
            }
        }
    }
}

@Composable
fun StatsScreen(viewModel: MedicineViewModel) {
    val stats by viewModel.weeklyStats.collectAsState(initial = emptyList())
    val allLogs by viewModel.allLogs.collectAsState(initial = emptyList())
    
    var selectedDetailDate by remember { mutableStateOf<Long?>(null) }
    var selectedDetailStatus by remember { mutableStateOf<String?>(null) }
    var showDetails by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Tuân thủ 7 ngày qua", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        if (stats.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Chưa có dữ liệu thống kê.", color = MaterialTheme.colorScheme.outline)
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth().height(320.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    WeeklyBarChart(stats) { date, status ->
                        selectedDetailDate = date
                        selectedDetailStatus = status
                        showDetails = true
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                LegendItem("Đã uống", Color(0xFF4CAF50))
                LegendItem("Bỏ lỡ", Color(0xFFF44336))
            }
        }
    }

    if (showDetails && selectedDetailDate != null && selectedDetailStatus != null) {
        StatsDetailsDialog(
            date = selectedDetailDate!!,
            status = selectedDetailStatus!!,
            logs = allLogs,
            onDismiss = { showDetails = false }
        )
    }
}

@Composable
fun WeeklyBarChart(stats: List<DailyStats>, onBarClick: (Long, String) -> Unit) {
    val maxCount = (stats.maxOfOrNull { it.takenCount + it.missedCount } ?: 1).coerceAtLeast(5)
    val dateFormat = SimpleDateFormat("EE", Locale.getDefault())
    val gridColor = MaterialTheme.colorScheme.outlineVariant.toArgb()

    Canvas(modifier = Modifier
        .fillMaxSize()
        .pointerInput(stats) {
            detectTapGestures { offset ->
                val canvasWidth = size.width
                val canvasHeight = size.height
                val bottomArea = 40.dp.toPx()
                val leftArea = 30.dp.toPx()
                val chartHeight = canvasHeight - bottomArea
                val chartWidth = canvasWidth - leftArea
                val spaceBetweenDays = chartWidth / stats.size
                val barWidth = spaceBetweenDays * 0.3f

                stats.forEachIndexed { index, dailyStats ->
                    val xCenter = leftArea + (index * spaceBetweenDays) + (spaceBetweenDays / 2)
                    
                    // Check "Taken" bar bounds
                    val takenHeight = (dailyStats.takenCount.toFloat() / maxCount) * (chartHeight * 0.9f)
                    if (offset.x in (xCenter - barWidth)..xCenter && 
                        offset.y in (chartHeight - takenHeight)..chartHeight) {
                        onBarClick(dailyStats.date, "Taken")
                    }
                    
                    // Check "Missed" bar bounds
                    val missedHeight = (dailyStats.missedCount.toFloat() / maxCount) * (chartHeight * 0.9f)
                    if (offset.x in xCenter..(xCenter + barWidth) && 
                        offset.y in (chartHeight - missedHeight)..chartHeight) {
                        onBarClick(dailyStats.date, "Missed")
                    }
                }
            }
        }
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val bottomArea = 40.dp.toPx()
        val leftArea = 30.dp.toPx()

        val chartHeight = canvasHeight - bottomArea
        val chartWidth = canvasWidth - leftArea
        
        // Draw Grid Lines (Y-Axis)
        val steps = 5
        for (i in 0..steps) {
            val y = chartHeight - (i.toFloat() / steps) * (chartHeight * 0.9f)
            drawLine(
                color = Color(gridColor),
                start = Offset(leftArea, y),
                end = Offset(canvasWidth, y),
                strokeWidth = 1.dp.toPx()
            )
            
            drawContext.canvas.nativeCanvas.drawText(
                (maxCount * i / steps).toString(),
                leftArea - 10.dp.toPx(),
                y + 5.dp.toPx(),
                android.graphics.Paint().apply {
                    textSize = 10.sp.toPx()
                    textAlign = android.graphics.Paint.Align.RIGHT
                    color = gridColor
                }
            )
        }

        val spaceBetweenDays = chartWidth / stats.size
        val barWidth = spaceBetweenDays * 0.3f
        
        stats.forEachIndexed { index, dailyStats ->
            val xCenter = leftArea + (index * spaceBetweenDays) + (spaceBetweenDays / 2)
            
            // Draw Taken Bar
            val takenHeight = (dailyStats.takenCount.toFloat() / maxCount) * (chartHeight * 0.9f)
            drawRect(
                color = Color(0xFF4CAF50),
                topLeft = Offset(xCenter - barWidth, chartHeight - takenHeight),
                size = Size(barWidth, takenHeight)
            )
            
            // Draw Missed Bar
            val missedHeight = (dailyStats.missedCount.toFloat() / maxCount) * (chartHeight * 0.9f)
            drawRect(
                color = Color(0xFFF44336),
                topLeft = Offset(xCenter, chartHeight - missedHeight),
                size = Size(barWidth, missedHeight)
            )
            
            // Label Day
            drawContext.canvas.nativeCanvas.drawText(
                dateFormat.format(Date(dailyStats.date)),
                xCenter,
                canvasHeight - 5.dp.toPx(),
                android.graphics.Paint().apply {
                    textSize = 11.sp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                    color = gridColor
                    isFakeBoldText = true
                }
            )
        }
    }
}

@Composable
fun StatsDetailsDialog(date: Long, status: String, logs: List<IntakeLog>, onDismiss: () -> Unit) {
    val filteredLogs = logs.filter { log ->
        val cal = Calendar.getInstance().apply { timeInMillis = log.intakeTime }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.timeInMillis == date && log.status == status
    }.sortedByDescending { it.intakeTime }

    val dateTitle = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(date))
    val statusLabel = if (status == "Taken") "Đã uống" else "Bỏ lỡ"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$statusLabel - $dateTitle", fontWeight = FontWeight.Bold) },
        text = {
            if (filteredLogs.isEmpty()) {
                Text("Không có dữ liệu cho mục này.")
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                    items(filteredLogs) { log ->
                        val fullTimeStr = SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault()).format(Date(log.intakeTime))
                        ListItem(
                            headlineContent = { Text(log.medicineName, fontWeight = FontWeight.Medium) },
                            supportingContent = { Text("Lúc: $fullTimeStr") },
                            leadingContent = { 
                                Icon(
                                    imageVector = if (status == "Taken") Icons.Default.CheckCircle else Icons.Default.Error,
                                    contentDescription = null,
                                    tint = if (status == "Taken") Color(0xFF4CAF50) else Color(0xFFF44336)
                                )
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Đóng") }
        }
    )
}

@Composable
fun MedicineListScreen(viewModel: MedicineViewModel, onEdit: (Medicine) -> Unit) {
    val medicines by viewModel.allMedicines.collectAsState(initial = emptyList())
    
    if (medicines.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Chưa có thuốc nào.", style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            items(medicines) { medicine ->
                MedicineItem(
                    medicine = medicine,
                    onTake = { viewModel.takeMedicine(medicine) },
                    onDelete = { viewModel.deleteMedicine(medicine) },
                    onEdit = { onEdit(medicine) },
                    onRefill = { 
                        viewModel.updateMedicine(medicine.copy(remainingQuantity = medicine.totalQuantity))
                    }
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
            Text("Lịch sử trống.", style = MaterialTheme.typography.bodyLarge)
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
fun MedicineItem(
    medicine: Medicine, 
    onTake: () -> Unit, 
    onDelete: () -> Unit, 
    onEdit: () -> Unit,
    onRefill: () -> Unit
) {
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
                    text = "Lần tới: ${SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(medicine.timeInMillis))}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Kho: ${medicine.remainingQuantity}/${medicine.totalQuantity}",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (medicine.remainingQuantity <= 5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                    if (medicine.remainingQuantity <= 5) {
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = onRefill, contentPadding = PaddingValues(0.dp)) {
                            Text("NẠP LẠI", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onTake,
                    enabled = medicine.remainingQuantity > 0,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text("Uống")
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
            Text("$dateStr — ${if(log.status == "Taken") "Đã uống" else "Bỏ lỡ"}")
        },
        leadingContent = {
            Icon(
                Icons.Default.Medication, 
                contentDescription = null, 
                tint = if (log.status == "Taken") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
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
    
    val frequencies = Frequency.getAllLabels()
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
                TextButton(onClick = { showTimePicker = false }) { Text("Hủy") }
            },
            text = { TimePicker(state = timePickerState) }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (medicine == null) "Thêm thuốc mới" else "Chỉnh sửa thuốc") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Tên thuốc") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = dosage, onValueChange = { dosage = it }, label = { Text("Liều lượng") }, modifier = Modifier.fillMaxWidth())
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = frequency,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tần suất") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        frequencies.forEach { freq ->
                            DropdownMenuItem(text = { Text(freq) }, onClick = { frequency = freq; expanded = false })
                        }
                    }
                }

                OutlinedTextField(value = quantity, onValueChange = { if (it.all { char -> char.isDigit() }) quantity = it }, label = { Text("Số lượng trong kho") }, modifier = Modifier.fillMaxWidth())
                
                Button(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                ) {
                    Text("Giờ nhắc: ${String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute)}")
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
                    if (calendar.timeInMillis <= System.currentTimeMillis()) calendar.add(Calendar.DAY_OF_YEAR, 1)
                    onConfirm(name, dosage, frequency, calendar.timeInMillis, quantity.toIntOrNull() ?: 0) 
                },
                enabled = name.isNotBlank()
            ) {
                Text(if (medicine == null) "Thêm" else "Lưu")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryEditDialog(log: IntakeLog, onDismiss: () -> Unit, onConfirm: (IntakeLog) -> Unit) {
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
                TextButton(onClick = { selectedDate = datePickerState.selectedDateMillis ?: selectedDate; showDatePicker = false }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Hủy") } }
        ) { DatePicker(state = datePickerState) }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(initialHour = selectedHour, initialMinute = selectedMinute)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = { selectedHour = timePickerState.hour; selectedMinute = timePickerState.minute; showTimePicker = false }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Hủy") } },
            text = { TimePicker(state = timePickerState) }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chỉnh sửa lịch sử") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Thuốc: ${log.medicineName}", style = MaterialTheme.typography.bodyLarge)
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = status,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Trạng thái") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        statuses.forEach { s ->
                            DropdownMenuItem(text = { Text(s) }, onClick = { status = s; expanded = false })
                        }
                    }
                }
                Button(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)) {
                    Text("Ngày: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(selectedDate))}")
                }
                Button(onClick = { showTimePicker = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)) {
                    Text("Giờ: ${String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute)}")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val finalCalendar = Calendar.getInstance().apply { timeInMillis = selectedDate; set(Calendar.HOUR_OF_DAY, selectedHour); set(Calendar.MINUTE, selectedMinute) }
                onConfirm(log.copy(status = status, intakeTime = finalCalendar.timeInMillis))
            }) { Text("Lưu") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(14.dp).background(color, RoundedCornerShape(3.dp)))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun ConfirmDeleteDialog(title: String, text: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Xóa tất cả") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}
