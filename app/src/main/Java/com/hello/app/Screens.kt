@file:OptIn(ExperimentalMaterial3Api::class)

package com.hello.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

fun toast(ctx: Context, msg: String) =
    Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()

fun friendlyError(e: Exception): String =
    if (e is FirebaseAuthException) {
        when (e.errorCode) {
            "ERROR_INVALID_EMAIL" -> "Format email salah"
            "ERROR_WEAK_PASSWORD" -> "Password minimal 6 karakter"
            "ERROR_EMAIL_ALREADY_IN_USE" -> "Email sudah dipakai"
            "ERROR_WRONG_PASSWORD", "ERROR_USER_NOT_FOUND", "ERROR_INVALID_CREDENTIAL" ->
                "Email atau password salah"
            "ERROR_OPERATION_NOT_ALLOWED" ->
                "Aktifkan Email/Password di Firebase Authentication dulu"
            else -> e.message ?: "Gagal masuk"
        }
    } else e.message ?: "Terjadi kesalahan"

// ======================= AVATAR =======================
@Composable
fun Avatar(url: String, name: String, size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0xFF5865F2)),
        contentAlignment = Alignment.Center
    ) {
        if (url.isNotBlank()) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                name.take(1).uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value / 2.2f).sp
            )
        }
    }
}

// ======================= LOGIN / REGISTER =======================
@Composable
fun AuthScreen() {
    val scope = rememberCoroutineScope()
    var register by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Ech0", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text("by H4ll0 W0rld", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(24.dp))

            if (register) {
                OutlinedTextField(
                    value = username, onValueChange = { username = it },
                    label = { Text("Username") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
            }
            OutlinedTextField(
                value = email, onValueChange = { email = it },
                label = { Text("Email") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = pass, onValueChange = { pass = it },
                label = { Text("Password") }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )

            error?.let {
                Text(it, color = Color(0xFFFF6B6B), modifier = Modifier.padding(top = 8.dp))
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    error = null
                    if (register && username.trim().length < 3) {
                        error = "Username minimal 3 huruf"
                        return@Button
                    }
                    if (email.isBlank() || pass.isBlank()) {
                        error = "Isi email & password dulu"
                        return@Button
                    }
                    scope.launch {
                        loading = true
                        try {
                            if (register) Repo.register(username.trim(), email.trim(), pass)
                            else Repo.login(email.trim(), pass)
                        } catch (e: Exception) {
                            error = friendlyError(e)
                        }
                        loading = false
                    }
                },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                else Text(if (register) "Daftar" else "Masuk")
            }
            TextButton(onClick = { register = !register; error = null }) {
                Text(if (register) "Sudah punya akun? Masuk" else "Belum punya akun? Daftar")
            }
        }
    }
}

