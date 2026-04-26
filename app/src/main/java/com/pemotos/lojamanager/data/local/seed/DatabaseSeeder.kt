package com.pemotos.lojamanager.data.local.seed

import androidx.room.withTransaction
import com.pemotos.lojamanager.data.local.LojaDatabase
import com.pemotos.lojamanager.data.local.entity.FornecedorEntity
import com.pemotos.lojamanager.data.local.entity.PedidoEntity
import com.pemotos.lojamanager.data.local.entity.PedidoItemEntity
import com.pemotos.lojamanager.data.local.entity.PrecoFornecedorEntity
import com.pemotos.lojamanager.data.local.entity.ProdutoEntity
import com.pemotos.lojamanager.data.local.entity.VendaEntity
import com.pemotos.lojamanager.domain.model.FormaPagamento
import com.pemotos.lojamanager.domain.model.StatusPedido
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate

/**
 * Popula o banco com os dados da Seção 2 do CLAUDE.md (planilha
 * "Gerenciador_Loja_Roupas.xlsx"). Idempotente: se já existirem produtos no
 * banco, não faz nada — exceto se [forcar] = true, caso em que apaga tudo
 * e re-popula (usado pelo item "Restaurar dados de exemplo").
 */
class DatabaseSeeder @Inject constructor(
    private val database: LojaDatabase,
) {

    suspend fun seed(forcar: Boolean = false) {
        val produtoDao = database.produtoDao()
        val existentes = produtoDao.observarTodos().first()
        if (existentes.isNotEmpty() && !forcar) return

        database.withTransaction {
            if (forcar) {
                limparTudo()
            }
            val produtosPorChave = inserirProdutos()
            val fornecedoresPorNome = inserirFornecedores()
            inserirPrecosFornecedor(produtosPorChave, fornecedoresPorNome)
            inserirPedidoInicial(produtosPorChave, fornecedoresPorNome)
            inserirVendasIniciais(produtosPorChave)
        }
    }

    suspend fun limparTudo() {
        database.withTransaction {
            // ordem importa por causa das FKs (vendas/itens dependem de produto/pedido)
            val db = database.openHelper.writableDatabase
            db.execSQL("DELETE FROM vendas")
            db.execSQL("DELETE FROM pedido_itens")
            db.execSQL("DELETE FROM pedidos")
            db.execSQL("DELETE FROM preco_fornecedor")
            db.execSQL("DELETE FROM produtos")
            db.execSQL("DELETE FROM fornecedores")
        }
    }

    private suspend fun inserirProdutos(): Map<ProdutoChave, Long> {
        val dao = database.produtoDao()
        val map = mutableMapOf<ProdutoChave, Long>()
        for (semente in PRODUTOS_SEED) {
            val id = dao.inserir(
                ProdutoEntity(
                    nome = semente.nome,
                    tipo = semente.tipo,
                    precoCusto = semente.precoCusto,
                    markup = MARKUP_PADRAO,
                )
            )
            map[ProdutoChave(semente.nome, semente.tipo)] = id
        }
        return map
    }

    private suspend fun inserirFornecedores(): Map<String, Long> {
        val dao = database.fornecedorDao()
        return FORNECEDORES_SEED.associate { (nome, url) ->
            nome to dao.inserir(FornecedorEntity(nome = nome, url = url))
        }
    }

    private suspend fun inserirPrecosFornecedor(
        produtosPorChave: Map<ProdutoChave, Long>,
        fornecedoresPorNome: Map<String, Long>,
    ) {
        val dao = database.fornecedorDao()
        // A matriz é por NOME do produto (sem distinção de tipo). Aplicamos o
        // mesmo preço a todas as variantes do mesmo nome.
        val produtosPorNome = produtosPorChave.entries
            .groupBy({ it.key.nome }) { it.value }
        for ((nomeProduto, precosPorFornecedor) in MATRIZ_PRECOS_SEED) {
            val produtoIds = produtosPorNome[nomeProduto].orEmpty()
            for ((nomeFornecedor, preco) in precosPorFornecedor) {
                if (preco == null) continue
                val fornecedorId = fornecedoresPorNome[nomeFornecedor] ?: continue
                for (produtoId in produtoIds) {
                    dao.upsertPreco(
                        PrecoFornecedorEntity(
                            fornecedorId = fornecedorId,
                            produtoId = produtoId,
                            preco = preco,
                        )
                    )
                }
            }
        }
    }

    private suspend fun inserirPedidoInicial(
        produtosPorChave: Map<ProdutoChave, Long>,
        fornecedoresPorNome: Map<String, Long>,
    ) {
        val pedidoDao = database.pedidoDao()
        val fornecedorId = fornecedoresPorNome[FORNECEDOR_PEDIDO_INICIAL] ?: return
        val pedidoId = pedidoDao.inserirPedido(
            PedidoEntity(
                numeroPedido = 1,
                data = LocalDate(2025, 4, 1),
                fornecedorId = fornecedorId,
                status = StatusPedido.Recebido.label,
                observacao = null,
            )
        )
        val itens = ITENS_PEDIDO_INICIAL.mapNotNull { item ->
            val produtoId = produtosPorChave[ProdutoChave(item.nome, item.tipo)]
                ?: return@mapNotNull null
            PedidoItemEntity(
                pedidoId = pedidoId,
                produtoId = produtoId,
                qtd = item.qtd,
                precoUnitCusto = item.precoUnit,
            )
        }
        pedidoDao.inserirItens(itens)
    }

    private suspend fun inserirVendasIniciais(produtosPorChave: Map<ProdutoChave, Long>) {
        val dao = database.vendaDao()
        for (venda in VENDAS_SEED) {
            val produtoId = produtosPorChave[ProdutoChave(venda.nome, venda.tipo)] ?: continue
            dao.inserir(
                VendaEntity(
                    data = venda.data,
                    produtoId = produtoId,
                    qtd = venda.qtd,
                    precoUnit = venda.precoUnit,
                    formaPgto = FormaPagamento.Pix.label,
                    cliente = null,
                    telefone = null,
                )
            )
        }
    }

    private data class ProdutoChave(val nome: String, val tipo: String)

    private data class ProdutoSemente(
        val nome: String,
        val tipo: String,
        val precoCusto: Double,
    )

    private data class ItemPedidoSemente(
        val nome: String,
        val tipo: String,
        val qtd: Int,
        val precoUnit: Double,
    )

    private data class VendaSemente(
        val data: LocalDate,
        val nome: String,
        val tipo: String,
        val qtd: Int,
        val precoUnit: Double,
    )

    companion object {
        private const val MARKUP_PADRAO = 0.40
        private const val FORNECEDOR_PEDIDO_INICIAL = "GIRASOL FITNESS"

        private val PRODUTOS_SEED = listOf(
            ProdutoSemente("Macaquinho",  "S", 23.00),
            ProdutoSemente("Conj. Calça", "S", 38.00),
            ProdutoSemente("Conj. Short", "S", 28.00),
            ProdutoSemente("Macacão",     "S", 35.00),
            ProdutoSemente("Macaquinho",  "P", 40.00),
            ProdutoSemente("Conj. Short", "P", 40.00),
        )

        private val FORNECEDORES_SEED: List<Pair<String, String?>> = listOf(
            "GIRASOL FITNESS" to null,
            "P&H Modas"       to "https://phmodas.vendizap.com/",
            "FITNESSPRO"      to "https://fitnesspro.vendizap.com/",
            "ALLENARE"        to "https://allenaremodafitness.vendizap.com/",
            "CAFRA FITNESS"   to "https://cafrafitnessatacado.vendizap.com/",
            "IMPULSE FITNESS" to "https://impulsefitness.vendizap.com/",
            "BONEKA PINK"     to "https://bonekapink.vendizap.com/",
            "LARAFIT"         to "https://larafit.vendizap.com/",
        )

        // Matriz da Seção 2.4 — por nome de produto, valor null = "não vende".
        private val MATRIZ_PRECOS_SEED: List<Pair<String, List<Pair<String, Double?>>>> = listOf(
            "Macaquinho" to listOf(
                "P&H Modas"       to 30.00,
                "FITNESSPRO"      to 40.00,
                "ALLENARE"        to 29.99,
                "CAFRA FITNESS"   to 45.00,
                "IMPULSE FITNESS" to 30.00,
                "BONEKA PINK"     to null,
                "LARAFIT"         to 36.00,
            ),
            "Conj. Calça" to listOf(
                "P&H Modas"       to 35.00,
                "FITNESSPRO"      to 40.00,
                "ALLENARE"        to 33.99,
                "CAFRA FITNESS"   to 53.00,
                "IMPULSE FITNESS" to 30.00,
                "BONEKA PINK"     to 27.00,
                "LARAFIT"         to 52.00,
            ),
            "Conj. Short" to listOf(
                "P&H Modas"       to 30.00,
                "FITNESSPRO"      to 36.00,
                "ALLENARE"        to 29.99,
                "CAFRA FITNESS"   to 48.00,
                "IMPULSE FITNESS" to 27.50,
                "BONEKA PINK"     to 24.00,
                "LARAFIT"         to 38.00,
            ),
            "Macacão" to listOf(
                "P&H Modas"       to 35.00,
                "FITNESSPRO"      to 40.00,
                "ALLENARE"        to 35.99,
                "CAFRA FITNESS"   to 65.00,
                "IMPULSE FITNESS" to null,
                "BONEKA PINK"     to null,
                "LARAFIT"         to 54.00,
            ),
        )

        private val ITENS_PEDIDO_INICIAL = listOf(
            ItemPedidoSemente("Macaquinho",  "S", 10, 23.00),
            ItemPedidoSemente("Conj. Calça", "S", 10, 38.00),
            ItemPedidoSemente("Conj. Short", "S",  5, 28.00),
            ItemPedidoSemente("Macacão",     "S", 10, 35.00),
            ItemPedidoSemente("Macaquinho",  "P",  5, 40.00),
            ItemPedidoSemente("Conj. Short", "P",  5, 40.00),
        )

        private val VENDAS_SEED = listOf(
            VendaSemente(LocalDate(2025, 4, 2), "Macaquinho",  "S", 2, 32.20),
            VendaSemente(LocalDate(2025, 4, 3), "Conj. Short", "S", 1, 39.20),
            VendaSemente(LocalDate(2025, 4, 4), "Macacão",     "S", 1, 49.00),
        )
    }
}
