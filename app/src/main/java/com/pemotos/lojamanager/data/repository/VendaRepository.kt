package com.pemotos.lojamanager.data.repository

import androidx.room.withTransaction
import com.pemotos.lojamanager.data.local.LojaDatabase
import com.pemotos.lojamanager.data.local.dao.ProdutoDao
import com.pemotos.lojamanager.data.local.dao.VendaDao
import com.pemotos.lojamanager.data.local.entity.VendaEntity
import com.pemotos.lojamanager.domain.model.VendaDetalhe
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

@Singleton
class VendaRepository @Inject constructor(
    private val vendaDao: VendaDao,
    private val produtoDao: ProdutoDao,
    private val database: LojaDatabase,
) {
    fun observarTodas(): Flow<List<VendaDetalhe>> = vendaDao.observarTodas()

    fun observarPorPeriodo(inicio: LocalDate, fim: LocalDate): Flow<List<VendaDetalhe>> =
        vendaDao.observarPorPeriodo(inicio, fim)

    suspend fun obterPorId(id: Long): VendaEntity? = vendaDao.obterPorId(id)

    suspend fun inserir(venda: VendaEntity): Long = vendaDao.inserir(venda)

    suspend fun inserirValidandoEstoque(venda: VendaEntity): Long =
        database.withTransaction {
            val estoqueAtual = produtoDao.obterEstoqueAtual(venda.produtoId)
                ?: throw ProdutoNaoEncontradoException(venda.produtoId)
            if (venda.qtd > estoqueAtual) {
                throw EstoqueInsuficienteException(estoqueAtual)
            }
            vendaDao.inserir(venda)
        }

    suspend fun excluir(id: Long) = vendaDao.excluirPorId(id)
}

class ProdutoNaoEncontradoException(produtoId: Long) :
    IllegalArgumentException("Produto não encontrado: $produtoId")

class EstoqueInsuficienteException(val disponivel: Int) :
    IllegalStateException("Estoque insuficiente. Disponível: $disponivel.")
