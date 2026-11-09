package com.example.admin

import android.content.Context
import android.content.Intent
import android.content.ClipData
import android.content.ClipboardManager
import android.net.Uri
import android.widget.Toast
import android.os.Bundle
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.pm.PackageManager
import android.provider.Settings
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.input.ImeAction
import androidx.core.content.FileProvider
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.BitmapFactory
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.UUID

private val Blue = Color(0xFF65BCEB)
private val Ink = Color(0xFFE8EDF3)
private val Muted = Color(0xFFA0AAB6)
private val BgDark = Color(0xFF101419)
private val CardDark = Color(0xFF1B232C)
private val SurfaceDark = Color(0xFF222D38)
private val DateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val Names = listOf("ISUR", "CAJA", "BCP", "KAELA")

data class Installment(
    val id: String,
    val number: Int,
    val date: String,
    val amount: Long,
    val paid: Boolean = false,
    val receipt: String? = null
)
data class ReceiptTarget(val debtName: String, val installmentId: String, val path: String)

data class Debt(
    val name: String,
    val count: Int = 0,
    val amount: Long = 0,
    val startDate: String = "",
    val installments: List<Installment> = emptyList()
) {
    val total: Long get() = count.toLong() * amount
    val pending: Long get() = installments.filterNot { it.paid }.sumOf { it.amount }
}

class MainActivity : ComponentActivity() {
    override fun onResume() {
        super.onResume()
        DebtNotifications.scheduleDailyCheck(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(16, 20, 25)
        window.navigationBarColor = android.graphics.Color.rgb(16, 20, 25)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        DebtNotifications.createChannels(this)
        DebtNotifications.scheduleDailyCheck(this)
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 4101)
        }
        if (Build.VERSION.SDK_INT >= 31) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                runCatching {
                    startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).setData(Uri.parse("package:$packageName")))
                }
            }
        }
        setContent { AdminApp() }
    }
}

