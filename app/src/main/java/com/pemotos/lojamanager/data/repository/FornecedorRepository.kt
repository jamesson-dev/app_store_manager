package com.pemotos.lojamanager.data.repository

import androidx.room.withTransaction
import com.pemotos.lojamanager.data.local.LojaDatabase
import com.pemotos.lojamanager.data.local.dao.FornecedorDao
import com.pemotos.lojamanager.data.local.entity.FornecedorEntity
import com.pemotos.lojamanager.data.local.entity.PrecoFornecedorEntity
import com.pemotos.lojamanager.domain.model.PrecoMatrizCelula
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class FornecedorRepository @Inject constructor(
    private val fornecedorDao: FornecedorDao,
    private val database: LojaDatabase,
) {
    fun observarTodos(): Flow<List<FornecedorEntity>> = fornecedorDao.observarTodos()

    suspend fun obterPorId(id: Long): FornecedorEntity? = fornecedorDao.obterPorId(id)

    suspend fun inserir(fornecedor: FornecedorEntity): Long = fornecedorDao.inserir(fornecedor)

    suspend fun atualizar(fornecedor: FornecedorEntity) = fornecedorDao.atualizar(fornecedor)

    suspend fun excluir(fornecedor: FornecedorEntity) = fornecedorDao.excluir(fornecedor)

    fun observarMatrizPrecos(): Flow<List<PrecoMatrizCelula>> = fornecedorDao.observarMatrizPrecos()

    suspend fun upsertPreco(fornecedorId: Long, produtoId: Long, preco: Double?) {
        upsertPrecos(fornecedorId, listOf(produtoId), preco)
    }

    suspend fun upsertPrecos(fornecedorId: Long, produtoIds: List<Long>, preco: Double?) {
        database.withTransaction {
            produtoIds.distinct().forEach { produtoId ->
                upsertPrecoSemTransacao(fornecedorId, produtoId, preco)
            }
        }
    }

    private suspend fun upsertPrecoSemTransacao(fornecedorId: Long, produtoId: Long, preco: Double?) {
        if (preco == null) {
            fornecedorDao.removerPreco(fornecedorId, produtoId)
        } else {
            fornecedorDao.upsertPreco(
                PrecoFornecedorEntity(
                    fornecedorId = fornecedorId,
                    produtoId = produtoId,
                    preco = preco,
                )
            )
        }
    }
}
