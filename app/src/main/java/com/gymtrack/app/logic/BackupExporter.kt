package com.gymtrack.app.logic

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.gymtrack.app.data.GymDatabase
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Offline backup: dumps the members and payments tables to two CSV files.
 *
 * On Android 10+ the files go to Downloads via MediaStore (no permission
 * needed); on older versions they land in the app's external files dir.
 * Nothing leaves the device — this is a local backup, not a cloud sync.
 */
object BackupExporter {

    private val stampFormat = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US)

    /** Writes members.csv + payments.csv. Returns a user-facing message. */
    suspend fun export(context: Context): String {
        val db = GymDatabase.getInstance(context)
        val members = db.memberDao().observeAll().first()
        val payments = db.paymentDao().observeAll().first()
        val stamp = stampFormat.format(Date())

        val membersCsv = buildString {
            appendLine("id,name,phone,photo_uri,plan_type,plan_fee,plan_start,plan_end")
            for (m in members) {
                appendLine(
                    listOf(
                        m.id.toString(),
                        csv(m.name),
                        csv(m.phone),
                        csv(m.photoUri.orEmpty()),
                        m.planType.name,
                        m.planFee.toString(),
                        MembershipCalculator.formatDate(m.planStartDate),
                        MembershipCalculator.formatDate(m.planEndDate)
                    ).joinToString(",")
                )
            }
        }
        val paymentsCsv = buildString {
            appendLine("id,member_id,amount,date,note")
            for (p in payments) {
                appendLine(
                    listOf(
                        p.id.toString(),
                        p.memberId.toString(),
                        p.amount.toString(),
                        MembershipCalculator.formatDate(p.date),
                        csv(p.note)
                    ).joinToString(",")
                )
            }
        }

        val membersFile = "gymtrack-members-$stamp.csv"
        val paymentsFile = "gymtrack-payments-$stamp.csv"
        writeTextFile(context, membersFile, membersCsv)
        writeTextFile(context, paymentsFile, paymentsCsv)

        val where = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) "Downloads"
        else "app files"
        return "Backup saved to $where: $membersFile, $paymentsFile"
    }

    private fun csv(value: String): String =
        "\"" + value.replace("\"", "\"\"") + "\""

    private fun writeTextFile(context: Context, fileName: String, content: String) {
        val bytes = content.toByteArray(Charsets.UTF_8)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = context.contentResolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI, values
            ) ?: error("MediaStore insert failed")
            context.contentResolver.openOutputStream(uri)?.use { out: OutputStream ->
                out.write(bytes)
            }
        } else {
            val dir = context.getExternalFilesDir(null) ?: context.filesDir
            File(dir, fileName).writeBytes(bytes)
        }
    }
}
