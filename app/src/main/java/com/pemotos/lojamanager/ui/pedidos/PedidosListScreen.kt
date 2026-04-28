package com.pemotos.lojamanager.ui.pedidos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pemotos.lojamanager.domain.model.PedidoResumo
import com.pemotos.lojamanager.domain.model.StatusPedido
import com.pemotos.lojamanager.ui.format.formatBr
import com.pemotos.lojamanager.ui.format.formatBrl
import com.pemotos.lojamanager.ui.theme.StatusDanger
import com.pemotos.lojamanager.ui.theme.StatusOk
import com.pemotos.lojamanager.ui.theme.StatusWarn

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PedidosListScreen(
    modifier: Modifier = Modifier,
    onNovoPedido: () -> Unit = {},
    onAbrirPedido: (Long) -> Unit = {},
    viewModel: PedidoViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val expandidos = remember { mutableStateMapOf<StatusPedido, Boolean>() }

    Scaffold(
        modifier = modifier,
        topBar = { CenterAlignedTopAppBar(title = { Text("Pedidos") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNovoPedido,
                text = { Text("Novo pedido") },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            )
        },
    ) { inner ->
        if (state.carregando) {
            Box(modifier = Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) {
                Text("Carregando…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (status in StatusPedido.entries) {
                val pedidos = state.porStatus[status].orEmpty()
                val aberto = expandidos[status] ?: (status == StatusPedido.Pendente || status == StatusPedido.Recebido)
                item(key = "header-${status.name}") {
                    StatusHeader(
                        status = status,
                        total = pedidos.size,
                        aberto = aberto,
                        onToggle = { expandidos[status] = !aberto },
                    )
                }
                if (aberto) {
                    if (pedidos.isEmpty()) {
                        item(key = "empty-${status.name}") {
                            Text(
                                text = "Nenhum pedido.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp),
                            )
                        }
                    } else {
                        items(items = pedidos, key = { "p-${it.id}" }) { pedido ->
                            PedidoCard(pedido = pedido, onClick = { onAbrirPedido(pedido.id) })
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun StatusHeader(status: StatusPedido, total: Int, aberto: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier.size(10.dp).background(corDoStatus(status), CircleShape),
        )
        Text(
            text = "${status.label} ($total)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = if (aberto) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
            contentDescription = if (aberto) "Recolher" else "Expandir",
        )
    }
}

@Composable
private fun PedidoCard(pedido: PedidoResumo, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Nº ${pedido.numeroPedido}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = pedido.totalCusto.formatBrl(),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Text(
                text = "${pedido.fornecedorNome} • ${pedido.data.formatBr()}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "${pedido.totalItens} ${if (pedido.totalItens == 1) "item" else "itens"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun corDoStatus(status: StatusPedido): Color = when (status) {
    StatusPedido.Pendente -> StatusWarn
    StatusPedido.Recebido -> StatusOk
    StatusPedido.Cancelado -> StatusDanger
}
