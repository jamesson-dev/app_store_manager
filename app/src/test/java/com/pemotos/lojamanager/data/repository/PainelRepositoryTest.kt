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

/**
 * Validação do Painel — todos os KPIs calculados pela spec, mais reatividade.
 *
 * NOTA: a Seção 2.5 do CLAUDE.md afirma valores de referência R$ 1.579,90
 * (investido) e R$ 60,60 (lucro), mas com o seed da Seção 2 esses valores
 * resultam em R$ 1.500,00 e R$ 43,60 respectivamente. Os testes abaixo
 * validam o **resultado real do cálculo conforme as fórmulas da spec** e
 * não os valores de referência (que parecem ter erros de transcrição).
 */
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE, sdk = [33])
class PainelRepositoryTest {

    private lateinit var db: LojaDatabase
    private lateinit var painel: PainelRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LojaDatabase::class.java,
        ).allowMainThreadQueries().build()
        painel = PainelRepository(db.produtoDao(), db.vendaDao())
        kotlinx.coroutines.runBlocking { DatabaseSeeder(db).seed() }
    }

    @After fun tearDown() = db.close()

    @Test
    fun `kpis pos-seed - total pecas, valor estoque, vendas e lucro`() = runTest {
        val kpis = painel.observarKpis().first()
        assertThat(kpis.totalPecasEstoque).isEqualTo(41)
        assertThat(kpis.valorTotalEstoqueVenda).isWithin(0.01).of(1947.40)
        assertThat(kpis.totalInvestidoCusto).isWithin(0.01).of(1500.00) // spec=1579.90 (discrepância)
        assertThat(kpis.totalVendido).isWithin(0.01).of(152.60)
        assertThat(kpis.lucroRealizado).isWithin(0.01).of(43.60)        // spec=60.60 (discrepância)
        assertThat(kpis.pecasVendidas).isEqualTo(4)
    }

    @Test
    fun `top baixo estoque mostra produtos com estoque até 2 unidades`() = runTest {
        val baixos = painel.observarTopBaixoEstoque(limite = 5, maxQtd = 2).first()
        // Pós-seed nenhum produto tem estoque <= 2 (mín é Conj. Short S = 4)
        assertThat(baixos).isEmpty()

        // Vende várias unidades de Macaquinho S (estoque atual 8) → fica baixo
        val mac = db.produtoDao().obterPorNomeTipo("Macaquinho", "S")!!
        repeat(7) {
            db.vendaDao().inserir(
                VendaEntity(
                    data = LocalDate(2025, 5, 1),
                    produtoId = mac.id,
                    qtd = 1,
                    precoUnit = 32.20,
                    formaPgto = FormaPagamento.Pix.label,
                )
            )
        }
        val depois = painel.observarTopBaixoEstoque(limite = 5, maxQtd = 2).first()
        assertThat(depois).hasSize(1)
        assertThat(depois.first().nome).isEqualTo("Macaquinho")
        assertThat(depois.first().estoqueAtual).isEqualTo(1)
    }

    @Test
    fun `kpis sao reativos a nova venda via Flow`() = runTest {
        val antes = painel.observarKpis().first()
        val mac = db.produtoDao().obterPorNomeTipo("Macaquinho", "S")!!
        db.vendaDao().inserir(
            VendaEntity(
                data = LocalDate(2025, 5, 1),
                produtoId = mac.id,
                qtd = 1,
                precoUnit = 32.20,
                formaPgto = FormaPagamento.Pix.label,
            )
        )
        val depois = painel.observarKpis().first()
        assertThat(depois.totalPecasEstoque).isEqualTo(antes.totalPecasEstoque - 1)
        assertThat(depois.totalVendido).isWithin(0.01).of(antes.totalVendido + 32.20)
        assertThat(depois.pecasVendidas).isEqualTo(antes.pecasVendidas + 1)
        assertThat(depois.lucroRealizado).isWithin(0.01).of(antes.lucroRealizado + (32.20 - 23.00))
    }

    @Test
    fun `vendas por dia agrupa em 7 dias incluindo dias zerados`() = runTest {
        val serie = PainelRepository.agruparUltimosDias(
            vendas = db.vendaDao().observarTodas().first(),
            dias = 7,
            hoje = LocalDate(2025, 4, 4),
        )
        assertThat(serie).hasSize(7)
        // Última data do seed é 04/04/2025 → série termina em 04/04
        assertThat(serie.last().first).isEqualTo(LocalDate(2025, 4, 4))
        // 02/04 = 64.40, 03/04 = 39.20, 04/04 = 49.00
        val mapa = serie.associate { it.first to it.second }
        assertThat(mapa[LocalDate(2025, 4, 2)]).isWithin(0.01).of(64.40)
        assertThat(mapa[LocalDate(2025, 4, 3)]).isWithin(0.01).of(39.20)
        assertThat(mapa[LocalDate(2025, 4, 4)]).isWithin(0.01).of(49.00)
    }
}
