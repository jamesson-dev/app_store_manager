package com.pemotos.lojamanager.ui.pedidos

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pemotos.lojamanager.data.local.entity.FornecedorEntity
import com.pemotos.lojamanager.data.local.entity.ProdutoEntity
import com.pemotos.lojamanager.domain.model.StatusPedido
import com.pemotos.lojamanager.ui.components.DateField
import com.pemotos.lojamanager.ui.components.SingleSelectDropdown
import com.pemotos.lojamanager.ui.format.formatBrl
import com.pemotos.lojamanager.ui.format.parseDecimalBr
import com.pemotos.lojamanager.ui.format.parseIntPositivo
import com.pemotos.lojamanager.ui.format.toBrTexto
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarPedidoScreen(
    onVoltar: () -> Unit,
    viewModel: EditarPedidoViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var menuAberto by remember { mutableStateOf(false) }
    var confirmarExcluir by remember { mutableStateOf(false) }
    var bottomSheetVisivel by remember { mutableStateOf(false) }
    var itemEditando: Int? by remember { mutableStateOf(null) }

    LaunchedEffect(state.salvo) { if (state.salvo) onVoltar() }
    LaunchedEffect(state.erro) { state.erro?.let { snackbar.showSnackbar(it) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.ehNovo) "Novo pedido" else "Pedido nº ${state.numeroPedido}") },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    if (!state.ehNovo) {
                        IconButton(onClick = { menuAberto = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Excluir")
                        }
                        DropdownMenu(expanded = menuAberto, onDismissRequest = { menuAberto = false }) {
                            DropdownMenuItem(
                                text = { Text("Excluir pedido") },
                                onClick = { menuAberto = false; confirmarExcluir = true },
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { itemEditando = null; bottomSheetVisivel = true },
                text = { Text("Adicionar item") },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            )
        },
        bottomBar = { TotalRodape(totalCusto = state.totalCusto, totalQtd = state.totalQtd, onSalvar = viewModel::salvar) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { inner ->
        if (state.carregando) {
            Box(modifier = Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier.fillMaxSize().padding(inner),
        ) {
            Cabecalho(
                state = state,
                onAtualizarNumero = viewModel::atualizarNumero,
                onAtualizarData = viewModel::atualizarData,
                onAtualizarFornecedor = viewModel::atualizarFornecedor,
                onAtualizarStatus = viewModel::atualizarStatus,
                onAtualizarObservacao = viewModel::atualizarObservacao,
            )
            HorizontalDivider()
            Text(
                text = "Itens",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            ItensLista(
                itens = state.itens,
                onClickItem = { idx -> itemEditando = idx; bottomSheetVisivel = true },
                onRemover = viewModel::removerItem,
            )
        }
    }

    if (bottomSheetVisivel) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val scope = rememberCoroutineScope()
        ModalBottomSheet(
            onDismissRequest = { bottomSheetVisivel = false },
            sheetState = sheetState,
        ) {
            ItemForm(
                produtosDisponiveis = state.produtosDisponiveis,
                inicial = itemEditando?.let { state.itens.getOrNull(it) },
                onConfirmar = { produtoId, qtd, preco ->
                    val idx = itemEditando
                    if (idx != null) viewModel.atualizarItem(idx, qtd, preco)
                    else viewModel.adicionarItem(produtoId, qtd, preco)
                    scope.launch { sheetState.hide() }.invokeOnCompletion { bottomSheetVisivel = false }
                },
                onCancelar = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion { bottomSheetVisivel = false }
                },
            )
        }
    }

    if (confirmarExcluir) {
        AlertDialog(
            onDismissRequest = { confirmarExcluir = false },
            title = { Text("Excluir pedido?") },
            text = { Text("Esta ação remove o pedido e todos os itens. Não pode ser desfeita.") },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Cabecalho(
    state: EditarPedidoUiState,
    onAtualizarNumero: (Int) -> Unit,
    onAtualizarData: (kotlinx.datetime.LocalDate) -> Unit,
    onAtualizarFornecedor: (Long) -> Unit,
    onAtualizarStatus: (StatusPedido) -> Unit,
    onAtualizarObservacao: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = state.numeroPedido.takeIf { it > 0 }?.toString().orEmpty(),
                onValueChange = { onAtualizarNumero(it.toIntOrNull() ?: 0) },
                label = { Text("Nº pedido") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            DateField(
                label = "Data",
                valor = state.data,
                onChange = onAtualizarData,
                modifier = Modifier.weight(1.4f),
            )
        }
        SingleSelectDropdown(
            label = "Fornecedor",
            options = state.fornecedores,
            selecionado = state.fornecedores.firstOrNull { it.id == state.fornecedorId },
            rotuloDe = FornecedorEntity::nome,
            onChange = { onAtualizarFornecedor(it.id) },
            modifier = Modifier.fillMaxWidth(),
        )
        SegmentedStatus(state.status, onAtualizarStatus)
        OutlinedTextField(
            value = state.observacao,
            onValueChange = onAtualizarObservacao,
            label = { Text("Observação (opcional)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 1,
            maxLines = 3,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SegmentedStatus(atual: StatusPedido, onChange: (StatusPedido) -> Unit) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        StatusPedido.entries.forEachIndexed { idx, st ->
            SegmentedButton(
                selected = st == atual,
                onClick = { onChange(st) },
                shape = SegmentedButtonDefaults.itemShape(idx, StatusPedido.entries.size),
                label = { Text(st.label) },
            )
        }
    }
}

@Composable
private fun ItensLista(
    itens: List<ItemPedidoEditavel>,
    onClickItem: (Int) -> Unit,
    onRemover: (Int) -> Unit,
) {
    if (itens.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            Text(
                "Nenhum item adicionado.\nUse o botão + para adicionar.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(itens, key = { _, it -> it.produtoId }) { idx, item ->
            Card(
                onClick = { onClickItem(idx) },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "${item.produtoNome} (${item.produtoTipo})",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            "${item.qtd} × ${item.precoUnitCusto.formatBrl()} = ${item.total.formatBrl()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { onRemover(idx) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Remover")
                    }
                }
            }
        }
        item { Spacer(Modifier.height(160.dp)) } // respiro pro FAB e bottom bar
    }
}

@Composable
private fun TotalRodape(totalCusto: Double, totalQtd: Int, onSalvar: () -> Unit) {
    androidx.compose.material3.Surface(
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Total custo", style = MaterialTheme.typography.bodySmall)
                Text(
                    totalCusto.formatBrl(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text("$totalQtd ${if (totalQtd == 1) "peça" else "peças"}", style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = onSalvar) { Text("Salvar pedido") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemForm(
    produtosDisponiveis: List<ProdutoEntity>,
    inicial: ItemPedidoEditavel?,
    onConfirmar: (produtoId: Long, qtd: Int, precoUnit: Double) -> Unit,
    onCancelar: () -> Unit,
) {
    var produto by remember(inicial) {
        mutableStateOf(produtosDisponiveis.firstOrNull { it.id == inicial?.produtoId })
    }
    var qtdTexto by remember(inicial) { mutableStateOf(inicial?.qtd?.toString() ?: "") }
    var precoTexto by remember(inicial) {
        mutableStateOf(inicial?.precoUnitCusto?.toBrTexto() ?: "")
    }
    var erroProduto: String? by remember { mutableStateOf(null) }
    var erroQtd: String? by remember { mutableStateOf(null) }
    var erroPreco: String? by remember { mutableStateOf(null) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = if (inicial == null) "Adicionar item" else "Editar item",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        SingleSelectDropdown(
            label = "Produto",
            options = produtosDisponiveis,
            selecionado = produto,
            rotuloDe = { "${it.nome} (${it.tipo})" },
            onChange = { p ->
                produto = p
                if (precoTexto.isBlank()) precoTexto = p.precoCusto.toBrTexto()
                erroProduto = null
            },
            modifier = Modifier.fillMaxWidth(),
        )
        erroProduto?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = qtdTexto,
                onValueChange = { qtdTexto = it; erroQtd = null },
                label = { Text("Qtd") },
                isError = erroQtd != null,
                supportingText = { erroQtd?.let { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = precoTexto,
                onValueChange = { precoTexto = it; erroPreco = null },
                label = { Text("Preço unit (R$)") },
                isError = erroPreco != null,
                supportingText = { erroPreco?.let { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1.5f),
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onCancelar) { Text("Cancelar") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                val p = produto
                val qtd = qtdTexto.parseIntPositivo()
                val preco = precoTexto.parseDecimalBr()
                var ok = true
                if (p == null) { erroProduto = "Selecione um produto."; ok = false }
                if (qtd == null) { erroQtd = "Qtd inválida."; ok = false }
                if (preco == null || preco <= 0.0) { erroPreco = "Preço inválido."; ok = false }
                if (ok) onConfirmar(p!!.id, qtd!!, preco!!)
            }) { Text("Confirmar") }
        }
        Spacer(Modifier.height(8.dp))
    }
}

