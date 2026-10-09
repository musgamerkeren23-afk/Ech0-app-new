package com.hello.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Ech0Theme {
                var crash by remember { mutableStateOf(CrashStore.read(this@MainActivity)) }
                val c = crash
                if (c != null) {
                    CrashScreen(c) {
                        CrashStore.clear(this@MainActivity)
                        crash = null
                    }
                } else {
                    App()
                }
            }
        }
    }
}

@Composable
fun Ech0Theme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF5865F2),
            onPrimary = Color.White,
            background = Color(0xFF1E1F22),
            onBackground = Color.White,
            surface = Color(0xFF2B2D31),
            onSurface = Color.White,
            surfaceVariant = Color(0xFF383A40),
            onSurfaceVariant = Color(0xFFDBDEE1)
        ),
        content = content
    )
}

sealed class Screen {
    object Chat : Screen()
    data class Profile(val uid: String) : Screen()
}

@Composable
fun App() {
    var loggedIn by remember { mutableStateOf(Repo.uid != null) }
    var me by remember { mutableStateOf<UserProfile?>(null) }
    var screen by remember { mutableStateOf<Screen>(Screen.Chat) }

    DisposableEffect(Unit) {
        val auth = FirebaseAuth.getInstance()
        val l = FirebaseAuth.AuthStateListener { loggedIn = it.currentUser != null }
        auth.addAuthStateListener(l)
        onDispose { auth.removeAuthStateListener(l) }
    }

    DisposableEffect(loggedIn) {
        val uid = Repo.uid
        val reg = if (loggedIn && uid != null) Repo.listenUser(uid) { me = it } else null
        if (!loggedIn) {
            me = null
            screen = Screen.Chat
        }
        onDispose { reg?.remove() }
    }

    val m = me
    when {
        !loggedIn -> AuthScreen()
        m == null -> LoadingScreen()
        else -> when (val s = screen) {
            is Screen.Chat -> ChatScreen(m, onOpenProfile = { screen = Screen.Profile(it) })
            is Screen.Profile -> {
                BackHandler { screen = Screen.Chat }
                ProfileScreen(m, s.uid, onBack = { screen = Screen.Chat })
            }
        }
    }
}

@Composable
fun LoadingScreen() {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Text("Memuat profil…", Modifier.padding(top = 16.dp))
        Text(
            "Kalau kelamaan: cek Firestore sudah dibuat + rules-nya.",
            Modifier.padding(top = 4.dp, start = 24.dp, end = 24.dp),
            style = MaterialTheme.typography.bodySmall
        )
        val scope = rememberCoroutineScope()
        val ctx = LocalContext.current
        TextButton(onClick = {
            scope.launch {
                try { Repo.createDefaultProfile() }
                catch (e: Exception) { toast(ctx, "Gagal: ${e.message}") }
            }
        }) { Text("Buat profil otomatis") }
        TextButton(onClick = { Repo.logout() }) { Text("Keluar") }
    }
}