package com.pemotos.lojamanager.domain.model

import kotlinx.datetime.LocalDate

/** Linha da lista de vendas — venda + nome+tipo do produto + lucro daquela venda. */
data class VendaDetalhe(
    val id: Long,
    val data: LocalDate,
    val produtoId: Long,
    val produtoNome: String,
    val produtoTipo: String,
    val precoCusto: Double,
    val qtd: Int,
    val precoUnit: Double,
    val formaPgto: String,
    val cliente: String?,
    val telefone: String?,
) {
    val totalVenda: Double get() = qtd * precoUnit
    val lucro: Double get() = (precoUnit - precoCusto) * qtd
}
