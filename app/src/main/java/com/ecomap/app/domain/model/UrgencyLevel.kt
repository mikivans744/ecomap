package com.ecomap.app.domain.model

enum class UrgencyLevel(val days: Int) {
    BAJA(7),
    MEDIA(3),
    ALTA(1),
    INMEDIATA(0)
}
