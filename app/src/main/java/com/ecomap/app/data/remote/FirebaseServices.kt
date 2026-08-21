package com.ecomap.app.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirebaseImageStorage(private val storage: FirebaseStorage) {
    fun reference(path: String) = storage.reference.child(path)
}

class FirebaseAuthDataSource(private val auth: FirebaseAuth) {
    suspend fun signIn(email: String, password: String) = auth.signInWithEmailAndPassword(email, password).awaitResult()
    suspend fun sendPasswordReset(email: String) = auth.sendPasswordResetEmail(email).awaitResult()
}

private suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitResult(): T =
    suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { continuation.resume(it) }
        addOnFailureListener { continuation.resumeWithException(it) }
    }

class FirestoreCollections(val firestore: FirebaseFirestore) {
    val usuarios get() = firestore.collection("usuarios")
    val publicaciones get() = firestore.collection("publicaciones")
    val solicitudes get() = firestore.collection("solicitudes")
}
