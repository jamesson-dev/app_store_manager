package com.pemotos.lojamanager.data.repository

import com.pemotos.lojamanager.data.local.dao.PedidoDao
import com.pemotos.lojamanager.data.local.entity.PedidoEntity
import com.pemotos.lojamanager.data.local.entity.PedidoItemEntity
import com.pemotos.lojamanager.domain.model.PedidoItemDetalhe
import com.pemotos.lojamanager.domain.model.PedidoResumo
import com.pemotos.lojamanager.domain.model.StatusPedido
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class PedidoRepository @Inject constructor(
    private val pedidoDao: PedidoDao,
) {
    fun observarResumo(): Flow<List<PedidoResumo>> = pedidoDao.observarResumoPedidos()

    fun observarItens(pedidoId: Long): Flow<List<PedidoItemDetalhe>> =
        pedidoDao.observarItens(pedidoId)

    suspend fun obterPorId(id: Long): PedidoEntity? = pedidoDao.obterPorId(id)

    suspend fun obterItens(pedidoId: Long): List<PedidoItemEntity> = pedidoDao.obterItens(pedidoId)

    suspend fun proximoNumeroPedido(): Int = pedidoDao.maxNumeroPedido() + 1

    /** Insere pedido novo + itens. Retorna o id do pedido criado. */
    suspend fun criar(pedido: PedidoEntity, itens: List<PedidoItemEntity>): Long {
        val pedidoId = pedidoDao.inserirPedido(pedido)
        if (itens.isNotEmpty()) {
            pedidoDao.inserirItens(itens.map { it.copy(id = 0, pedidoId = pedidoId) })
        }
        return pedidoId
    }

    /** Atualiza cabeçalho do pedido + substitui todos os itens. */
    suspend fun atualizar(pedido: PedidoEntity, itens: List<PedidoItemEntity>) {
        pedidoDao.atualizarPedido(pedido)
        pedidoDao.substituirItens(pedido.id, itens)
    }

    suspend fun excluir(pedido: PedidoEntity) = pedidoDao.excluirPedido(pedido)

    suspend fun atualizarStatus(id: Long, status: StatusPedido) =
        pedidoDao.atualizarStatus(id, status.label)
}
