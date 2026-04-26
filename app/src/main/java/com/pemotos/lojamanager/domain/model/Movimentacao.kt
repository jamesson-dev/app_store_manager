package com.pemotos.lojamanager.domain.model

import kotlinx.datetime.LocalDate

/** Linha do histórico de movimentações de um produto (entrada ou saída). */
data class Movimentacao(
    val data: LocalDate,
    val tipo: TipoMovimentacao,
    val qtd: Int,
    val precoUnit: Double,
    val origem: String,           // ex: "Pedido nº 1 (GIRASOL FITNESS)" ou "Venda — Pix"
) {
    val total: Double get() = qtd * precoUnit
}

enum class TipoMovimentacao { Entrada, Saida }
