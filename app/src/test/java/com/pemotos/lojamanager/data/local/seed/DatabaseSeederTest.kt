package com.pemotos.lojamanager.data.local.seed

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.pemotos.lojamanager.data.local.LojaDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Teste de aceitação do seed (Seção 2 do CLAUDE.md):
 * após popular o banco, os números calculados em tempo real devem bater com
 * os valores de referência da planilha.
 *
 * NOTA: alguns "valores de referência" da Seção 2.5 não conferem com a soma
 * dos itens listados na Seção 2.2 (pedido nº 1) e na Seção 2.3 (vendas):
 *   - Total Investido: spec diz R$ 1.579,90; itens somam R$ 1.500,00.
 *   - Lucro Realizado: spec diz R$ 60,60; cálculo (preco_venda − preco_custo) × qtd = R$ 43,60.
 * Os demais valores (41 peças, R$ 1.947,40 estoque, R$ 152,60 vendido, 4 peças vendidas)
 * são consistentes e validados aqui. Os dois discrepantes ficam comentados para
 * o usuário decidir se ajusta a planilha-fonte ou os valores de referência.
 */
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE, sdk = [33])
class DatabaseSeederTest {

    @get:Rule val instantExecutor = InstantTaskExecutorRule()

    private lateinit var db: LojaDatabase
    private lateinit var seeder: DatabaseSeeder

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LojaDatabase::class.java,
        )
            .allowMainThreadQueries()
            .build()
        seeder = DatabaseSeeder(db)
    }

    @After fun tearDown() = db.close()

    @Test
    fun `seed e idempotente`() = runTest {
        seeder.seed()
        val n1 = db.produtoDao().observarTodos().first().size
        seeder.seed()
        val n2 = db.produtoDao().observarTodos().first().size
        assertThat(n2).isEqualTo(n1)
    }

    @Test
    fun `seed produz 6 produtos, 8 fornecedores e 3 vendas`() = runTest {
        seeder.seed()
        assertThat(db.produtoDao().observarTodos().first()).hasSize(6)
        assertThat(db.fornecedorDao().observarTodos().first()).hasSize(8)
        assertThat(db.vendaDao().observarTodas().first()).hasSize(3)
    }

    @Test
    fun `painel - total de pecas em estoque eh 41`() = runTest {
        seeder.seed()
        val resumo = db.produtoDao().observarResumoEstoque().first()
        val total = resumo.sumOf { it.estoqueAtual }
        assertThat(total).isEqualTo(41)
    }

    @Test
    fun `painel - valor total em estoque (venda) eh R$ 1947,40`() = runTest {
        seeder.seed()
        val resumo = db.produtoDao().observarResumoEstoque().first()
        val valor = resumo.sumOf { it.valorEmEstoque }
        assertThat(valor).isWithin(0.01).of(1947.40)
    }

    @Test
    fun `painel - total vendido eh R$ 152,60`() = runTest {
        seeder.seed()
        val total = db.vendaDao().observarTodas().first().sumOf { it.totalVenda }
        assertThat(total).isWithin(0.01).of(152.60)
    }

    @Test
    fun `painel - pecas vendidas eh 4`() = runTest {
        seeder.seed()
        val total = db.vendaDao().observarTodas().first().sumOf { it.qtd }
        assertThat(total).isEqualTo(4)
    }

    @Test
    fun `pedido inicial tem 6 itens com total custo R$ 1500`() = runTest {
        seeder.seed()
        val pedidos = db.pedidoDao().observarResumoPedidos().first()
        assertThat(pedidos).hasSize(1)
        val pedido = pedidos.single()
        assertThat(pedido.totalItens).isEqualTo(45)
        assertThat(pedido.totalCusto).isWithin(0.01).of(1500.00)
        assertThat(pedido.status).isEqualTo("Recebido")
    }

    @Test
    fun `matriz de precos eh aplicada a todas as variantes do mesmo nome`() = runTest {
        seeder.seed()
        val matriz = db.fornecedorDao().observarMatrizPrecos().first()
        // Macaquinho tem 2 variantes (S, P) e 6 fornecedores com preço (BONEKA não vende)
        // ⇒ 12 células para Macaquinho.
        val produtos = db.produtoDao().observarTodos().first()
        val macaquinhoIds = produtos.filter { it.nome == "Macaquinho" }.map { it.id }
        assertThat(macaquinhoIds).hasSize(2)
        val celulasMacaquinho = matriz.filter { it.produtoId in macaquinhoIds }
        assertThat(celulasMacaquinho).hasSize(12)
    }

    @Test
    fun `forcar seed substitui dados existentes sem duplicar`() = runTest {
        seeder.seed()
        val produtos1 = db.produtoDao().observarTodos().first()
        seeder.seed(forcar = true)
        val produtos2 = db.produtoDao().observarTodos().first()
        assertThat(produtos2).hasSize(produtos1.size)
    }
}
