package com.pemotos.lojamanager.domain.model

import kotlinx.datetime.LocalDate

/** Linha resumo da lista de pedidos: cabeçalho + agregados de itens + nome do fornecedor. */
data class PedidoResumo(
    val id: Long,
    val numeroPedido: Int,
    val data: LocalDate,
    val fornecedorId: Long,
    val fornecedorNome: String,
    val status: String,
    val observacao: String?,
    val totalCusto: Double,
    val totalItens: Int,
)
