package com.pemotos.lojamanager.ui.vendas

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pemotos.lojamanager.domain.model.FormaPagamento
import com.pemotos.lojamanager.domain.model.VendaDetalhe
import com.pemotos.lojamanager.ui.format.formatBr
import com.pemotos.lojamanager.ui.format.formatBrl
import com.pemotos.lojamanager.ui.theme.StatusOk

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendasListScreen(
    modifier: Modifier = Modifier,
    onNovaVenda: () -> Unit = {},
    viewModel: VendaViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        modifier = modifier,
        topBar = { CenterAlignedTopAppBar(title = { Text("Vendas") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNovaVenda,
                text = { Text("Nova venda") },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            )
        },
    ) { inner ->
        Column(modifier = Modifier.fillMaxSize().padding(inner)) {
            FiltrosPeriodoRow(
                periodo = state.periodo,
                onPeriodo = viewModel::selecionarPeriodo,
            )
            FiltrosFormaRow(
                selecionadas = state.formasSelecionadas,
                onAlternar = viewModel::alternarForma,
            )
            ResumoCard(
                totalReceita = state.totalReceita,
                totalLucro = state.totalLucro,
                totalPecas = state.totalPecas,
                totalVendas = state.itens.size,
            )
            ListaVendas(itens = state.itens, carregando = state.carregando)
        }
    }
}

@Composable
private fun FiltrosPeriodoRow(periodo: PeriodoFiltro, onPeriodo: (PeriodoFiltro) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(PeriodoFiltro.entries) { p ->
            FilterChip(
                selected = p == periodo,
                onClick = { onPeriodo(p) },
                label = { Text(p.label) },
            )
        }
    }
}

@Composable
private fun FiltrosFormaRow(
    selecionadas: Set<FormaPagamento>,
    onAlternar: (FormaPagamento) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(FormaPagamento.entries) { f ->
            FilterChip(
                selected = f in selecionadas,
                onClick = { onAlternar(f) },
                label = { Text(f.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = corDaForma(f),
                    selectedLabelColor = Color.White,
                ),
            )
        }
    }
}

@Composable
private fun ResumoCard(
    totalReceita: Double,
    totalLucro: Double,
    totalPecas: Int,
    totalVendas: Int,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "$totalVendas ${if (totalVendas == 1) "venda" else "vendas"} • $totalPecas peças",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = totalReceita.formatBrl(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "Lucro ${totalLucro.formatBrl()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = StatusOk,
                )
            }
        }
    }
}

@Composable
private fun ListaVendas(itens: List<VendaDetalhe>, carregando: Boolean) {
    when {
        carregando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Carregando…", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        itens.isEmpty() -> Box(Modifier.fillMaxSize().padding(top = 32.dp), contentAlignment = Alignment.TopCenter) {
            Text("Nenhuma venda no filtro atual.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(items = itens, key = { it.id }) { venda -> VendaCard(venda) }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun VendaCard(venda: VendaDetalhe) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${venda.produtoNome} (${venda.produtoTipo})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    venda.totalVenda.formatBrl(),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "${venda.data.formatBr()} • ${venda.qtd} × ${venda.precoUnit.formatBrl()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                FormaChip(forma = FormaPagamento.fromLabel(venda.formaPgto))
            }
            if (!venda.cliente.isNullOrBlank()) {
                Text(
                    "Cliente: ${venda.cliente}${venda.telefone?.let { " • $it" } ?: ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun FormaChip(forma: FormaPagamento) {
    Box(
        modifier = Modifier
            .background(corDaForma(forma), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            forma.label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
        )
    }
}

private fun corDaForma(forma: FormaPagamento): Color = when (forma) {
    FormaPagamento.Pix -> Color(0xFF2E7D32)
    FormaPagamento.Dinheiro -> Color(0xFF558B2F)
    FormaPagamento.Cartao -> Color(0xFF1565C0)
    FormaPagamento.Fiado -> Color(0xFFEF6C00)
}
