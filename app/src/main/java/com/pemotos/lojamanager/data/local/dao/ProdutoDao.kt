package com.pemotos.lojamanager.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pemotos.lojamanager.data.local.entity.ProdutoEntity
import com.pemotos.lojamanager.domain.model.ProdutoResumoEstoque
import kotlinx.coroutines.flow.Flow

@Dao
interface ProdutoDao {

    @Query("SELECT * FROM produtos ORDER BY nome, tipo")
    fun observarTodos(): Flow<List<ProdutoEntity>>

    @Query("SELECT * FROM produtos WHERE id = :id LIMIT 1")
    suspend fun obterPorId(id: Long): ProdutoEntity?

    @Query("SELECT * FROM produtos WHERE nome = :nome AND tipo = :tipo LIMIT 1")
    suspend fun obterPorNomeTipo(nome: String, tipo: String): ProdutoEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun inserir(produto: ProdutoEntity): Long

    @Update
    suspend fun atualizar(produto: ProdutoEntity)

    @Delete
    suspend fun excluir(produto: ProdutoEntity)

    /** Conta vínculos do produto em pedidos+vendas — > 0 bloqueia exclusão. */
    @Query("""
        SELECT
          (SELECT COUNT(*) FROM pedido_itens WHERE produtoId = :id) +
          (SELECT COUNT(*) FROM vendas WHERE produtoId = :id)
    """)
    suspend fun contarVinculos(id: Long): Int

    @Query("""
        SELECT
          COALESCE(
            (SELECT SUM(pi.qtd) FROM pedido_itens pi
             JOIN pedidos pd ON pd.id = pi.pedidoId
             WHERE pi.produtoId = :produtoId AND pd.status = 'Recebido'),
            0
          ) -
          COALESCE(
            (SELECT SUM(v.qtd) FROM vendas v WHERE v.produtoId = :produtoId),
            0
          )
        FROM produtos p
        WHERE p.id = :produtoId
        LIMIT 1
    """)
    suspend fun obterEstoqueAtual(produtoId: Long): Int?

    /**
     * Resumo por produto: qtd comprada (apenas pedidos com status 'Recebido'),
     * qtd vendida (todas as vendas) e demais campos para cálculo de estoque atual.
     */
    @Query("""
        SELECT
          p.id,
          p.nome,
          p.tipo,
          p.precoCusto,
          p.markup,
          COALESCE(
            (SELECT SUM(pi.qtd) FROM pedido_itens pi
             JOIN pedidos pd ON pd.id = pi.pedidoId
             WHERE pi.produtoId = p.id AND pd.status = 'Recebido'),
            0
          ) AS qtdComprada,
          COALESCE(
            (SELECT SUM(v.qtd) FROM vendas v WHERE v.produtoId = p.id),
            0
          ) AS qtdVendida
        FROM produtos p
        ORDER BY p.nome, p.tipo
    """)
    fun observarResumoEstoque(): Flow<List<ProdutoResumoEstoque>>

    @Query("DELETE FROM produtos")
    suspend fun limpar()

    /** Histórico de entradas de um produto — itens de pedidos Recebidos. */
    @Query("""
        SELECT pd.data AS data, pi.qtd AS qtd, pi.precoUnitCusto AS precoUnit,
               pd.numeroPedido AS numeroPedido, f.nome AS fornecedorNome
        FROM pedido_itens pi
        JOIN pedidos pd ON pd.id = pi.pedidoId
        JOIN fornecedores f ON f.id = pd.fornecedorId
        WHERE pi.produtoId = :produtoId AND pd.status = 'Recebido'
        ORDER BY pd.data DESC, pd.id DESC
    """)
    fun observarEntradas(produtoId: Long): kotlinx.coroutines.flow.Flow<List<EntradaProdutoLinha>>

    /** Histórico de saídas — vendas do produto. */
    @Query("""
        SELECT data, qtd, precoUnit, formaPgto
        FROM vendas
        WHERE produtoId = :produtoId
        ORDER BY data DESC, id DESC
    """)
    fun observarSaidas(produtoId: Long): kotlinx.coroutines.flow.Flow<List<SaidaProdutoLinha>>
}

/** Linha de entrada usada por [ProdutoDao.observarEntradas]. */
data class EntradaProdutoLinha(
    val data: kotlinx.datetime.LocalDate,
    val qtd: Int,
    val precoUnit: Double,
    val numeroPedido: Int,
    val fornecedorNome: String,
)

/** Linha de saída usada por [ProdutoDao.observarSaidas]. */
data class SaidaProdutoLinha(
    val data: kotlinx.datetime.LocalDate,
    val qtd: Int,
    val precoUnit: Double,
    val formaPgto: String,
)
