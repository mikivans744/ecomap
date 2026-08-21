package com.ecomap.app.domain.model

import java.time.Instant

enum class RoleCode { ADMINISTRADOR, CIUDADANO, RECOLECTOR, CENTRO_ACOPIO }
enum class MaterialCategory { PLASTICO, METAL, VIDRIO, PAPEL_CARTON, ELECTRONICO, TEXTIL, OTRO }

data class Rol(val id: String, val codigo: RoleCode, val nombre: String, val activo: Boolean = true)

data class Usuario(
    val id: String,
    val email: String,
    val firebaseUid: String,
    val nombre: String,
    val telefono: String? = null,
    val fotoUrl: String? = null,
    val rolId: String,
    val lat: Double? = null,
    val lon: Double? = null,
    val direccion: String? = null,
    val reputacion: Double = 0.0,
    val puntos: Int = 0,
    val activo: Boolean = true,
    // RECOLECTOR
    val placaVehiculo: String? = null,
    val tipoVehiculo: String? = null,
    val capacidadKg: Double? = null,
    val zonaCobertura: String? = null,
    // CENTRO
    val nombreEmpresa: String? = null,
    val direccionFija: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)

data class CategoriaMaterial(val id:String, val nombre:String, val precioBase:Double, val color:String, val activo:Boolean=true)

enum class PublicationStatus { DISPONIBLE, EN_NEGOCIACION, VENDIDO, CANCELADO }
data class EstadoPublicacion(val id:String, val codigo:PublicationStatus, val nombre:String)

data class Publicacion(
    val id: String,
    val usuarioId: String,
    val categoriaId: String,
    val estadoId: String,
    val titulo: String,
    val descripcion: String,
    val pesoKg: Double,
    val precioSugerido: Double,
    val calidad: String,
    val imagenUrl: String? = null,
    val lat: Double,
    val lon: Double,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)

enum class CenterOfferStatus { OFERTADA, ACEPTADA, RECHAZADA, CONTRAOFERTA, EN_CAMINO, ENTREGADO, FINALIZADO }

data class OfertaCentro(
    val id: String,
    val recolectorId: String,
    val centroId: String,
    val categoria: String,
    val pesoKg: Double,
    val precioSugerido: Double,
    val fotoUrl: String?,
    val estado: CenterOfferStatus,
    val lat: Double,
    val lon: Double,
    val createdAt: Instant = Instant.now(),
    val signatureUrl: String? = null,
    val weightConfirmed: Double? = null
)

enum class NegotiationStatus { OFERTA, CONTRAOFERTA, ACEPTADA, RECHAZADA }
data class EstadoNegociacion(val id:String,val codigo:NegotiationStatus,val nombre:String)

data class Oferta(
    val id: String,
    val publicacionId: String,
    val ofertanteId: String,
    val monto: Double,
    val mensaje: String,
    val estado: NegotiationStatus,
    val createdAt: Instant = Instant.now()
)

enum class RequestStatus { SOLICITADA, ACEPTADA, EN_RUTA, RECOLECTADO, ENTREGADO, FINALIZADO }
data class EstadoSolicitud(val id:String,val codigo:RequestStatus,val nombre:String)

data class Solicitud(
    val id: String,
    val publicacionId: String,
    val ciudadanoId: String,
    val recolectorId: String? = null,
    val estado: RequestStatus = RequestStatus.SOLICITADA,
    val createdAt: Instant = Instant.now()
)

data class OfertaRecoleccion(
    val id: String,
    val publicacionId: String,
    val ciudadanoId: String,
    val recolectorId: String,
    val monto: Double,
    val mensaje: String,
    val estado: String = "PENDIENTE", // PENDIENTE, ACEPTADA, RECHAZADA
    val createdAt: Instant = Instant.now()
)

data class Vehiculo(val id:String,val usuarioId:String,val placa:String,val tipo:String,val capacidadKg:Double,val createdAt:Instant=Instant.now())
data class Soporte(val id:String,val usuarioId:String,val categoria:String,val prioridad:String,val estado:String,val titulo:String,val descripcion:String,val respuesta:String?=null,val createdAt:Instant=Instant.now(),val resolvedAt:Instant?=null)
