package com.pemotos.lojamanager.domain.model

/** Célula da matriz de pesquisa de preços (linha = produto, coluna = fornecedor). */
data class PrecoMatrizCelula(
    val produtoId: Long,
    val fornecedorId: Long,
    val preco: Double?,
)
