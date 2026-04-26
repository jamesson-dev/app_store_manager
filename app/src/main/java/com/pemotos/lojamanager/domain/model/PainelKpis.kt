package com.pemotos.lojamanager.domain.model

/** KPIs consolidados — todos derivados em tempo real (Seção 2.5 do CLAUDE.md). */
data class PainelKpis(
    val totalPecasEstoque: Int,
    val valorTotalEstoqueVenda: Double,
    val totalInvestidoCusto: Double,
    val totalVendido: Double,
    val lucroRealizado: Double,
    val pecasVendidas: Int,
)
