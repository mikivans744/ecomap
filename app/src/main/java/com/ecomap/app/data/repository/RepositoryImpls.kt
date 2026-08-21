package com.ecomap.app.data.repository

import android.net.Uri
import android.util.Log
import com.ecomap.app.data.local.EcoMapDatabase
import com.ecomap.app.data.local.MaterialPendienteEntity
import com.ecomap.app.domain.model.*
import com.ecomap.app.domain.repository.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

class UsuarioRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : UsuarioRepository {
    override suspend fun signIn(email:String,password:String): Result<Usuario> = try {
        val authResult = auth.signInWithEmailAndPassword(email, password).await()
        val uid = authResult.user?.uid ?: throw Exception("Usuario no encontrado")
        val doc = firestore.collection("usuarios").document(uid).get().await()
        val user = Usuario(
            id = uid,
            email = email,
            firebaseUid = uid,
            nombre = doc.getString("nombre") ?: "Usuario",
            rolId = doc.getString("rol") ?: "CIUDADANO",
            telefono = doc.getString("telefono"),
            reputacion = doc.getDouble("reputacion") ?: 0.0,
            puntos = doc.getLong("puntos")?.toInt() ?: 0,
            placaVehiculo = doc.getString("placa_vehiculo"),
            tipoVehiculo = doc.getString("tipo_vehiculo"),
            capacidadKg = doc.getDouble("capacidad_kg"),
            zonaCobertura = doc.getString("zona_cobertura"),
            nombreEmpresa = doc.getString("nombre_empresa"),
            direccionFija = doc.getString("direccion_fija"),
            lat = doc.getDouble("lat"),
            lon = doc.getDouble("lon")
        )
        Result.success(user)
    } catch(e:Exception) { Result.failure(e) }
    
    override suspend fun register(user:Usuario,password:String): Result<Usuario> = try {
        Log.d("ECOMAP_AUTH", "Repo: Iniciando createUserWithEmailAndPassword para ${user.email}")
        val authResult = auth.createUserWithEmailAndPassword(user.email, password).await()
        val uid = authResult.user?.uid ?: throw Exception("Error al crear cuenta: UID nulo")
        Log.d("ECOMAP_AUTH", "Repo: Auth OK uid=$uid")
        
        val newUser = user.copy(id = uid, firebaseUid = uid)
        
        Log.d("ECOMAP_AUTH", "Repo: Guardando en Firestore coleccion 'usuarios'...")
        firestore.collection("usuarios").document(uid).set(mapOf(
            "uid" to uid,
            "email" to newUser.email,
            "nombre" to newUser.nombre,
            "telefono" to (newUser.telefono ?: ""),
            "rol" to newUser.rolId,
            "reputacion" to 0.0,
            "puntos" to 0,
            "lat" to 0.0,
            "lon" to 0.0,
            "creado_at" to System.currentTimeMillis()
        )).await()
        Log.d("ECOMAP_AUTH", "Repo: Firestore OK")
        
        Result.success(newUser)
    } catch(e:Exception) {
        Log.e("ECOMAP_AUTH", "Repo: Error en register: ${e.message}")
        Result.failure(e)
    }

    override suspend fun resetPassword(email:String): Result<Unit> = try {
        auth.sendPasswordResetEmail(email).await()
        Result.success(Unit)
    } catch(e:Exception) { Result.failure(e) }

    override suspend fun currentUser(): Usuario? = auth.currentUser?.let {
        Usuario(id = it.uid, email = it.email ?: "", firebaseUid = it.uid, nombre = "", rolId = "")
    }

    suspend fun updateProfile(uid: String, updates: Map<String, Any?>): Result<Unit> = try {
        firestore.collection("usuarios").document(uid).update(updates).await()
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }
}

class MaterialRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val db: EcoMapDatabase
) : MaterialRepository {
    
    override fun publications(): Flow<List<Publicacion>> = callbackFlow {
        val listener = firestore.collection("publicaciones")
            .addSnapshotListener { snapshot, _ ->
                val list = snapshot?.documents?.mapNotNull { doc ->
                    Publicacion(
                        id = doc.id,
                        usuarioId = doc.getString("usuarioId") ?: "",
                        categoriaId = doc.getString("categoriaId") ?: "",
                        estadoId = doc.getString("estado") ?: "DISPONIBLE",
                        titulo = doc.getString("titulo") ?: "",
                        descripcion = doc.getString("descripcion") ?: "",
                        pesoKg = doc.getDouble("pesoKg") ?: 0.0,
                        precioSugerido = doc.getDouble("precioSugerido") ?: 0.0,
                        calidad = doc.getString("calidad") ?: "",
                        imagenUrl = doc.getString("imagenUrl"),
                        lat = doc.getDouble("lat") ?: 0.0,
                        lon = doc.getDouble("lon") ?: 0.0
                    )
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }
    
    override suspend fun publish(item: Publicacion): Result<Publicacion> = try {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "anon"
        val imageUri = item.imagenUrl?.let { Uri.parse(it) }
        var finalUrl = ""
        
        if (imageUri != null && imageUri.scheme != "http" && imageUri.scheme != "https") {
            try {
                withTimeout(5000) {
                    val ref = storage.reference.child("materiales/$uid/${System.currentTimeMillis()}.jpg")
                    ref.putFile(imageUri).await()
                    finalUrl = ref.downloadUrl.await().toString()
                }
            } catch (e: Exception) {
                Log.e("ECOMAP_PUB", "Fallo subida Storage, marcando como pendiente: ${e.message}")
                db.materialDao().insertPendiente(MaterialPendienteEntity(
                    titulo = item.titulo,
                    descripcion = item.descripcion,
                    pesoKg = item.pesoKg,
                    localImagePath = item.imagenUrl ?: ""
                ))
            }
        } else {
            finalUrl = item.imagenUrl ?: ""
        }

        val data = mutableMapOf(
            "usuarioId" to uid,
            "titulo" to item.titulo,
            "descripcion" to item.descripcion,
            "pesoKg" to item.pesoKg,
            "categoriaId" to item.categoriaId,
            "estado" to "DISPONIBLE",
            "imagenUrl" to finalUrl,
            "lat" to item.lat,
            "lon" to item.lon,
            "createdAt" to System.currentTimeMillis()
        )
        
        firestore.collection("publicaciones").add(data).await()
        Result.success(item.copy(imagenUrl = finalUrl))
    } catch(e:Exception) {
        Log.e("ECOMAP_PUB", "Error Firestore: ${e.message}")
        Result.failure(e)
    }

    override suspend fun syncPending(): Int {
        val pendientes = db.materialDao().getPendientes()
        var synced = 0
        pendientes.forEach { p ->
            val publication = Publicacion(
                id = "", usuarioId = "", categoriaId = "1", estadoId = "1",
                titulo = p.titulo, descripcion = p.descripcion, pesoKg = p.pesoKg,
                precioSugerido = 0.0, calidad = "Normal", imagenUrl = p.localImagePath,
                lat = 0.0, lon = 0.0
            )
            val result = publish(publication)
            if (result.isSuccess) {
                db.materialDao().deletePendiente(p.id)
                synced++
            }
        }
        return synced
    }
}

class SolicitudRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : SolicitudRepository {
    override fun requests(): Flow<List<Solicitud>> = callbackFlow {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        val listener = firestore.collection("solicitudes")
            .whereIn("ciudadanoId", listOf(uid)) // Simplificado
            .addSnapshotListener { snapshot, _ ->
                val list = snapshot?.documents?.mapNotNull { doc ->
                    Solicitud(
                        id = doc.id,
                        publicacionId = doc.getString("publicacionId") ?: "",
                        ciudadanoId = doc.getString("ciudadanoId") ?: "",
                        recolectorId = doc.getString("recolectorId"),
                        estado = RequestStatus.valueOf(doc.getString("estado") ?: "SOLICITADA")
                    )
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun create(request: Solicitud): Result<Solicitud> = try {
        firestore.collection("solicitudes").add(request).await()
        Result.success(request)
    } catch(e:Exception) { Result.failure(e) }

    override suspend fun updateStatus(id: String, status: RequestStatus): Result<Unit> = try {
        firestore.collection("solicitudes").document(id).update("estado", status.name).await()
        Result.success(Unit)
    } catch(e:Exception) { Result.failure(e) }
}

class OfertaRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : OfertaRepository {
    override suspend fun create(offer: Oferta): Result<Oferta> = try {
        firestore.collection("ofertas").add(offer).await()
        Result.success(offer)
    } catch(e:Exception) { Result.failure(e) }

    override suspend fun updateStatus(id: String, status: NegotiationStatus): Result<Unit> = try {
        firestore.collection("ofertas").document(id).update("estado", status.name).await()
        Result.success(Unit)
    } catch(e:Exception) { Result.failure(e) }
}

class OfertaCentroRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : OfertaCentroRepository {
    override fun offers(): Flow<List<OfertaCentro>> = callbackFlow {
        val listener = firestore.collection("ofertas_centro")
            .addSnapshotListener { snapshot, _ ->
                val list = snapshot?.documents?.mapNotNull { doc ->
                    OfertaCentro(
                        id = doc.id,
                        recolectorId = doc.getString("recolectorId") ?: "",
                        centroId = doc.getString("centroId") ?: "",
                        categoria = doc.getString("categoria") ?: "",
                        pesoKg = doc.getDouble("pesoKg") ?: 0.0,
                        precioSugerido = doc.getDouble("precioSugerido") ?: 0.0,
                        fotoUrl = doc.getString("fotoUrl"),
                        estado = CenterOfferStatus.valueOf(doc.getString("estado") ?: "OFERTADA"),
                        lat = doc.getDouble("lat") ?: 0.0,
                        lon = doc.getDouble("lon") ?: 0.0,
                        weightConfirmed = doc.getDouble("weightConfirmed"),
                        signatureUrl = doc.getString("signatureUrl")
                    )
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun create(offer: OfertaCentro): Result<OfertaCentro> = try {
        firestore.collection("ofertas_centro").add(offer).await()
        Result.success(offer)
    } catch(e:Exception) { Result.failure(e) }

    override suspend fun updateStatus(id: String, status: CenterOfferStatus, weight: Double?, signature: String?): Result<Unit> = try {
        val updates = mutableMapOf<String, Any>("estado" to status.name)
        weight?.let { updates["weightConfirmed"] = it }
        signature?.let { updates["signatureUrl"] = it }
        firestore.collection("ofertas_centro").document(id).update(updates).await()
        Result.success(Unit)
    } catch(e:Exception) { Result.failure(e) }
}

class NotificacionRepositoryImpl @Inject constructor() : NotificacionRepository {
    override suspend fun notify(role: RoleCode, title: String, body: String): Result<Unit> = Result.success(Unit)
}
