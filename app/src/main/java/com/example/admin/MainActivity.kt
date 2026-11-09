package com.example.admin

import android.content.Context
import android.content.Intent
import android.content.ClipData
import android.content.ClipboardManager
import android.net.Uri
import android.widget.Toast
import android.os.Bundle
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
import androidx.core.content.FileProvider
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

private val Blue = Color(0xFF55B8E8)
private val Ink = Color(0xFF20252B)
private val Muted = Color(0xFF737B84)
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AdminApp() }
    }
}

@Composable
fun AdminApp() {
    val context = LocalContext.current
    var debts by remember { mutableStateOf(loadDebts(context)) }
    var selected by remember { mutableStateOf<String?>(null) }
    var showConfig by remember { mutableStateOf(false) }
    var paying by remember { mutableStateOf<Installment?>(null) }
    var receiptUri by remember { mutableStateOf<Uri?>(null) }
    var viewingReceipt by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    fun save(next: List<Debt>) {
        debts = next
        persistDebts(context, next)
    }

    BackHandler(enabled = selected != null) { selected = null }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        receiptUri = uri
    }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Blue,
            onPrimary = Color.White,
            background = Color(0xFFF7F9FB),
            surface = Color.White,
            onSurface = Ink
        )
    ) {
        Surface(Modifier.fillMaxSize(), color = Color(0xFFF7F9FB)) {
            Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            val current = debts.firstOrNull { it.name == selected }
            if (current == null) {
                Column(Modifier.fillMaxSize().padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text("ADMIN", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Ink)
                            Text("Control de deudas", color = Muted, fontSize = 14.sp)
                        }
                        TextButton(onClick = { showConfig = true }) { Text("CONFIGURAR") }
                    }
                    Spacer(Modifier.height(22.dp))
                    debts.forEach { debt ->
                        DebtCard(debt) { selected = debt.name }
                        Spacer(Modifier.height(12.dp))
                    }
                    Spacer(Modifier.weight(1f))
                    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF6FC))) {
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
                    onViewReceipt = { viewingReceipt = it }
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

            viewingReceipt?.let { path ->
                ReceiptDialog(path = path, onClose = { viewingReceipt = null })
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
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(debt.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
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
    onViewReceipt: (String) -> Unit
) {
    var count by remember(debt.name) { mutableStateOf(if (debt.count == 0) "" else debt.count.toString()) }
    var amount by remember(debt.name) { mutableStateOf(if (debt.amount == 0L) "" else money(debt.amount)) }
    var date by remember(debt.name) { mutableStateOf(debt.startDate) }
    var dateError by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(debt.name, fontWeight = FontWeight.Bold, fontSize = 20.sp)
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
                        Text(installment.date, color = Muted, fontSize = 13.sp)
                        if (installment.paid) {
                            Text("PAGADA", color = Color(0xFF23834B), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            if (installment.receipt != null) TextButton(onClick = { onViewReceipt(installment.receipt) }) { Text("VER COMPROBANTE") }
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
private fun ReceiptDialog(path: String, onClose: () -> Unit) {
    val context = LocalContext.current
    val bitmap = remember(path) {
        runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull()
    }
    val downloadLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/jpeg")) { destination ->
        if (destination != null) {
            runCatching {
                val output = context.contentResolver.openOutputStream(destination) ?: error("No se pudo abrir el destino")
                File(path).inputStream().use { input -> output.use { input.copyTo(it) } }
            }.onSuccess { Toast.makeText(context, "Comprobante descargado", Toast.LENGTH_SHORT).show() }
             .onFailure { Toast.makeText(context, "No se pudo descargar el comprobante", Toast.LENGTH_LONG).show() }
        }
    }
    fun receiptContentUri(): Uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", File(path))
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(Modifier.fillMaxWidth(0.94f), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onClose) { Text("×", fontSize = 28.sp) }
                }
                if (bitmap != null) {
                    Image(bitmap, contentDescription = "Comprobante de pago", modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp), contentScale = ContentScale.Fit)
                } else { Text("No se pudo abrir el comprobante guardado.") }
                Button(onClick = { downloadLauncher.launch("comprobante.jpg") }, modifier = Modifier.fillMaxWidth()) { Text("DESCARGAR") }
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
