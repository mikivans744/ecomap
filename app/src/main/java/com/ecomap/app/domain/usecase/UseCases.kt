package com.ecomap.app.domain.usecase
import com.ecomap.app.domain.model.*
import com.ecomap.app.domain.repository.*
class IniciarSesionUseCase(private val r:UsuarioRepository){ suspend operator fun invoke(e:String,p:String)=r.signIn(e,p) }
class RegistrarUsuarioUseCase(private val r:UsuarioRepository){ suspend operator fun invoke(u:Usuario,p:String)=r.register(u,p) }
class PublicarMaterialUseCase(private val r:MaterialRepository){ suspend operator fun invoke(p:Publicacion)=r.publish(p) }
class CrearOfertaUseCase(private val r:OfertaRepository){ suspend operator fun invoke(o:Oferta)=r.create(o) }
class AceptarOfertaUseCase(private val r:OfertaRepository){ suspend operator fun invoke(id:String)=r.updateStatus(id,NegotiationStatus.ACEPTADA) }
class RechazarOfertaUseCase(private val r:OfertaRepository){ suspend operator fun invoke(id:String)=r.updateStatus(id,NegotiationStatus.RECHAZADA) }
class SolicitarRecoleccionUseCase(private val r:SolicitudRepository){ suspend operator fun invoke(s:Solicitud)=r.create(s) }
class AceptarSolicitudUseCase(private val r:SolicitudRepository){ suspend operator fun invoke(id:String)=r.updateStatus(id,RequestStatus.ACEPTADA) }
class RegistrarEntregaUseCase(private val r:SolicitudRepository){ suspend operator fun invoke(id:String)=r.updateStatus(id,RequestStatus.ENTREGADO) }
