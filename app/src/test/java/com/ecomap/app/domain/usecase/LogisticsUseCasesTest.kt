package com.ecomap.app.domain.usecase

import com.ecomap.app.domain.model.*
import com.ecomap.app.domain.repository.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class LogisticsUseCasesTest {

    private val fakeRepo = object : SolicitudRepository {
        override fun requests(): Flow<List<Solicitud>> = emptyFlow()
        override suspend fun create(s: Solicitud) = Result.success(s)
        override suspend fun updateStatus(id: String, st: RequestStatus) = Result.success(Unit)
    }

    @Test
    fun `cuando solicita recoleccion retorna exito`() = runTest {
        val useCase = SolicitarRecoleccionUseCase(fakeRepo)
        val request = Solicitud(id="1", publicacionId="p1", ciudadanoId="c1", recolectorId=null)
        val result = useCase(request)
        assertTrue(result.isSuccess)
    }
}
