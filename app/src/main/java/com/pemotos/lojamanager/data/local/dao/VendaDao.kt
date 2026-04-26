package com.pemotos.lojamanager.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pemotos.lojamanager.data.local.entity.VendaEntity
import com.pemotos.lojamanager.domain.model.VendaDetalhe
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

@Dao
interface VendaDao {

    @Query("""
        SELECT
          v.id, v.data, v.produtoId,
          pr.nome AS produtoNome, pr.tipo AS produtoTipo, pr.precoCusto,
          v.qtd, v.precoUnit, v.formaPgto, v.cliente, v.telefone
        FROM vendas v
        JOIN produtos pr ON pr.id = v.produtoId
        ORDER BY v.data DESC, v.id DESC
    """)
    fun observarTodas(): Flow<List<VendaDetalhe>>

    @Query("""
        SELECT
          v.id, v.data, v.produtoId,
          pr.nome AS produtoNome, pr.tipo AS produtoTipo, pr.precoCusto,
          v.qtd, v.precoUnit, v.formaPgto, v.cliente, v.telefone
        FROM vendas v
        JOIN produtos pr ON pr.id = v.produtoId
        WHERE v.data >= :inicio AND v.data <= :fim
        ORDER BY v.data DESC, v.id DESC
    """)
    fun observarPorPeriodo(inicio: LocalDate, fim: LocalDate): Flow<List<VendaDetalhe>>

    @Query("""
        SELECT
          v.id, v.data, v.produtoId,
          pr.nome AS produtoNome, pr.tipo AS produtoTipo, pr.precoCusto,
          v.qtd, v.precoUnit, v.formaPgto, v.cliente, v.telefone
        FROM vendas v
        JOIN produtos pr ON pr.id = v.produtoId
        WHERE v.produtoId = :produtoId
        ORDER BY v.data DESC, v.id DESC
    """)
    fun observarPorProduto(produtoId: Long): Flow<List<VendaDetalhe>>

    @Query("SELECT * FROM vendas WHERE id = :id LIMIT 1")
    suspend fun obterPorId(id: Long): VendaEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun inserir(venda: VendaEntity): Long

    @Delete
    suspend fun excluir(venda: VendaEntity)

    @Query("DELETE FROM vendas WHERE id = :id")
    suspend fun excluirPorId(id: Long)
}
