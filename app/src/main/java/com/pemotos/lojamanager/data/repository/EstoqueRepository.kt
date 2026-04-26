package com.pemotos.lojamanager.data.repository

import com.pemotos.lojamanager.data.local.dao.ProdutoDao
import com.pemotos.lojamanager.data.local.entity.ProdutoEntity
import com.pemotos.lojamanager.domain.model.ProdutoResumoEstoque
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class EstoqueRepository @Inject constructor(
    private val produtoDao: ProdutoDao,
) {
    fun observarResumo(): Flow<List<ProdutoResumoEstoque>> = produtoDao.observarResumoEstoque()

    fun observarProdutos(): Flow<List<ProdutoEntity>> = produtoDao.observarTodos()

    suspend fun obterPorId(id: Long): ProdutoEntity? = produtoDao.obterPorId(id)

    suspend fun inserir(produto: ProdutoEntity): Long = produtoDao.inserir(produto)

    suspend fun atualizar(produto: ProdutoEntity) = produtoDao.atualizar(produto)

    /** Retorna `false` se houver pedidos/vendas vinculados (excluído bloqueado). */
    suspend fun excluirSeSemVinculos(produto: ProdutoEntity): Boolean {
        if (produtoDao.contarVinculos(produto.id) > 0) return false
        produtoDao.excluir(produto)
        return true
    }
}
