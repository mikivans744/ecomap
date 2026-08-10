package com.ecomap.app.domain.repository
import com.ecomap.app.domain.model.*
import kotlinx.coroutines.flow.Flow
interface UsuarioRepository { suspend fun signIn(email:String,password:String): Result<Usuario>; suspend fun register(user:Usuario,password:String):Result<Usuario>; suspend fun resetPassword(email:String):Result<Unit>; suspend fun currentUser():Usuario? }
interface MaterialRepository { fun publications():Flow<List<Publicacion>>; suspend fun publish(item:Publicacion):Result<Publicacion> }
interface SolicitudRepository { fun requests():Flow<List<Solicitud>>; suspend fun create(request:Solicitud):Result<Solicitud>; suspend fun updateStatus(id:String,status:RequestStatus):Result<Unit> }
interface OfertaRepository { suspend fun create(offer:Oferta):Result<Oferta>; suspend fun updateStatus(id:String,status:NegotiationStatus):Result<Unit> }
interface NotificacionRepository { suspend fun notify(role:RoleCode,title:String,body:String):Result<Unit> }
