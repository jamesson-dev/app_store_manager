package com.pemotos.lojamanager.domain.model

/** Item de pedido enriquecido com nome+tipo do produto, para exibição. */
data class PedidoItemDetalhe(
    val id: Long,
    val pedidoId: Long,
    val produtoId: Long,
    val produtoNome: String,
    val produtoTipo: String,
    val qtd: Int,
    val precoUnitCusto: Double,
) {
    val totalCusto: Double get() = qtd * precoUnitCusto
}
