package com.pemotos.lojamanager.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.pemotos.lojamanager.data.local.LojaDatabase
import com.pemotos.lojamanager.data.local.entity.VendaEntity
import com.pemotos.lojamanager.data.local.seed.DatabaseSeeder
import com.pemotos.lojamanager.domain.model.FormaPagamento
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
class VendaRepositoryTest {

    private lateinit var db: LojaDatabase
    private lateinit var vendas: VendaRepository
    private lateinit var estoque: EstoqueRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LojaDatabase::class.java,
        ).allowMainThreadQueries().build()
        vendas = VendaRepository(db.vendaDao(), db.produtoDao(), db)
        estoque = EstoqueRepository(db.produtoDao())
        kotlinx.coroutines.runBlocking { DatabaseSeeder(db).seed() }
    }

    @After fun tearDown() = db.close()

    @Test
    fun `nova venda decrementa estoque atual via Flow`() = runTest {
        val produto = estoque.observarProdutos().first().first { it.nome == "Macaquinho" && it.tipo == "S" }
        val antes = estoque.observarResumo().first().first { it.id == produto.id }.estoqueAtual
        vendas.inserir(
            VendaEntity(
                data = LocalDate(2025, 5, 1),
                produtoId = produto.id,
                qtd = 1,
                precoUnit = 32.20,
                formaPgto = FormaPagamento.Pix.label,
            )
        )
        val depois = estoque.observarResumo().first().first { it.id == produto.id }.estoqueAtual
        assertThat(depois).isEqualTo(antes - 1)
    }

    @Test
    fun `inserirValidandoEstoque bloqueia venda acima do estoque disponivel`() = runTest {
        val produto = estoque.observarProdutos().first().first { it.nome == "Conj. Short" && it.tipo == "S" }
        val antes = estoque.observarResumo().first().first { it.id == produto.id }.estoqueAtual

        try {
            vendas.inserirValidandoEstoque(
                VendaEntity(
                    data = LocalDate(2025, 5, 1),
                    produtoId = produto.id,
                    qtd = antes + 1,
                    precoUnit = 39.20,
                    formaPgto = FormaPagamento.Pix.label,
                )
            )
        } catch (e: EstoqueInsuficienteException) {
            assertThat(e.disponivel).isEqualTo(antes)
        }

        val depois = estoque.observarResumo().first().first { it.id == produto.id }.estoqueAtual
        assertThat(depois).isEqualTo(antes)
    }

    @Test
    fun `filtro por periodo retorna apenas vendas dentro do intervalo`() = runTest {
        val noPeriodo = vendas.observarPorPeriodo(
            LocalDate(2025, 4, 1), LocalDate(2025, 4, 3)
        ).first()
        // Seed: 02/04 e 03/04 estão dentro; 04/04 está fora
        assertThat(noPeriodo.map { it.data }).containsExactly(
            LocalDate(2025, 4, 3), LocalDate(2025, 4, 2),
        ).inOrder()
    }

    @Test
    fun `excluir venda restaura o estoque (efeito colateral via DAO derivado)`() = runTest {
        val produto = estoque.observarProdutos().first().first { it.nome == "Macaquinho" && it.tipo == "S" }
        val antes = estoque.observarResumo().first().first { it.id == produto.id }.estoqueAtual
        // Achar a venda do seed (Macaquinho S, qtd 2)
        val vendaSeed = vendas.observarTodas().first().first { it.produtoId == produto.id }
        vendas.excluir(vendaSeed.id)
        val depois = estoque.observarResumo().first().first { it.id == produto.id }.estoqueAtual
        assertThat(depois).isEqualTo(antes + 2)
    }
}