// ======================= CHAT =======================
@Composable
fun ChatScreen(me: UserProfile, onOpenProfile: (String) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var input by remember { mutableStateOf("") }
    var recording by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var viewer by remember { mutableStateOf<String?>(null) }
    var secs by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()
    val recorder = remember { VoiceRecorder(ctx.applicationContext) }

    DisposableEffect(Unit) {
        val reg = Repo.listenMessages(
            onData = { messages = it },
            onError = { toast(ctx, "Gagal memuat chat: $it") }
        )
        onDispose {
            reg.remove()
            recorder.cancel()
            AudioPlayer.stop()
        }
    }
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }
    LaunchedEffect(recording) {
        secs = 0
        while (recording) {
            delay(1000)
            secs++
        }
    }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                busy = true
                try {
                    val f = withContext(Dispatchers.IO) { Media.compressImage(ctx, uri) }
                    val url = Repo.upload("chat_images/${UUID.randomUUID()}.jpg", Uri.fromFile(f), "image/jpeg")
                    Repo.sendMessage(me, "image", mediaUrl = url)
                    f.delete()
                } catch (e: Exception) {
                    toast(ctx, "Gagal kirim gambar: ${e.message}")
                }
                busy = false
            }
        }
    }

    fun startRec() {
        try {
            recorder.start()
            recording = true
        } catch (e: Exception) {
            toast(ctx, "Gagal mulai rekam: ${e.message}")
        }
    }

    val micPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startRec() else toast(ctx, "Izin mikrofon ditolak")
    }

    fun sendVoice() {
        val f = recorder.stop()
        recording = false
        if (f == null) {
            toast(ctx, "Rekaman terlalu pendek")
            return
        }
        scope.launch {
            busy = true
            try {
                val url = Repo.upload("chat_audio/${UUID.randomUUID()}.m4a", Uri.fromFile(f), "audio/mp4")
                Repo.sendMessage(me, "audio", mediaUrl = url)
            } catch (e: Exception) {
                toast(ctx, "Gagal kirim voice: ${e.message}")
            }
            f.delete()
            busy = false
        }
    }

    fun sendText() {
        val t = input.trim()
        if (t.isEmpty()) return
        input = ""
        scope.launch {
            try {
                Repo.sendMessage(me, "text", text = t)
            } catch (e: Exception) {
                toast(ctx, "Gagal kirim: ${e.message}")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ech0 • Chat Umum") },
                actions = {
                    Avatar(me.photoUrl, me.username, 36.dp, Modifier.clickable { onOpenProfile(me.uid) })
                    Spacer(Modifier.width(12.dp))
                }
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                items(messages, key = { it.id }) { m ->
                    MessageItem(
                        m = m,
                        mine = m.senderId == me.uid,
                        onProfile = onOpenProfile,
                        onImage = { viewer = it }
                    )
                }
            }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())

            Surface(color = MaterialTheme.colorScheme.surface) {
                if (recording) {
                    Row(
                        Modifier.fillMaxWidth().padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Mic, null, tint = Color.Red)
                        Text(
                            "  Merekam… ${secs / 60}:${(secs % 60).toString().padStart(2, '0')}",
                            Modifier.weight(1f)
                        )
                        TextButton(onClick = { recorder.cancel(); recording = false }) { Text("Batal") }
                        IconButton(onClick = { sendVoice() }) {
                            Icon(Icons.AutoMirrored.Filled.Send, "Kirim voice", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                } else {
                    Row(
                        Modifier.fillMaxWidth().padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { pickImage.launch("image/*") }, enabled = !busy) {
                            Icon(Icons.Default.AddPhotoAlternate, "Kirim gambar")
                        }
                        TextField(
                            value = input,
                            onValueChange = { input = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Ketik pesan…") },
                            shape = RoundedCornerShape(24.dp),
                            maxLines = 4,
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )
                        if (input.isBlank()) {
                            IconButton(
                                onClick = {
                                    val ok = ContextCompat.checkSelfPermission(
                                        ctx, Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (ok) startRec() else micPerm.launch(Manifest.permission.RECORD_AUDIO)
                                },
                                enabled = !busy
                            ) { Icon(Icons.Default.Mic, "Voice note") }
                        } else {
                            IconButton(onClick = { sendText() }) {
                                Icon(Icons.AutoMirrored.Filled.Send, "Kirim", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }

    viewer?.let { url ->
        Dialog(
            onDismissRequest = { viewer = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().clickable { viewer = null }
            )
        }
    }
}

@Composable
fun MessageItem(
    m: ChatMessage,
    mine: Boolean,
    onProfile: (String) -> Unit,
    onImage: (String) -> Unit
) {
    val time = remember(m.createdAt) {
        if (m.createdAt == 0L) "" else SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(m.createdAt))
    }
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!mine) {
            Avatar(m.senderPhoto, m.senderName, 36.dp, Modifier.clickable { onProfile(m.senderId) })
            Spacer(Modifier.width(8.dp))
        }
        Column(horizontalAlignment = if (mine) Alignment.End else Alignment.Start) {
            if (!mine) {
                Text(
                    m.senderName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onProfile(m.senderId) }
                )
            }
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (mine) Color(0xFF5865F2) else Color(0xFF383A40),
                contentColor = Color.White
            ) {
                Column(Modifier.padding(10.dp)) {
                    when (m.type) {
                        "image" -> AsyncImage(
                            model = m.mediaUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .width(220.dp)
                                .heightIn(min = 120.dp, max = 300.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onImage(m.mediaUrl) }
                        )
                        "audio" -> {
                            val playing = AudioPlayer.playingUrl == m.mediaUrl
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { AudioPlayer.toggle(m.mediaUrl) }) {
                                    Icon(
                                        if (playing) Icons.Default.Stop else Icons.Default.PlayArrow,
                                        "Putar"
                                    )
                                }
                                Text(if (playing) "Memutar…" else "Voice note")
                                Spacer(Modifier.width(8.dp))
                            }
                        }
                        else -> Text(m.text)
                    }
                    if (time.isNotEmpty()) {
                        Text(
                            time,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xB3FFFFFF),
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }
            }
        }
    }
}

// ======================= PROFIL / AKUN =======================
@Composable
fun ProfileScreen(me: UserProfile, uid: String, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val isMine = uid == me.uid
    var other by remember(uid) { mutableStateOf<UserProfile?>(null) }
    var busy by remember { mutableStateOf(false) }

    DisposableEffect(uid) {
        val reg = if (!isMine) Repo.listenUser(uid) { other = it } else null
        onDispose { reg?.remove() }
    }
    val profile = if (isMine) me else other

    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                busy = true
                try {
                    val f = withContext(Dispatchers.IO) { Media.compressImage(ctx, uri, 512) }
                    val url = Repo.upload(
                        "avatars/${me.uid}_${System.currentTimeMillis()}.jpg",
                        Uri.fromFile(f), "image/jpeg"
                    )
                    Repo.updatePhoto(me.uid, url)
                    f.delete()
                    toast(ctx, "Foto profil diganti ✅")
                } catch (e: Exception) {
                    toast(ctx, "Gagal ganti foto: ${e.message}")
                }
                busy = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isMine) "Akun Saya" else "Profil") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali")
                    }
                }
            )
        }
    ) { pad ->
        if (profile == null) {
            Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            var name by remember(profile.username) { mutableStateOf(profile.username) }
            var bio by remember(profile.bio) { mutableStateOf(profile.bio) }

            Column(
                Modifier
                    .padding(pad)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Avatar(profile.photoUrl, profile.username, 110.dp)
                if (busy) {
                    LinearProgressIndicator(Modifier.padding(top = 12.dp).width(110.dp))
                }
                if (isMine) {
                    TextButton(onClick = { pickPhoto.launch("image/*") }, enabled = !busy) {
                        Text("Ganti foto profil")
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = name, onValueChange = { name = it },
                        label = { Text("Username") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = bio, onValueChange = { bio = it },
                        label = { Text("Bio") }, maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                busy = true
                                try {
                                    Repo.updateProfile(me.uid, name.trim(), bio.trim())
                                    toast(ctx, "Tersimpan ✅")
                                } catch (e: Exception) {
                                    toast(ctx, "Gagal simpan: ${e.message}")
                                }
                                busy = false
                            }
                        },
                        enabled = !busy && name.trim().length >= 3,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Simpan") }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { AudioPlayer.stop(); Repo.logout() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Keluar") }
                } else {
                    Spacer(Modifier.height(12.dp))
                    Text(profile.username, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (profile.bio.isBlank()) "Belum ada bio." else profile.bio,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
