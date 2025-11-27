package com.example.mobileapp.data.model

enum class EstadoPedido(val displayName: String, val step: Int) {
    PENDIENTE("Pendiente", 0),
    EMPAQUETADO("Empaquetado", 1),
    ENVIADO("Enviado", 2),
    ENTREGADO("Entregado", 3),
    CANCELADO("Cancelado", -1);

    companion object {
        fun fromString(estado: String?): EstadoPedido {
            return values().find { it.name.equals(estado, ignoreCase = true) } ?: PENDIENTE
        }

        fun getNextEstado(currentEstado: EstadoPedido): EstadoPedido? {
            return when (currentEstado) {
                PENDIENTE -> EMPAQUETADO
                EMPAQUETADO -> ENVIADO
                ENVIADO -> ENTREGADO
                ENTREGADO -> null
                CANCELADO -> null
            }
        }

        fun getPreviousEstado(currentEstado: EstadoPedido): EstadoPedido? {
            return when (currentEstado) {
                PENDIENTE -> null
                EMPAQUETADO -> PENDIENTE
                ENVIADO -> EMPAQUETADO
                ENTREGADO -> ENVIADO
                CANCELADO -> null
            }
        }
    }
}
