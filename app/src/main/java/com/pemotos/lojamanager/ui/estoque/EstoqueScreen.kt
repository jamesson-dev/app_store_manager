package com.pemotos.lojamanager.ui.estoque

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pemotos.lojamanager.domain.model.ProdutoResumoEstoque
import com.pemotos.lojamanager.ui.components.BuscaTextField
import com.pemotos.lojamanager.ui.components.EstoqueBadge
import com.pemotos.lojamanager.ui.format.formatBrl
import com.pemotos.lojamanager.ui.theme.LojaManagerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstoqueScreen(
    modifier: Modifier = Modifier,
    onAbrirProduto: (Long) -> Unit = {},
    viewModel: EstoqueViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val busca by viewModel.buscaState.collectAsStateWithLifecycle()
    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Estoque") })
        },
    ) { inner ->
        Column(modifier = Modifier.padding(inner).fillMaxSize()) {
            BuscaTextField(
                valor = busca,
                onChange = viewModel::atualizarBusca,
                placeholder = "Buscar produto",
            )
            ListaEstoque(
                itens = state.itens,
                carregando = state.carregando,
                onClickItem = onAbrirProduto,
            )
        }
    }
}

@Composable
private fun ListaEstoque(
    itens: List<ProdutoResumoEstoque>,
    carregando: Boolean,
    onClickItem: (Long) -> Unit,
) {
    when {
        carregando -> EstadoVazio(texto = "Carregando…")
        itens.isEmpty() -> EstadoVazio(texto = "Nenhum produto encontrado.")
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp),
        ) {
            items(items = itens, key = { it.id }) { resumo ->
                ProdutoCard(resumo = resumo, onClick = { onClickItem(resumo.id) })
            }
            item { Spacer(Modifier.height(80.dp)) } // respiro pro FAB futuro
        }
    }
}

@Composable
private fun ProdutoCard(resumo: ProdutoResumoEstoque, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = resumo.nome,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = { Text(resumo.tipo) },
                        colors = AssistChipDefaults.assistChipColors(
                            disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            disabledLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ),
                        shape = RoundedCornerShape(50),
                    )
                    Text(
                        text = resumo.precoVenda.formatBrl(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            EstoqueBadge(qtd = resumo.estoqueAtual)
        }
    }
}

@Composable
private fun EstadoVazio(texto: String) {
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = texto, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Preview(showBackground = true)
@Composable
private fun ProdutoCardPreview() {
    LojaManagerTheme {
        ProdutoCard(
            resumo = ProdutoResumoEstoque(
                id = 1, nome = "Macaquinho", tipo = "S",
                precoCusto = 23.0, markup = 0.40, qtdComprada = 10, qtdVendida = 2,
            ),
            onClick = {},
        )
    }
}
