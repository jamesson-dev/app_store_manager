package com.pemotos.lojamanager.domain.model

/**
 * Projeção: produto + agregados derivados de pedidos recebidos e vendas.
 * Não é Entity — vem do JOIN da query [ProdutoDao.observarResumoEstoque].
 *
 * - precoVenda = precoCusto * (1 + markup)  (planilha original)
 * - estoqueAtual = qtdComprada - qtdVendida
 * - valorEmEstoque = estoqueAtual * precoVenda
 */
data class ProdutoResumoEstoque(
    val id: Long,
    val nome: String,
    val tipo: String,
    val precoCusto: Double,
    val markup: Double,
    val qtdComprada: Int,
    val qtdVendida: Int,
) {
    val precoVenda: Double get() = precoCusto * (1.0 + markup)
    val estoqueAtual: Int get() = qtdComprada - qtdVendida
    val valorEmEstoque: Double get() = estoqueAtual * precoVenda
}
