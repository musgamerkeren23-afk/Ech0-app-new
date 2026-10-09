package com.hello.app

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class UserProfile(
    val uid: String = "",
    val username: String = "",
    val bio: String = "",
    val photoUrl: String = ""
)

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderPhoto: String = "",
    val type: String = "text", // text | image | audio
    val text: String = "",
    val mediaUrl: String = "",
    val createdAt: Long = 0L
)

object Repo {
    private val auth get() = FirebaseAuth.getInstance()
    private val db get() = FirebaseFirestore.getInstance()
    private val storage get() = FirebaseStorage.getInstance()

    val uid: String? get() = auth.currentUser?.uid

    // ---------- AUTH ----------
    suspend fun register(username: String, email: String, pass: String) =
        withContext(NonCancellable) {
            val res = auth.createUserWithEmailAndPassword(email, pass).await()
            val u = res.user ?: throw IllegalStateException("Gagal membuat akun")
            db.collection("users").document(u.uid).set(
                mapOf(
                    "username" to username,
                    "bio" to "",
                    "photoUrl" to "",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

    suspend fun createDefaultProfile() {
        val u = auth.currentUser ?: return
        val name = (u.email ?: "user").substringBefore("@").take(20).ifBlank { "user" }
        db.collection("users").document(u.uid).set(
            mapOf(
                "username" to name,
                "bio" to "",
                "photoUrl" to "",
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun login(email: String, pass: String) {
        auth.signInWithEmailAndPassword(email, pass).await()
    }

    fun logout() = auth.signOut()

    // ---------- USER ----------
    fun listenUser(uid: String, onData: (UserProfile?) -> Unit): ListenerRegistration =
        db.collection("users").document(uid).addSnapshotListener { d, _ ->
            if (d != null && d.exists()) {
                onData(
                    UserProfile(
                        uid = uid,
                        username = d.getString("username") ?: "User",
                        bio = d.getString("bio") ?: "",
                        photoUrl = d.getString("photoUrl") ?: ""
                    )
                )
            } else onData(null)
        }

    suspend fun updateProfile(uid: String, username: String, bio: String) {
        db.collection("users").document(uid)
            .update(mapOf("username" to username, "bio" to bio)).await()
    }

    suspend fun updatePhoto(uid: String, url: String) {
        db.collection("users").document(uid).update("photoUrl", url).await()
    }

    // ---------- CHAT ----------
    private fun toMessage(d: DocumentSnapshot) = ChatMessage(
        id = d.id,
        senderId = d.getString("senderId") ?: "",
        senderName = d.getString("senderName") ?: "User",
        senderPhoto = d.getString("senderPhoto") ?: "",
        type = d.getString("type") ?: "text",
        text = d.getString("text") ?: "",
        mediaUrl = d.getString("mediaUrl") ?: "",
        createdAt = d.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            ?.toDate()?.time ?: 0L
    )

    fun listenMessages(
        onData: (List<ChatMessage>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration =
        db.collection("messages").orderBy("createdAt").limitToLast(100)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    onError(err.message ?: "error")
                    return@addSnapshotListener
                }
                if (snap != null) onData(snap.documents.map { toMessage(it) })
            }

    suspend fun sendMessage(
        me: UserProfile,
        type: String,
        text: String = "",
        mediaUrl: String = ""
    ) {
        db.collection("messages").add(
            mapOf(
                "senderId" to me.uid,
                "senderName" to me.username,
                "senderPhoto" to me.photoUrl,
                "type" to type,
                "text" to text,
                "mediaUrl" to mediaUrl,
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    // ---------- STORAGE ----------
    suspend fun upload(path: String, uri: Uri, contentType: String): String {
        val ref = storage.reference.child(path)
        val meta = StorageMetadata.Builder().setContentType(contentType).build()
        ref.putFile(uri, meta).await()
        return ref.downloadUrl.await().toString()
    }
}