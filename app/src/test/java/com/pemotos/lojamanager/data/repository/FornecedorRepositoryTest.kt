package com.pemotos.lojamanager.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.pemotos.lojamanager.data.local.LojaDatabase
import com.pemotos.lojamanager.data.local.entity.FornecedorEntity
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
class FornecedorRepositoryTest {

    private lateinit var db: LojaDatabase
    private lateinit var repo: FornecedorRepository
    private lateinit var estoque: EstoqueRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LojaDatabase::class.java,
        ).allowMainThreadQueries().build()
        repo = FornecedorRepository(db.fornecedorDao(), db)
        estoque = EstoqueRepository(db.produtoDao())
        kotlinx.coroutines.runBlocking { DatabaseSeeder(db).seed() }
    }

    @After fun tearDown() = db.close()

    @Test
    fun `upsertPreco grava e atualiza preco existente`() = runTest {
        val produtos = estoque.observarProdutos().first()
        val fornecedores = repo.observarTodos().first()
        val mac = produtos.first { it.nome == "Macaquinho" && it.tipo == "S" }
        val ph = fornecedores.first { it.nome == "P&H Modas" }

        // do seed: P&H Modas × Macaquinho = 30
        var matriz = repo.observarMatrizPrecos().first().associate { (it.produtoId to it.fornecedorId) to it.preco }
        assertThat(matriz[mac.id to ph.id]).isWithin(0.001).of(30.00)

        repo.upsertPreco(ph.id, mac.id, 28.50)
        matriz = repo.observarMatrizPrecos().first().associate { (it.produtoId to it.fornecedorId) to it.preco }
        assertThat(matriz[mac.id to ph.id]).isWithin(0.001).of(28.50)
    }

    @Test
    fun `upsertPreco com null remove a celula`() = runTest {
        val produtos = estoque.observarProdutos().first()
        val fornecedores = repo.observarTodos().first()
        val mac = produtos.first { it.nome == "Macaquinho" && it.tipo == "S" }
        val ph = fornecedores.first { it.nome == "P&H Modas" }

        repo.upsertPreco(ph.id, mac.id, null)
        val matriz = repo.observarMatrizPrecos().first().associate { (it.produtoId to it.fornecedorId) to it.preco }
        assertThat(matriz[mac.id to ph.id]).isNull()
    }

    @Test
    fun `upsertPrecos atualiza todas as variantes do mesmo modelo`() = runTest {
        val produtos = estoque.observarProdutos().first()
        val fornecedores = repo.observarTodos().first()
        val macaquinhos = produtos.filter { it.nome == "Macaquinho" }
        val ph = fornecedores.first { it.nome == "P&H Modas" }

        repo.upsertPrecos(ph.id, macaquinhos.map { it.id }, 31.90)

        val matriz = repo.observarMatrizPrecos().first().associate { (it.produtoId to it.fornecedorId) to it.preco }
        macaquinhos.forEach { produto ->
            assertThat(matriz[produto.id to ph.id]).isWithin(0.001).of(31.90)
        }
    }

    @Test
    fun `excluir fornecedor faz cascade nos precos vinculados`() = runTest {
        val ph = repo.observarTodos().first().first { it.nome == "P&H Modas" }
        repo.excluir(FornecedorEntity(id = ph.id, nome = ph.nome, url = ph.url))
        val matriz = repo.observarMatrizPrecos().first()
        assertThat(matriz.none { it.fornecedorId == ph.id }).isTrue()
    }
}
