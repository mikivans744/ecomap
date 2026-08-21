package com.ecomap.app.domain.usecase

import com.ecomap.app.domain.model.*
import com.ecomap.app.domain.repository.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthUseCasesTest {

    private val fakeRepo = object : UsuarioRepository {
        override suspend fun signIn(e: String, p: String) = Result.success(Usuario(id="1", email=e, firebaseUid="uid", nombre="Test", rolId="1"))
        override suspend fun register(u: Usuario, p: String) = Result.success(u)
        override suspend fun resetPassword(e: String) = Result.success(Unit)
        override suspend fun currentUser() = null
    }

    @Test
    fun `cuando inicia sesion retorna exito`() = runTest {
        val useCase = IniciarSesionUseCase(fakeRepo)
        val result = useCase("test@ecomap.com", "123456")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `cuando registra usuario retorna exito`() = runTest {
        val useCase = RegistrarUsuarioUseCase(fakeRepo)
        val user = Usuario(id="1", email="a@a.com", firebaseUid="u", nombre="N", rolId="1")
        val result = useCase(user, "123456")
        assertTrue(result.isSuccess)
    }
}
