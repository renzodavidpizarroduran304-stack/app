package com.example.admin
import java.time.ZoneId
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import org.json.JSONArray
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object DebtNotifications {
    private const val REMINDER_CHANNEL = "debt_due_reminders"
    private const val BADGE_CHANNEL = "debt_overdue_badge"
    private const val DAILY_ALARM_ID = 5201
    private const val BADGE_NOTIFICATION_ID = 5202
    private const val REMINDER_BASE_ID = 5300
    private val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    fun getReferenceToday(context: Context): LocalDate = LocalDate.now()

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(NotificationChannel(
                REMINDER_CHANNEL, "Avisos de vencimiento", NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Aviso a las 8:30 a. m. un día antes del vencimiento" })
            manager.createNotificationChannel(NotificationChannel(
                BADGE_CHANNEL, "Contador de cuotas vencidas", NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Contador silencioso de cuotas vencidas en el icono de ADMIN"
                setShowBadge(true)
                setSound(null, null)
                enableVibration(false)
                enableLights(false)
            })
        }
    }

    fun scheduleDailyCheck(context: Context) {
        val intent = Intent(context, DebtReminderReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context, DAILY_ALARM_ID, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val now = java.time.ZonedDateTime.now()
        var next = now.toLocalDate().atTime(8, 30).atZone(ZoneId.systemDefault())
        if (!next.isAfter(now)) next = next.plusDays(1)
        val trigger = next.toInstant().toEpochMilli()
        if (Build.VERSION.SDK_INT >= 23) {
            if (Build.VERSION.SDK_INT >= 31 && alarm.canScheduleExactAlarms()) {
                alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending)
            } else {
                alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending)
            }
        } else {
            @Suppress("DEPRECATION")
            alarm.setExact(AlarmManager.RTC_WAKEUP, trigger, pending)
        }
    }

    fun checkTomorrowAndReschedule(context: Context) {
        createChannels(context)
        val tomorrow = getReferenceToday(context).plusDays(1)
        val raw = context.getSharedPreferences("admin_data", Context.MODE_PRIVATE).getString("debts", null)
        if (raw != null) {
            runCatching {
                val debts = JSONArray(raw)
                for (i in 0 until debts.length()) {
                    val debt = debts.getJSONObject(i)
                    val rows = debt.optJSONArray("installments") ?: JSONArray()
                    for (j in 0 until rows.length()) {
                        val row = rows.getJSONObject(j)
                        val date = runCatching { LocalDate.parse(row.optString("date"), formatter) }.getOrNull()
                        if (!row.optBoolean("paid", false) && date == tomorrow) {
                            val id = (debt.optString("name") + row.optString("id")).hashCode()
                            val notification = NotificationCompat.Builder(context, REMINDER_CHANNEL)
                                .setSmallIcon(android.R.drawable.ic_dialog_info)
                                .setContentTitle("Pago próximo: " + debt.optString("name"))
                                .setContentText("Mañana vence la cuota " + row.optInt("number") + ".")
                                .setAutoCancel(true)
                                .setOnlyAlertOnce(true)
                                .build()
                            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                            manager.notify(REMINDER_BASE_ID + kotlin.math.abs(id % 100000), notification)
                        }
                    }
                }
            }
        }
        scheduleDailyCheck(context)
        updateOverdueBadge(context)
    }

    fun updateOverdueBadge(context: Context, debts: List<Debt>? = null) {
        createChannels(context)
        val count = debts?.sumOf { debt ->
            debt.installments.count { row ->
                !row.paid && (runCatching { LocalDate.parse(row.date, formatter) }.getOrNull()?.isBefore(getReferenceToday(context)) == true)
            }
        } ?: countOverdueFromStorage(context)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (count <= 0) {
            manager.cancel(BADGE_NOTIFICATION_ID)
        } else {
            val notification = NotificationCompat.Builder(context, BADGE_CHANNEL)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("ADMIN")
                .setContentText("$count cuotas vencidas pendientes")
                .setNumber(count)
                .setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .setShowWhen(false)
                .build()
            manager.notify(BADGE_NOTIFICATION_ID, notification)
        }
    }

    private fun countOverdueFromStorage(context: Context): Int {
        val raw = context.getSharedPreferences("admin_data", Context.MODE_PRIVATE).getString("debts", null) ?: return 0
        return runCatching {
            val debts = JSONArray(raw)
            var count = 0
            for (i in 0 until debts.length()) {
                val rows = debts.getJSONObject(i).optJSONArray("installments") ?: JSONArray()
                for (j in 0 until rows.length()) {
                    val row = rows.getJSONObject(j)
                    val date = runCatching { LocalDate.parse(row.optString("date"), formatter) }.getOrNull()
                    if (!row.optBoolean("paid", false) && date?.isBefore(getReferenceToday(context)) == true) count++
                }
            }
            count
        }.getOrDefault(0)
    }
}

class DebtReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        DebtNotifications.checkTomorrowAndReschedule(context)
    }
}

class DebtBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            DebtNotifications.createChannels(context)
            DebtNotifications.scheduleDailyCheck(context)
            DebtNotifications.updateOverdueBadge(context)
        }
    }
}
