package com.pemotos.lojamanager.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.pemotos.lojamanager.data.local.LojaDatabase
import com.pemotos.lojamanager.data.local.entity.ProdutoEntity
import com.pemotos.lojamanager.data.local.seed.DatabaseSeeder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE, sdk = [33])
class EstoqueRepositoryTest {

    private lateinit var db: LojaDatabase
    private lateinit var repo: EstoqueRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LojaDatabase::class.java,
        ).allowMainThreadQueries().build()
        repo = EstoqueRepository(db.produtoDao())
    }

    @After fun tearDown() = db.close()

    @Test
    fun `resumo reflete preco_venda derivado de markup`() = runTest {
        DatabaseSeeder(db).seed()
        val resumo = repo.observarResumo().first().associateBy { it.nome to it.tipo }
        // Macaquinho S: custo 23 * 1.40 = 32.20
        val mac = resumo["Macaquinho" to "S"]!!
        assertThat(mac.precoVenda).isWithin(0.001).of(32.20)
        // Estoque atual = qtdComprada - qtdVendida = 10 - 2 = 8
        assertThat(mac.estoqueAtual).isEqualTo(8)
        // Valor em estoque = 8 * 32.20 = 257.60
        assertThat(mac.valorEmEstoque).isWithin(0.01).of(257.60)
    }

    @Test
    fun `excluir bloqueado quando produto possui pedidos ou vendas`() = runTest {
        DatabaseSeeder(db).seed()
        val macaquinhoS = repo.observarProdutos().first().first { it.nome == "Macaquinho" && it.tipo == "S" }
        val ok = repo.excluirSeSemVinculos(macaquinhoS)
        assertThat(ok).isFalse()
        // ainda existe
        assertThat(repo.obterPorId(macaquinhoS.id)).isNotNull()
    }

    @Test
    fun `excluir permitido para produto novo sem vinculos`() = runTest {
        val id = repo.inserir(ProdutoEntity(nome = "Top", tipo = "M", precoCusto = 25.0, markup = 0.40))
        val produto = repo.obterPorId(id)!!
        val ok = repo.excluirSeSemVinculos(produto)
        assertThat(ok).isTrue()
        assertThat(repo.obterPorId(id)).isNull()
    }
}
