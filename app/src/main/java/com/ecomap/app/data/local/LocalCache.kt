package com.ecomap.app.data.local

import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.RoomDatabase
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ecomap.app.domain.model.Publicacion
import com.ecomap.app.domain.model.Usuario
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "usuarios")
data class UsuarioEntity(
    @PrimaryKey val id: String,
    val nombre: String,
    val email: String,
    val rolId: String,
    val telefono: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "materiales")
data class MaterialEntity(
    @PrimaryKey val id: String,
    val usuarioId: String = "",
    val titulo: String,
    val descripcion: String = "",
    val pesoKg: Double = 0.0,
    val categoriaId: String,
    val estado: String,
    val precioSugerido: Double = 0.0,
    val calidad: String = "",
    val imagenUrl: String? = null,
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val updatedAt: Long = System.currentTimeMillis()
)

fun MaterialEntity.toPublicacion() = Publicacion(
    id = id,
    usuarioId = usuarioId,
    categoriaId = categoriaId,
    estadoId = estado,
    titulo = titulo,
    descripcion = descripcion,
    pesoKg = pesoKg,
    precioSugerido = precioSugerido,
    calidad = calidad,
    imagenUrl = imagenUrl,
    lat = lat,
    lon = lon
)

fun Publicacion.toEntity() = MaterialEntity(
    id = id,
    usuarioId = usuarioId,
    titulo = titulo,
    descripcion = descripcion,
    pesoKg = pesoKg,
    categoriaId = categoriaId,
    estado = estadoId,
    precioSugerido = precioSugerido,
    calidad = calidad,
    imagenUrl = imagenUrl,
    lat = lat,
    lon = lon,
    updatedAt = System.currentTimeMillis()
)

fun UsuarioEntity.toUsuario() = Usuario(
    id = id,
    email = email,
    firebaseUid = id,
    nombre = nombre,
    rolId = rolId,
    telefono = telefono,
    lat = lat,
    lon = lon
)

fun Usuario.toEntity() = UsuarioEntity(
    id = id,
    nombre = nombre,
    email = email,
    rolId = rolId,
    telefono = telefono,
    lat = lat,
    lon = lon,
    updatedAt = System.currentTimeMillis()
)

@Entity(tableName = "solicitudes")
data class SolicitudEntity(
    @PrimaryKey val id: String,
    val publicacionId: String,
    val estado: String,
    val updatedAt: Long
)

@Entity(tableName = "favoritos", primaryKeys = ["usuarioId", "publicacionId"])
data class FavoritoEntity(val usuarioId: String, val publicacionId: String)

@Entity(tableName = "geocercas")
data class GeocercaEntity(
    @PrimaryKey val id: String,
    val usuarioId: String,
    val nombre: String,
    val lat: Double,
    val lon: Double,
    val radioMetros: Double
)

@Entity(tableName = "materiales_pendientes")
data class MaterialPendienteEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val titulo: String,
    val descripcion: String,
    val pesoKg: Double,
    val localImagePath: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface UsuarioDao {
    @Query("SELECT * FROM usuarios WHERE id=:id")
    fun observe(id: String): Flow<UsuarioEntity?>

    @Query("SELECT * FROM usuarios")
    fun observeAll(): Flow<List<UsuarioEntity>>

    @Query("SELECT * FROM usuarios")
    suspend fun getAll(): List<UsuarioEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: UsuarioEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<UsuarioEntity>)
}

@Dao
interface MaterialDao {
    @Query("SELECT * FROM materiales")
    fun observeAll(): Flow<List<MaterialEntity>>

    @Query("SELECT * FROM materiales")
    suspend fun getAll(): List<MaterialEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<MaterialEntity>)

    @Insert
    suspend fun insertPendiente(item: MaterialPendienteEntity)

    @Query("SELECT * FROM materiales_pendientes")
    suspend fun getPendientes(): List<MaterialPendienteEntity>

    @Query("DELETE FROM materiales_pendientes WHERE id=:id")
    suspend fun deletePendiente(id: Int)
}

@Dao
interface SolicitudDao {
    @Query("SELECT * FROM solicitudes")
    fun observeAll(): Flow<List<SolicitudEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: SolicitudEntity)
}

@Database(
    entities = [
        UsuarioEntity::class,
        MaterialEntity::class,
        SolicitudEntity::class,
        FavoritoEntity::class,
        GeocercaEntity::class,
        MaterialPendienteEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class EcoMapDatabase : RoomDatabase() {
    abstract fun usuarioDao(): UsuarioDao
    abstract fun materialDao(): MaterialDao
    abstract fun solicitudDao(): SolicitudDao
}
