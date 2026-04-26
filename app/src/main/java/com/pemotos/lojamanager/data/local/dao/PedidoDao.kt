package com.pemotos.lojamanager.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.pemotos.lojamanager.data.local.entity.PedidoEntity
import com.pemotos.lojamanager.data.local.entity.PedidoItemEntity
import com.pemotos.lojamanager.domain.model.PedidoItemDetalhe
import com.pemotos.lojamanager.domain.model.PedidoResumo
import kotlinx.coroutines.flow.Flow

@Dao
interface PedidoDao {

    @Query("""
        SELECT
          p.id, p.numeroPedido, p.data, p.fornecedorId,
          f.nome AS fornecedorNome, p.status, p.observacao,
          COALESCE(SUM(pi.qtd * pi.precoUnitCusto), 0.0) AS totalCusto,
          COALESCE(SUM(pi.qtd), 0) AS totalItens
        FROM pedidos p
        JOIN fornecedores f ON f.id = p.fornecedorId
        LEFT JOIN pedido_itens pi ON pi.pedidoId = p.id
        GROUP BY p.id
        ORDER BY p.data DESC, p.numeroPedido DESC
    """)
    fun observarResumoPedidos(): Flow<List<PedidoResumo>>

    @Query("SELECT * FROM pedidos WHERE id = :id LIMIT 1")
    suspend fun obterPorId(id: Long): PedidoEntity?

    @Query("""
        SELECT
          pi.id, pi.pedidoId, pi.produtoId,
          pr.nome AS produtoNome, pr.tipo AS produtoTipo,
          pi.qtd, pi.precoUnitCusto
        FROM pedido_itens pi
        JOIN produtos pr ON pr.id = pi.produtoId
        WHERE pi.pedidoId = :pedidoId
        ORDER BY pr.nome, pr.tipo
    """)
    fun observarItens(pedidoId: Long): Flow<List<PedidoItemDetalhe>>

    @Query("SELECT * FROM pedido_itens WHERE pedidoId = :pedidoId")
    suspend fun obterItens(pedidoId: Long): List<PedidoItemEntity>

    @Query("SELECT COALESCE(MAX(numeroPedido), 0) FROM pedidos")
    suspend fun maxNumeroPedido(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun inserirPedido(pedido: PedidoEntity): Long

    @Update
    suspend fun atualizarPedido(pedido: PedidoEntity)

    @Delete
    suspend fun excluirPedido(pedido: PedidoEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun inserirItens(itens: List<PedidoItemEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun inserirItem(item: PedidoItemEntity): Long

    @Update
    suspend fun atualizarItem(item: PedidoItemEntity)

    @Delete
    suspend fun excluirItem(item: PedidoItemEntity)

    @Query("DELETE FROM pedido_itens WHERE pedidoId = :pedidoId")
    suspend fun excluirItensDoPedido(pedidoId: Long)

    @Transaction
    suspend fun substituirItens(pedidoId: Long, novosItens: List<PedidoItemEntity>) {
        excluirItensDoPedido(pedidoId)
        if (novosItens.isNotEmpty()) {
            inserirItens(novosItens.map { it.copy(id = 0, pedidoId = pedidoId) })
        }
    }

    @Query("UPDATE pedidos SET status = :status WHERE id = :id")
    suspend fun atualizarStatus(id: Long, status: String)
}
