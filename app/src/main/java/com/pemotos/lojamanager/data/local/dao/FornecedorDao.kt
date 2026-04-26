package com.pemotos.lojamanager.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pemotos.lojamanager.data.local.entity.FornecedorEntity
import com.pemotos.lojamanager.data.local.entity.PrecoFornecedorEntity
import com.pemotos.lojamanager.domain.model.PrecoMatrizCelula
import kotlinx.coroutines.flow.Flow

@Dao
interface FornecedorDao {

    @Query("SELECT * FROM fornecedores ORDER BY nome")
    fun observarTodos(): Flow<List<FornecedorEntity>>

    @Query("SELECT * FROM fornecedores WHERE id = :id LIMIT 1")
    suspend fun obterPorId(id: Long): FornecedorEntity?

    @Query("SELECT * FROM fornecedores WHERE nome = :nome LIMIT 1")
    suspend fun obterPorNome(nome: String): FornecedorEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun inserir(fornecedor: FornecedorEntity): Long

    @Update
    suspend fun atualizar(fornecedor: FornecedorEntity)

    @Delete
    suspend fun excluir(fornecedor: FornecedorEntity)

    /** Pesquisa de preços — todas as células (linha = produto, coluna = fornecedor). */
    @Query("SELECT produtoId, fornecedorId, preco FROM preco_fornecedor")
    fun observarMatrizPrecos(): Flow<List<PrecoMatrizCelula>>

    @Query("""
        SELECT * FROM preco_fornecedor
        WHERE fornecedorId = :fornecedorId AND produtoId = :produtoId
        LIMIT 1
    """)
    suspend fun obterPreco(fornecedorId: Long, produtoId: Long): PrecoFornecedorEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPreco(preco: PrecoFornecedorEntity): Long

    @Query("DELETE FROM preco_fornecedor WHERE fornecedorId = :fornecedorId AND produtoId = :produtoId")
    suspend fun removerPreco(fornecedorId: Long, produtoId: Long)
}
