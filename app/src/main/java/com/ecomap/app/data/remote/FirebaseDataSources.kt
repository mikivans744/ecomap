package com.ecomap.app.data.remote

import com.google.firebase.firestore.FirebaseFirestore

class UsuarioFirestoreDataSource(private val firestore: FirebaseFirestore) {
    val collection get() = firestore.collection("usuarios")
}
class MaterialFirestoreDataSource(private val firestore: FirebaseFirestore) {
    val collection get() = firestore.collection("publicaciones")
}
class SolicitudFirestoreDataSource(private val firestore: FirebaseFirestore) {
    val collection get() = firestore.collection("solicitudes")
}