@Composable
fun AdminApp() {
    val context = LocalContext.current
    var debts by remember { mutableStateOf(loadDebts(context)) }
    var referenceDate by remember {
        mutableStateOf(context.getSharedPreferences("admin_data", Context.MODE_PRIVATE)
            .getString("reference_date", LocalDate.now().format(DateFmt)) ?: LocalDate.now().format(DateFmt))
    }
    var referenceDateDraft by remember { mutableStateOf(referenceDate) }
    var referenceDateError by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<String?>(null) }
    var showConfig by remember { mutableStateOf(false) }
    var paying by remember { mutableStateOf<Installment?>(null) }
    var receiptUri by remember { mutableStateOf<Uri?>(null) }
    var viewingReceipt by remember { mutableStateOf<ReceiptTarget?>(null) }
    var showCreateDebt by remember { mutableStateOf(false) }
    var newDebtName by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var manageAction by remember { mutableStateOf<String?>(null) }
    var targetDebtName by remember { mutableStateOf<String?>(null) }
    var editedDebtName by remember { mutableStateOf("") }

    fun save(next: List<Debt>) {
        debts = next
        persistDebts(context, next)
        DebtNotifications.updateOverdueBadge(context, next)
    }

    LaunchedEffect(debts) {
        DebtNotifications.updateOverdueBadge(context, debts)
        DebtNotifications.scheduleDailyCheck(context)
    }

    BackHandler(enabled = selected != null) { selected = null }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        receiptUri = uri
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Blue,
            onPrimary = Color(0xFF08131B),
            background = BgDark,
            surface = CardDark,
            onSurface = Ink,
            secondary = Blue,
            surfaceVariant = SurfaceDark,
            onSurfaceVariant = Muted
        )
    ) {
        Surface(Modifier.fillMaxSize(), color = BgDark) {
            Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            val current = debts.firstOrNull { it.name == selected }
            if (current == null) {
                Column(Modifier.fillMaxSize().padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text("ADMIN", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Ink)
                            Text("Control de deudas", color = Muted, fontSize = 14.sp)
                        }
                        IconButton(onClick = { newDebtName = ""; showCreateDebt = true }) {
                            Text("+", fontSize = 30.sp, fontWeight = FontWeight.Normal, color = Blue)
                        }
                        TextButton(onClick = { showConfig = true }) { Text("CONFIGURAR") }
                    }
                    Spacer(Modifier.height(12.dp))
                    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = CardDark)) {
                        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("FECHA ACTUAL", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = referenceDateDraft,
                                    onValueChange = { referenceDateDraft = it; referenceDateError = false },
                                    modifier = Modifier.weight(1f),
                                    label = { Text("dd/MM/yyyy") },
                                    singleLine = true,
                                    isError = referenceDateError,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
                                )
                                TextButton(onClick = {
                                    val parsed = runCatching { LocalDate.parse(referenceDateDraft, DateFmt) }.getOrNull()
                                    if (parsed == null) referenceDateError = true
                                    else {
                                        referenceDate = parsed.format(DateFmt)
                                        referenceDateDraft = referenceDate
                                        referenceDateError = false
                                        context.getSharedPreferences("admin_data", Context.MODE_PRIVATE).edit()
                                            .putString("reference_date", referenceDate).apply()
                                    }
                                }) { Text("GUARDAR") }
                            }
                            val annualDate = runCatching { LocalDate.parse(referenceDate, DateFmt) }.getOrNull() ?: LocalDate.now()
                            Text("Periodo anual: 01/01/" + annualDate.year + " – 31/12/" + annualDate.year, color = Muted, fontSize = 12.sp)
                            if (referenceDateError) Text("Fecha no válida. Usa dd/MM/yyyy.", color = Color(0xFFFF6B6B), fontSize = 12.sp)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    debts.forEach { debt ->
                        DebtCard(debt) { selected = debt.name }
                        Spacer(Modifier.height(12.dp))
                    }
                    Spacer(Modifier.weight(1f))
                    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = SurfaceDark)) {
                        Column(Modifier.fillMaxWidth().padding(20.dp)) {
                            Text("DEUDA TOTAL", color = Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("S/ " + money(debts.sumOf { it.pending }), fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Ink)
                        }
                    }
                }
            } else {
                DebtDetail(
                    debt = current,
                    onEdit = { count, amount, date ->
                        val oldPaid = current.installments.filter { it.paid }
                        val generated = generateInstallments(count, amount, date)
                        val preserved = generated.map { fresh ->
                            oldPaid.firstOrNull { it.number == fresh.number }?.let {
                                fresh.copy(paid = true, receipt = it.receipt)
                            } ?: fresh
                        }
                        save(debts.map { if (it.name == current.name) current.copy(count = count, amount = amount, startDate = date, installments = preserved) else it })
                    },
                    onPay = { paying = it; receiptUri = null },
                    onViewReceipt = { installment ->
                        installment.receipt?.let { path -> viewingReceipt = ReceiptTarget(current.name, installment.id, path) }
                    },
                    onEditName = { manageAction = "edit_select" },
                    onDeleteDebt = { manageAction = "delete_select" }
                )
            }

            when (manageAction) {
                "edit_select" -> AlertDialog(
                    onDismissRequest = { manageAction = null; targetDebtName = null },
                    title = { Text("¿Qué deuda quieres editar?") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            debts.forEach { debt ->
                                OutlinedButton(
                                    onClick = {
                                        targetDebtName = debt.name
                                        editedDebtName = debt.name
                                        manageAction = "edit_name"
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text(debt.name) }
                            }
                        }
                    },
                    confirmButton = { TextButton(onClick = { manageAction = null }) { Text("CERRAR") } }
                )
                "edit_name" -> AlertDialog(
                    onDismissRequest = { manageAction = null; targetDebtName = null },
                    title = { Text("Editar nombre") },
                    text = {
                        OutlinedTextField(
                            value = editedDebtName,
                            onValueChange = { editedDebtName = it },
                            label = { Text("Nuevo nombre") },
                            singleLine = true
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            val oldName = targetDebtName
                            val newName = editedDebtName.trim()
                            if (oldName != null && newName.isNotBlank() &&
                                debts.none { it.name.equals(newName, ignoreCase = true) && it.name != oldName }) {
                                save(debts.map { if (it.name == oldName) it.copy(name = newName) else it })
                                if (selected == oldName) selected = newName
                                manageAction = null
                                targetDebtName = null
                            } else {
                                error = if (newName.isBlank()) "Escribe un nombre para la deuda." else "Ya existe una deuda con ese nombre."
                            }
                        }) { Text("GUARDAR") }
                    },
                    dismissButton = {
                        TextButton(onClick = { manageAction = null; targetDebtName = null }) { Text("CANCELAR") }
                    }
                )
                "delete_select" -> AlertDialog(
                    onDismissRequest = { manageAction = null; targetDebtName = null },
                    title = { Text("¿Qué deuda quieres eliminar?") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            debts.forEach { debt ->
                                OutlinedButton(
                                    onClick = {
                                        targetDebtName = debt.name
                                        manageAction = "delete_confirm"
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text(debt.name) }
                            }
                        }
                    },
                    confirmButton = { TextButton(onClick = { manageAction = null }) { Text("CERRAR") } }
                )
                "delete_confirm" -> AlertDialog(
                    onDismissRequest = { manageAction = null; targetDebtName = null },
                    title = { Text("Eliminar deuda") },
                    text = { Text("¿Seguro que deseas eliminar la deuda " + targetDebtName.orEmpty() + " y todos sus datos y cuotas?") },
                    confirmButton = {
                        TextButton(onClick = {
                            val nameToDelete = targetDebtName
                            if (nameToDelete != null) {
                                save(debts.filterNot { it.name == nameToDelete })
                                if (selected == nameToDelete) selected = null
                            }
                            manageAction = null
                            targetDebtName = null
                        }) { Text("ACEPTAR") }
                    },
                    dismissButton = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { manageAction = null; targetDebtName = null }) { Text("RECHAZAR") }
                            TextButton(onClick = { manageAction = null; targetDebtName = null }) { Text("×", fontSize = 22.sp) }
                        }
                    }
                )
            }

            if (showCreateDebt) {
                AlertDialog(
                    onDismissRequest = { showCreateDebt = false },
                    title = { Text("Crear nueva deuda") },
                    text = {
                        OutlinedTextField(value = newDebtName, onValueChange = { newDebtName = it },
                            label = { Text("Nombre de la deuda") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            val name = newDebtName.trim()
                            if (name.isBlank()) error = "Escribe un nombre para la deuda."
                            else if (debts.any { it.name.equals(name, ignoreCase = true) }) error = "Ya existe una deuda con ese nombre."
                            else { save(debts + Debt(name)); showCreateDebt = false; newDebtName = "" }
                        }) { Text("CREAR") }
                    },
                    dismissButton = { TextButton(onClick = { showCreateDebt = false }) { Text("CANCELAR") } }
                )
            }

            if (showConfig) {
                AlertDialog(
                    onDismissRequest = { showConfig = false },
                    title = { Text("Configuración") },
                    text = { Text("Las deudas se guardan en este dispositivo. Para editar una deuda, entra en su tarjeta.") },
                    confirmButton = { TextButton(onClick = { showConfig = false }) { Text("Cerrar") } }
                )
            }

            paying?.let { installment ->
                AlertDialog(
                    onDismissRequest = { paying = null; receiptUri = null },
                    title = { Text("Confirmar pago") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Deuda: $selected")
                            Text("Cuota: ${installment.number}")
                            Text("Monto: S/ ${money(installment.amount)}")
                            OutlinedButton(onClick = { imagePicker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                                Text(if (receiptUri == null) "ADJUNTAR COMPROBANTE / VOUCHER" else "COMPROBANTE ADJUNTADO")
                            }
                            receiptUri?.let { Text("Imagen seleccionada", color = Color(0xFF24864A)) }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            val uri = receiptUri
                            if (uri == null) {
                                error = "Debes adjuntar el comprobante o voucher antes de confirmar el pago"
                            } else {
                                val file = copyReceipt(context, uri)
                                val target = selected
                                if (file != null && target != null) {
                                    save(debts.map { debt ->
                                        if (debt.name != target) debt else debt.copy(installments = debt.installments.map { row ->
                                            if (row.id == installment.id && !row.paid) row.copy(paid = true, receipt = file) else row
                                        })
                                    })
                                    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        vibrator.vibrate(VibrationEffect.createOneShot(140, VibrationEffect.DEFAULT_AMPLITUDE))
                                    } else {
                                        @Suppress("DEPRECATION") vibrator.vibrate(140)
                                    }
                                    paying = null
                                    receiptUri = null
                                } else error = "No se pudo guardar el comprobante. Inténtalo nuevamente."
                            }
                        }) { Text("CONFIRMAR") }
                    },
                    dismissButton = { TextButton(onClick = { paying = null; receiptUri = null }) { Text("Cancelar") } }
                )
            }

            error?.let { message ->
                AlertDialog(onDismissRequest = { error = null }, text = { Text(message) }, confirmButton = {
                    TextButton(onClick = { error = null }) { Text("Aceptar") }
                })
            }

            viewingReceipt?.let { target ->
                ReceiptDialog(path = target.path, onClose = { viewingReceipt = null }, onSaveReplacement = { newPath ->
                    save(debts.map { debt ->
                        if (debt.name != target.debtName) debt else debt.copy(installments = debt.installments.map { row ->
                            if (row.id == target.installmentId) row.copy(receipt = newPath) else row
                        })
                    })
                    if (target.path != newPath) File(target.path).delete()
                    viewingReceipt = target.copy(path = newPath)
                })
            }
            }
        }
    }
}

