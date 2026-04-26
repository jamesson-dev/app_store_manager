package com.pemotos.lojamanager.ui.estoque

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pemotos.lojamanager.domain.model.Movimentacao
import com.pemotos.lojamanager.domain.model.TipoMovimentacao
import com.pemotos.lojamanager.ui.components.EstoqueBadge
import com.pemotos.lojamanager.ui.format.formatBr
import com.pemotos.lojamanager.ui.format.formatBrl
import com.pemotos.lojamanager.ui.theme.StatusDanger
import com.pemotos.lojamanager.ui.theme.StatusOk

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalheProdutoScreen(
    onVoltar: () -> Unit,
    onEditar: (Long) -> Unit,
    viewModel: DetalheProdutoViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var menuAberto by remember { mutableStateOf(false) }
    var confirmarExcluir by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.eventos.collect { ev ->
            when (ev) {
                ExclusaoEvento.Sucesso -> onVoltar()
                is ExclusaoEvento.Bloqueada -> snackbar.showSnackbar(ev.mensagem)
                is ExclusaoEvento.Erro -> snackbar.showSnackbar(ev.mensagem)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.resumo?.let { "${it.nome} ${it.tipo}" } ?: "Produto") },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    if (state.resumo != null) {
                        IconButton(onClick = { menuAberto = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Mais opções")
                        }
                        DropdownMenu(expanded = menuAberto, onDismissRequest = { menuAberto = false }) {
                            DropdownMenuItem(
                                text = { Text("Editar") },
                                leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                                onClick = {
                                    menuAberto = false
                                    state.resumo?.id?.let(onEditar)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Excluir") },
                                leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                                onClick = { menuAberto = false; confirmarExcluir = true },
                            )
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { inner ->
        when {
            state.carregando -> CenterLoading(inner)
            state.resumo == null -> CenterLoading(inner) // poderia exibir "não encontrado"
            else -> Conteudo(
                resumo = state.resumo!!,
                movimentacoes = state.movimentacoes,
                contentPadding = inner,
            )
        }
    }

    if (confirmarExcluir) {
        AlertDialog(
            onDismissRequest = { confirmarExcluir = false },
            title = { Text("Excluir produto?") },
            text = { Text("Esta ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(onClick = { confirmarExcluir = false; viewModel.excluir() }) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmarExcluir = false }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun CenterLoading(inner: PaddingValues) {
    Box(modifier = Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun Conteudo(
    resumo: com.pemotos.lojamanager.domain.model.ProdutoResumoEstoque,
    movimentacoes: List<Movimentacao>,
    contentPadding: PaddingValues,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Linha("Tamanho", resumo.tipo)
                    Linha("Preço de custo", resumo.precoCusto.formatBrl())
                    Linha("Markup", "%.0f%%".format(resumo.markup * 100))
                    Linha("Preço de venda", resumo.precoVenda.formatBrl())
                    Linha("Comprado", "${resumo.qtdComprada}")
                    Linha("Vendido", "${resumo.qtdVendida}")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Estoque atual", style = MaterialTheme.typography.bodyMedium)
                        EstoqueBadge(qtd = resumo.estoqueAtual)
                    }
                    Linha("Valor em estoque (venda)", resumo.valorEmEstoque.formatBrl())
                }
            }
        }
        item {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Histórico de movimentações",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (movimentacoes.isEmpty()) {
            item {
                Text(
                    "Nenhuma movimentação registrada.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(items = movimentacoes, key = { "${it.tipo.name}-${it.data}-${it.origem}-${it.qtd}" }) { mov ->
                MovimentacaoLinha(mov)
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun Linha(rotulo: String, valor: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(rotulo, style = MaterialTheme.typography.bodyMedium)
        Text(valor, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun MovimentacaoLinha(mov: Movimentacao) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    color = if (mov.tipo == TipoMovimentacao.Entrada) StatusOk else StatusDanger,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (mov.tipo == TipoMovimentacao.Entrada) "+" else "−",
                color = androidx.compose.ui.graphics.Color.White,
                fontWeight = FontWeight.Bold,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(mov.origem, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(
                "${mov.data.formatBr()} • ${mov.qtd} × ${mov.precoUnit.formatBrl()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = mov.total.formatBrl(),
            style = MaterialTheme.typography.titleMedium,
            color = if (mov.tipo == TipoMovimentacao.Entrada) StatusOk else StatusDanger,
        )
    }
}
