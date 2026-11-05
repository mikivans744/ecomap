package com.ecomap.app.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlateValidatorTest {

    @Test
    fun `valida placa de motocicleta correctamente`() {
        val (valid1, _) = PlateValidator.validate("MOTOCICLETA", "123-AB")
        assertTrue(valid1)

        val (valid2, _) = PlateValidator.validate("MOTOCICLETA", "1234-ABC")
        assertTrue(valid2)

        val (valid3, errorMsg) = PlateValidator.validate("MOTOCICLETA", "12345-ABC")
        assertFalse(valid3)
        assertTrue(errorMsg != null && errorMsg.contains("motocicleta"))
    }

    @Test
    fun `valida placa de automovil correctamente`() {
        val (valid1, _) = PlateValidator.validate("AUTOMOVIL", "1234-ABC")
        assertTrue(valid1)

        val (valid2, errorMsg) = PlateValidator.validate("AUTOMOVIL", "123-AB")
        assertFalse(valid2)
        assertTrue(errorMsg != null && errorMsg.contains("AUTOMOVIL"))
    }
}
