package com.pemotos.lojamanager.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.pemotos.lojamanager.data.local.LojaDatabase
import com.pemotos.lojamanager.data.local.entity.PedidoEntity
import com.pemotos.lojamanager.data.local.entity.PedidoItemEntity
import com.pemotos.lojamanager.data.local.seed.DatabaseSeeder
import com.pemotos.lojamanager.domain.model.StatusPedido
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE, sdk = [33])
class PedidoRepositoryTest {

    private lateinit var db: LojaDatabase
    private lateinit var pedidoRepo: PedidoRepository
    private lateinit var estoque: EstoqueRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LojaDatabase::class.java,
        ).allowMainThreadQueries().build()
        pedidoRepo = PedidoRepository(db.pedidoDao())
        estoque = EstoqueRepository(db.produtoDao())
        kotlinx.coroutines.runBlocking { DatabaseSeeder(db).seed() }
    }

    @After fun tearDown() = db.close()

    @Test
    fun `proximo numero do pedido eh max + 1`() = runTest {
        // Já existe pedido nº 1 do seed.
        val proximo = pedidoRepo.proximoNumeroPedido()
        assertThat(proximo).isEqualTo(2)
    }

    @Test
    fun `criar pedido pendente nao impacta estoque ate ser Recebido`() = runTest {
        val produto = estoque.observarProdutos().first().first { it.nome == "Macaquinho" && it.tipo == "S" }
        val antes = estoque.observarResumo().first().first { it.id == produto.id }.estoqueAtual

        // novo pedido pendente com 5 unidades
        val pedidoId = pedidoRepo.criar(
            pedido = PedidoEntity(
                numeroPedido = 99,
                data = LocalDate(2025, 5, 1),
                fornecedorId = pedidoRepo.observarResumo().first().first().fornecedorId,
                status = StatusPedido.Pendente.label,
            ),
            itens = listOf(
                PedidoItemEntity(pedidoId = 0L, produtoId = produto.id, qtd = 5, precoUnitCusto = 23.0)
            ),
        )

        val depoisPendente = estoque.observarResumo().first().first { it.id == produto.id }.estoqueAtual
        assertThat(depoisPendente).isEqualTo(antes)  // sem mudança

        // muda pra Recebido → estoque incrementa
        pedidoRepo.atualizarStatus(pedidoId, StatusPedido.Recebido)
        val depoisRecebido = estoque.observarResumo().first().first { it.id == produto.id }.estoqueAtual
        assertThat(depoisRecebido).isEqualTo(antes + 5)
    }

    @Test
    fun `substituir itens funciona ao atualizar pedido`() = runTest {
        val pedido = pedidoRepo.observarResumo().first().first()
        val produto = estoque.observarProdutos().first().first { it.nome == "Conj. Calça" && it.tipo == "S" }

        pedidoRepo.atualizar(
            pedido = PedidoEntity(
                id = pedido.id,
                numeroPedido = pedido.numeroPedido,
                data = pedido.data,
                fornecedorId = pedido.fornecedorId,
                status = pedido.status,
                observacao = "Atualizado",
            ),
            itens = listOf(
                PedidoItemEntity(pedidoId = pedido.id, produtoId = produto.id, qtd = 3, precoUnitCusto = 38.0)
            ),
        )

        val itens = pedidoRepo.observarItens(pedido.id).first()
        assertThat(itens).hasSize(1)
        assertThat(itens.single().qtd).isEqualTo(3)
    }
}
