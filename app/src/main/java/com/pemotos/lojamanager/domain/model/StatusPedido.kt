package com.pemotos.lojamanager.domain.model

/** Valores possíveis para [com.pemotos.lojamanager.data.local.entity.PedidoEntity.status]. */
enum class StatusPedido(val label: String) {
    Pendente("Pendente"),
    Recebido("Recebido"),
    Cancelado("Cancelado");

    companion object {
        fun fromLabel(label: String): StatusPedido =
            entries.firstOrNull { it.label == label }
                ?: error("Status de pedido inválido: $label")
    }
}
