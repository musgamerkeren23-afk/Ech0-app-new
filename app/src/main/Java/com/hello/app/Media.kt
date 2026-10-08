package com.hello.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream

object Media {
    /** Kecilkan + putar sesuai EXIF, simpan ke cache sebagai JPEG. */
    fun compressImage(ctx: Context, uri: Uri, maxSide: Int = 1280): File {
        val cr = ctx.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / sample > maxSide * 2 || bounds.outHeight / sample > maxSide * 2) sample *= 2

        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val bmp = cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            ?: throw IllegalStateException("Gagal membaca gambar")

        val orientation = cr.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
            )
        } ?: ExifInterface.ORIENTATION_NORMAL
        val deg = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        val scale = minOf(1f, maxSide.toFloat() / maxOf(bmp.width, bmp.height))
        val m = Matrix().apply {
            postRotate(deg)
            postScale(scale, scale)
        }
        val out = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
        val f = File(ctx.cacheDir, "img_${System.currentTimeMillis()}.jpg")
        FileOutputStream(f).use { out.compress(Bitmap.CompressFormat.JPEG, 82, it) }
        return f
    }
}

class VoiceRecorder(private val ctx: Context) {
    private var rec: MediaRecorder? = null
    private var file: File? = null
    private var startAt = 0L

    @Suppress("DEPRECATION")
    fun start() {
        val f = File(ctx.cacheDir, "vn_${System.currentTimeMillis()}.m4a")
        val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(ctx) else MediaRecorder()
        r.setAudioSource(MediaRecorder.AudioSource.MIC)
        r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        r.setAudioEncodingBitRate(64000)
        r.setAudioSamplingRate(44100)
        r.setOutputFile(f.absolutePath)
        r.prepare()
        r.start()
        rec = r
        file = f
        startAt = System.currentTimeMillis()
    }

    /** Hentikan rekaman. Return file, atau null kalau gagal / terlalu pendek. */
    fun stop(): File? {
        val r = rec ?: return null
        rec = null
        val ok = try { r.stop(); true } catch (e: Exception) { false }
        r.release()
        val f = file
        file = null
        val tooShort = System.currentTimeMillis() - startAt < 700
        if (!ok || tooShort) {
            f?.delete()
            return null
        }
        return f
    }

    fun cancel() {
        stop()?.delete()
    }
}

object AudioPlayer {
    private var mp: MediaPlayer? = null
    var playingUrl by mutableStateOf<String?>(null)
        private set

    fun toggle(url: String) {
        if (playingUrl == url) {
            stop()
            return
        }
        stop()
        val p = MediaPlayer()
        try {
            p.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            p.setDataSource(url)
            p.setOnPreparedListener { it.start() }
            p.setOnCompletionListener { stop() }
            p.setOnErrorListener { _, _, _ -> stop(); true }
            mp = p
            playingUrl = url
            p.prepareAsync()
        } catch (e: Exception) {
            p.release()
            mp = null
            playingUrl = null
        }
    }

    fun stop() {
        try { mp?.release() } catch (_: Exception) {}
        mp = null
        playingUrl = null
    }
}
