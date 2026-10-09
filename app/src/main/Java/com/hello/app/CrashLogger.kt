package com.hello.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.util.Date

/** Jalan paling awal (sebelum Firebase) lewat ContentProvider ber-initOrder tertinggi. */
class CrashInitProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        context?.let { CrashHandler.install(it) }
        return true
    }

    override fun query(
        uri: Uri, projection: Array<String>?, selection: String?,
        selectionArgs: Array<String>?, sortOrder: String?
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0
    override fun update(
        uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?
    ): Int = 0
}

object CrashHandler {
    @Volatile
    private var installed = false

    fun install(ctx: Context) {
        if (installed) return
        installed = true
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, e ->
            try {
                val sw = StringWriter()
                e.printStackTrace(PrintWriter(sw))
                val report = buildString {
                    appendLine("=== ECH0 CRASH REPORT (v1.5.3) ===")
                    appendLine("Waktu : ${Date()}")
                    appendLine("HP    : ${Build.MANUFACTURER} ${Build.MODEL}")
                    appendLine("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
                    appendLine("Thread: ${thread.name}")
                    appendLine()
                    append(sw.toString())
                }.take(8000)
                try { CrashStore.file(ctx).writeText(report) } catch (_: Throwable) {}
                try { copyToClipboard(ctx, report) } catch (_: Throwable) {}
                try { saveToDownloads(ctx, report) } catch (_: Throwable) {}
            } catch (_: Throwable) {
            }
            previous?.uncaughtException(thread, e)
        }
    }

    private fun copyToClipboard(ctx: Context, report: String) {
        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("ech0-crash", report))
    }

    private fun saveToDownloads(ctx: Context, report: String) {
        if (Build.VERSION.SDK_INT < 29) return
        val v = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, "ech0-crash-${System.currentTimeMillis() / 1000}.txt")
            put(MediaStore.Downloads.MIME_TYPE, "text/plain")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        val uri = ctx.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v) ?: return
        ctx.contentResolver.openOutputStream(uri)?.use { it.write(report.toByteArray()) }
    }
}

object CrashStore {
    fun file(ctx: Context) = File(ctx.filesDir, "crash.txt")
    fun read(ctx: Context): String? =
        file(ctx).takeIf { it.exists() }?.readText()?.takeIf { it.isNotBlank() }

    fun clear(ctx: Context) {
        file(ctx).delete()
    }
}

@Composable
fun CrashScreen(report: String, onContinue: () -> Unit) {
    val ctx = LocalContext.current
    fun copyNow() {
        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("ech0-crash", report))
    }
    LaunchedEffect(report) {
        try {
            copyNow()
            toast(ctx, "Error otomatis tersalin ✅ tinggal paste ke Claude")
        } catch (_: Throwable) {
        }
    }
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.padding(16.dp)) {
            Text("😵 App tadi crash", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("Error udah otomatis tersalin. Tinggal paste ke Claude 📋", fontSize = 13.sp)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    copyNow()
                    toast(ctx, "Error tersalin ✅")
                }) { Text("Salin lagi") }
                OutlinedButton(onClick = onContinue) { Text("Lanjut ke app") }
            }
            Spacer(Modifier.height(12.dp))
            SelectionContainer(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(report, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}