@Composable
private fun DebtCard(debt: Debt, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            val overdueCount = debt.installments.count { row ->
                !row.paid && (runCatching { LocalDate.parse(row.date, DateFmt) }.getOrNull()?.isBefore(LocalDate.now()) == true)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(debt.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.weight(1f))
                if (overdueCount > 0) Text("! " + overdueCount, color = Color(0xFFFF4D5A), fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            Text("S/ ${money(debt.pending)}", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text("Deuda pendiente", color = Muted, fontSize = 13.sp)
        }
    }
}

@Composable
private fun DebtDetail(
    debt: Debt,
    onEdit: (Int, Long, String) -> Unit,
    onPay: (Installment) -> Unit,
    onViewReceipt: (Installment) -> Unit,
    onEditName: () -> Unit,
    onDeleteDebt: () -> Unit
) {
    var count by remember(debt.name) { mutableStateOf(if (debt.count == 0) "" else debt.count.toString()) }
    var amount by remember(debt.name) { mutableStateOf(if (debt.amount == 0L) "" else money(debt.amount)) }
    var date by remember(debt.name) { mutableStateOf(debt.startDate) }
    var dateError by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(debt.name, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.weight(1f))
            TextButton(onClick = onEditName, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)) {
                Text("EDITAR", fontSize = 11.sp)
            }
            TextButton(onClick = onDeleteDebt, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)) {
                Text("ELIMINAR", fontSize = 11.sp, color = Color.Red)
            }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
            item {
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Text("Configuración de deuda", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        OutlinedTextField(count, { count = it.filter(Char::isDigit) }, label = { Text("Número de cuotas") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(amount, { amount = it.filter { c -> c.isDigit() || c == '.' || c == ',' }.replace(',', '.') }, label = { Text("Monto por cuota (S/)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(date, { date = it }, label = { Text("Fecha inicial (dd/MM/yyyy)") }, placeholder = { Text("08/11/2026") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        if (dateError) Text("Ingresa cuotas, monto y fecha válidos.", color = Color.Red, fontSize = 12.sp)
                        Button(
                            onClick = {
                                val n = count.toIntOrNull()
                                val cents = parseMoney(amount)
                                val parsed = runCatching { LocalDate.parse(date, DateFmt) }.getOrNull()
                                if (n != null && n > 0 && cents != null && cents > 0 && parsed != null) {
                                    onEdit(n, cents, date)
                                    dateError = false
                                } else dateError = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("GUARDAR") }
                        Text("Deuda pendiente: S/ ${money(debt.pending)}", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            item { Text("Cuotas", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
            if (debt.installments.isEmpty()) {
                item { Text("Configura la deuda para generar las cuotas.", color = Muted) }
            }
            items(debt.installments, key = { it.id }) { installment ->
                Card(shape = RoundedCornerShape(14.dp)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Cuota ${installment.number}", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f),
                                textDecoration = if (installment.paid) TextDecoration.LineThrough else TextDecoration.None,
                                color = if (installment.paid) Color.Red else Ink)
                            Text("S/ ${money(installment.amount)}", fontWeight = FontWeight.Bold,
                                textDecoration = if (installment.paid) TextDecoration.LineThrough else TextDecoration.None,
                                color = if (installment.paid) Color.Red else Ink)
                        }
                        val dueDate = runCatching { LocalDate.parse(installment.date, DateFmt) }.getOrNull()
                        val overdue = !installment.paid && dueDate != null && dueDate.isBefore(LocalDate.now())
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(installment.date, color = Muted, fontSize = 13.sp, modifier = Modifier.weight(1f))
                            if (overdue) Text("VENCIDA", color = Color(0xFFFF4D5A), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        if (installment.paid) {
                            Text("PAGADA", color = Color(0xFF55D68B), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            if (installment.receipt != null) TextButton(onClick = { onViewReceipt(installment) }) { Text("VER COMPROBANTE") }
                        } else {
                            Button(onClick = { onPay(installment) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
                                Text("CANCELAR")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceiptDialog(path: String, onClose: () -> Unit, onSaveReplacement: (String) -> Unit) {
    val context = LocalContext.current
    var replacementUri by remember(path) { mutableStateOf<Uri?>(null) }
    var acceptedReplacementPath by remember(path) { mutableStateOf<String?>(null) }
    val replacementPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        replacementUri = uri
        acceptedReplacementPath = null
    }
    val displayedPath = acceptedReplacementPath ?: path
    val bitmap = remember(displayedPath) {
        runCatching { BitmapFactory.decodeFile(displayedPath)?.asImageBitmap() }.getOrNull()
    }
    val downloadLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/jpeg")) { destination ->
        if (destination != null) {
            runCatching {
                val output = context.contentResolver.openOutputStream(destination) ?: error("No se pudo abrir el destino")
                File(displayedPath).inputStream().use { input -> output.use { input.copyTo(it) } }
            }.onSuccess { Toast.makeText(context, "Comprobante descargado", Toast.LENGTH_SHORT).show() }
             .onFailure { Toast.makeText(context, "No se pudo descargar el comprobante", Toast.LENGTH_LONG).show() }
        }
    }
    fun receiptContentUri(): Uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", File(displayedPath))
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(Modifier.fillMaxWidth(0.94f), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onClose) { Text("×", fontSize = 28.sp) }
                }
                if (bitmap != null) {
                    Image(bitmap, contentDescription = "Comprobante de pago", modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp), contentScale = ContentScale.Fit)
                } else { Text("No se pudo abrir el comprobante guardado.") }
                if (replacementUri != null && acceptedReplacementPath == null) {
                    Button(onClick = {
                        val newPath = copyReceipt(context, replacementUri!!)
                        if (newPath != null) acceptedReplacementPath = newPath
                        else Toast.makeText(context, "No se pudo preparar el nuevo comprobante", Toast.LENGTH_LONG).show()
                    }, modifier = Modifier.fillMaxWidth()) { Text("ACEPTAR") }
                }
                if (acceptedReplacementPath != null) {
                    Button(onClick = {
                        val newPath = acceptedReplacementPath ?: return@Button
                        onSaveReplacement(newPath)
                        replacementUri = null
                        acceptedReplacementPath = null
                    }, modifier = Modifier.fillMaxWidth()) { Text("GUARDAR COMPROBANTE") }
                }
                Button(onClick = { downloadLauncher.launch("comprobante.jpg") }, modifier = Modifier.fillMaxWidth()) { Text("DESCARGAR") }
                if (acceptedReplacementPath == null && replacementUri == null) {
                    Button(onClick = { replacementPicker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                        Text("AÑADIR NUEVO COMPROBANTE")
                    }
                }
                Button(onClick = {
                    runCatching {
                        val uri = receiptContentUri()
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newUri(context.contentResolver, "Comprobante", uri))
                        Toast.makeText(context, "Imagen copiada", Toast.LENGTH_SHORT).show()
                    }.onFailure { Toast.makeText(context, "No se pudo copiar la imagen", Toast.LENGTH_LONG).show() }
                }, modifier = Modifier.fillMaxWidth()) { Text("COPIAR IMAGEN") }
                Button(onClick = {
                    runCatching {
                        val uri = receiptContentUri()
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "image/*"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            clipData = ClipData.newUri(context.contentResolver, "Comprobante", uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Compartir comprobante"))
                    }.onFailure { Toast.makeText(context, "No se pudo compartir el comprobante", Toast.LENGTH_LONG).show() }
                }, modifier = Modifier.fillMaxWidth()) { Text("COMPARTIR") }
            }
        }
    }
}

private fun generateInstallments(count: Int, amount: Long, startText: String): List<Installment> {
    val start = LocalDate.parse(startText, DateFmt)
    val originalDay = start.dayOfMonth
    return (0 until count).map { index ->
        val month = YearMonth.from(start).plusMonths(index.toLong())
        val date = month.atDay(minOf(originalDay, month.lengthOfMonth()))
        Installment(UUID.randomUUID().toString(), index + 1, date.format(DateFmt), amount)
    }
}

private fun parseMoney(value: String): Long? = runCatching {
    BigDecimal(value.trim()).setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact()
}.getOrNull()

private fun money(cents: Long): String = BigDecimal.valueOf(cents, 2).setScale(2).toPlainString()

private fun copyReceipt(context: Context, uri: Uri): String? = runCatching {
    val dir = File(context.filesDir, "receipts").apply { mkdirs() }
    val file = File(dir, "${UUID.randomUUID()}.img")
    context.contentResolver.openInputStream(uri).use { input ->
        requireNotNull(input).use { source -> file.outputStream().use(source::copyTo) }
    }
    file.absolutePath
}.getOrNull()

private fun persistDebts(context: Context, debts: List<Debt>) {
    val root = JSONArray()
    debts.forEach { debt ->
        val d = JSONObject().put("name", debt.name).put("count", debt.count).put("amount", debt.amount).put("startDate", debt.startDate)
        val rows = JSONArray()
        debt.installments.forEach { row ->
            rows.put(JSONObject().put("id", row.id).put("number", row.number).put("date", row.date)
                .put("amount", row.amount).put("paid", row.paid).put("receipt", row.receipt))
        }
        d.put("installments", rows)
        root.put(d)
    }
    context.getSharedPreferences("admin_data", Context.MODE_PRIVATE).edit().putString("debts", root.toString()).apply()
}

private fun loadDebts(context: Context): List<Debt> {
    val raw = context.getSharedPreferences("admin_data", Context.MODE_PRIVATE).getString("debts", null)
        ?: return Names.map { Debt(it) }
    return runCatching {
        val root = JSONArray(raw)
        (0 until root.length()).map { i ->
            val d = root.getJSONObject(i)
            val rows = d.getJSONArray("installments")
            val installments = (0 until rows.length()).map { j ->
                val row = rows.getJSONObject(j)
                Installment(row.getString("id"), row.getInt("number"), row.getString("date"),
                    row.getLong("amount"), row.getBoolean("paid"), row.optString("receipt").takeIf { it.isNotBlank() && it != "null" })
            }
            Debt(d.getString("name"), d.getInt("count"), d.getLong("amount"), d.optString("startDate"), installments)
        }
    }.getOrElse { Names.map { Debt(it) } }
}
