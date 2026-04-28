package com.pemotos.lojamanager.data.repository

import com.pemotos.lojamanager.data.local.dao.ProdutoDao
import com.pemotos.lojamanager.data.local.dao.VendaDao
import com.pemotos.lojamanager.domain.model.PainelKpis
import com.pemotos.lojamanager.domain.model.ProdutoResumoEstoque
import com.pemotos.lojamanager.domain.model.VendaDetalhe
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * Repository do Painel — calcula os 6 KPIs da Seção 2.5 do CLAUDE.md
 * combinando os flows de produtos (resumo) e vendas em tempo real.
 */
@Singleton
class PainelRepository @Inject constructor(
    private val produtoDao: ProdutoDao,
    private val vendaDao: VendaDao,
) {
    fun observarKpis(): Flow<PainelKpis> = combine(
        produtoDao.observarResumoEstoque(),
        vendaDao.observarTodas(),
    ) { resumos, vendas ->
        calcularKpis(resumos, vendas)
    }

    fun observarTopBaixoEstoque(limite: Int = 5, maxQtd: Int = 2): Flow<List<ProdutoResumoEstoque>> =
        produtoDao.observarResumoEstoque().map { lista ->
            lista.filter { it.estoqueAtual <= maxQtd }
                .sortedBy { it.estoqueAtual }
                .take(limite)
        }

    fun observarVendasPorDia(dias: Int = 7): Flow<List<Pair<LocalDate, Double>>> =
        vendaDao.observarTodas().map { vendas ->
            agruparUltimosDias(vendas, dias)
        }

    companion object {
        /** Cálculo direto, isolado e testável. */
        fun calcularKpis(
            resumos: List<ProdutoResumoEstoque>,
            vendas: List<VendaDetalhe>,
        ): PainelKpis {
            val pecas = resumos.sumOf { it.estoqueAtual }
            val valorEstoque = resumos.sumOf { it.valorEmEstoque }
            // Total investido: usa precoCusto do produto * qtdComprada
            // (qtdComprada já é derivada de pedidos Recebidos via SQL).
            val investido = resumos.sumOf { it.qtdComprada * it.precoCusto }
            val totalVendido = vendas.sumOf { it.totalVenda }
            val lucro = vendas.sumOf { it.lucro }
            val pecasVendidas = vendas.sumOf { it.qtd }
            return PainelKpis(
                totalPecasEstoque = pecas,
                valorTotalEstoqueVenda = valorEstoque,
                totalInvestidoCusto = investido,
                totalVendido = totalVendido,
                lucroRealizado = lucro,
                pecasVendidas = pecasVendidas,
            )
        }

        /**
         * Vendas agrupadas pelos últimos N dias (incluindo hoje), em ordem
         * cronológica. Dias sem vendas aparecem com 0,0.
         */
        fun agruparUltimosDias(
            vendas: List<VendaDetalhe>,
            dias: Int,
        ): List<Pair<LocalDate, Double>> {
            val hoje = vendas.maxOfOrNull { it.data } ?: return emptyList()
            val inicio = hoje.minus(dias - 1, DateTimeUnit.DAY)
            val porDia = vendas
                .filter { it.data in inicio..hoje }
                .groupBy { it.data }
                .mapValues { (_, lst) -> lst.sumOf { it.totalVenda } }
            return (0 until dias).map { offset ->
                val d = inicio.plus(offset, DateTimeUnit.DAY)
                d to (porDia[d] ?: 0.0)
            }
        }
    }
}
