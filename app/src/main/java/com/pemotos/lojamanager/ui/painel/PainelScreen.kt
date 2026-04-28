package com.pemotos.lojamanager.ui.painel

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pemotos.lojamanager.domain.model.PainelKpis
import com.pemotos.lojamanager.domain.model.ProdutoResumoEstoque
import com.pemotos.lojamanager.ui.components.EstoqueBadge
import com.pemotos.lojamanager.ui.format.formatBrl
import com.pemotos.lojamanager.ui.theme.StatusOk
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PainelScreen(
    modifier: Modifier = Modifier,
    viewModel: PainelViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        modifier = modifier,
        topBar = { CenterAlignedTopAppBar(title = { Text("Painel") }) },
    ) { inner ->
        if (state.carregando) {
            Box(modifier = Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) {
                Text("Carregando…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { KpisGrid(state.kpis) }
            item {
                CardSecao(titulo = "Vendas dos últimos 7 dias") {
                    GraficoBarras(state.vendasUltimosDias)
                }
            }
            item {
                CardSecao(titulo = "Top 5 com baixo estoque (≤ 2 unidades)") {
                    BaixoEstoque(state.topBaixoEstoque)
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun KpisGrid(kpis: PainelKpis) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            KpiCard(
                titulo = "Peças em estoque",
                valor = kpis.totalPecasEstoque.toString(),
                modifier = Modifier.weight(1f),
            )
            KpiCard(
                titulo = "Valor em estoque (venda)",
                valor = kpis.valorTotalEstoqueVenda.formatBrl(),
                modifier = Modifier.weight(1.4f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            KpiCard(
                titulo = "Total investido (custo)",
                valor = kpis.totalInvestidoCusto.formatBrl(),
                modifier = Modifier.weight(1f),
            )
            KpiCard(
                titulo = "Total vendido",
                valor = kpis.totalVendido.formatBrl(),
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            KpiCard(
                titulo = "Lucro realizado",
                valor = kpis.lucroRealizado.formatBrl(),
                cor = StatusOk,
                modifier = Modifier.weight(1f),
            )
            KpiCard(
                titulo = "Peças vendidas",
                valor = kpis.pecasVendidas.toString(),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun KpiCard(
    titulo: String,
    valor: String,
    modifier: Modifier = Modifier,
    cor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                titulo,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                valor,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = cor,
            )
        }
    }
}

@Composable
private fun CardSecao(titulo: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
private fun BaixoEstoque(itens: List<ProdutoResumoEstoque>) {
    if (itens.isEmpty()) {
        Text("Nenhum produto em alerta de estoque baixo.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        itens.forEach { p ->
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${p.nome} (${p.tipo})",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
                EstoqueBadge(qtd = p.estoqueAtual)
            }
        }
    }
}

@Composable
private fun GraficoBarras(dados: List<Pair<LocalDate, Double>>) {
    if (dados.isEmpty()) {
        Text("Sem vendas registradas.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    val maximo = (dados.maxOf { it.second }).coerceAtLeast(1.0)
    val totalSemana = dados.sumOf { it.second }
    val corBarra = MaterialTheme.colorScheme.primary
    val corBaseline = MaterialTheme.colorScheme.outlineVariant

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "Total: ${totalSemana.formatBrl()}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
            val w = size.width
            val h = size.height
            val n = dados.size
            val gap = 12f
            val larguraBarra = (w - gap * (n - 1)) / n
            // baseline
            drawRect(
                color = corBaseline,
                topLeft = Offset(0f, h - 1f),
                size = Size(width = w, height = 2f),
            )
            dados.forEachIndexed { i, (_, valor) ->
                val barH = (valor / maximo).toFloat() * (h - 4f)
                val x = i * (larguraBarra + gap)
                val y = h - barH
                drawRoundRect(
                    color = corBarra,
                    topLeft = Offset(x, y),
                    size = Size(larguraBarra, barH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            dados.forEach { (d, _) ->
                Text(
                    "${d.dayOfMonth.toString().padStart(2, '0')}/${d.monthNumber.toString().padStart(2, '0')}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

