package com.pemotos.lojamanager.data.repository

import com.pemotos.lojamanager.data.local.dao.ProdutoDao
import com.pemotos.lojamanager.data.local.entity.ProdutoEntity
import com.pemotos.lojamanager.domain.model.Movimentacao
import com.pemotos.lojamanager.domain.model.ProdutoResumoEstoque
import com.pemotos.lojamanager.domain.model.TipoMovimentacao
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

@Singleton
class EstoqueRepository @Inject constructor(
    private val produtoDao: ProdutoDao,
) {
    fun observarResumo(): Flow<List<ProdutoResumoEstoque>> = produtoDao.observarResumoEstoque()

    fun observarProdutos(): Flow<List<ProdutoEntity>> = produtoDao.observarTodos()

    suspend fun obterPorId(id: Long): ProdutoEntity? = produtoDao.obterPorId(id)

    suspend fun obterPorNomeTipo(nome: String, tipo: String): ProdutoEntity? =
        produtoDao.obterPorNomeTipo(nome, tipo)

    suspend fun inserir(produto: ProdutoEntity): Long = produtoDao.inserir(produto)

    suspend fun atualizar(produto: ProdutoEntity) = produtoDao.atualizar(produto)

    /** Retorna `false` se houver pedidos/vendas vinculados (excluído bloqueado). */
    suspend fun excluirSeSemVinculos(produto: ProdutoEntity): Boolean {
        if (produtoDao.contarVinculos(produto.id) > 0) return false
        produtoDao.excluir(produto)
        return true
    }

    /** Histórico unificado (entradas via Pedidos Recebidos + saídas via Vendas), do mais recente ao mais antigo. */
    fun observarMovimentacoes(produtoId: Long): Flow<List<Movimentacao>> =
        combine(
            produtoDao.observarEntradas(produtoId),
            produtoDao.observarSaidas(produtoId),
        ) { entradas, saidas ->
            val items = buildList(entradas.size + saidas.size) {
                entradas.forEach {
                    add(
                        Movimentacao(
                            data = it.data,
                            tipo = TipoMovimentacao.Entrada,
                            qtd = it.qtd,
                            precoUnit = it.precoUnit,
                            origem = "Pedido nº ${it.numeroPedido} (${it.fornecedorNome})",
                        )
                    )
                }
                saidas.forEach {
                    add(
                        Movimentacao(
                            data = it.data,
                            tipo = TipoMovimentacao.Saida,
                            qtd = it.qtd,
                            precoUnit = it.precoUnit,
                            origem = "Venda — ${it.formaPgto}",
                        )
                    )
                }
            }
            items.sortedWith(compareByDescending<Movimentacao> { it.data }.thenBy { it.tipo.ordinal })
        }
}
