package com.pemotos.lojamanager.ui.fornecedores

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pemotos.lojamanager.data.local.entity.FornecedorEntity
import com.pemotos.lojamanager.util.CsvExporter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FornecedoresScreen(
    modifier: Modifier = Modifier,
    viewModel: FornecedorViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val formAberto by viewModel.formAberto.collectAsStateWithLifecycle()
    var aba by remember { mutableStateOf(0) }
    var confirmarExcluir: FornecedorEntity? by remember { mutableStateOf(null) }
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.eventos.collect { ev ->
            when (ev) {
                is FornecedorEvento.Mensagem -> snackbar.showSnackbar(ev.texto)
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = { CenterAlignedTopAppBar(title = { Text("Fornecedores") }) },
        floatingActionButton = {
            when (aba) {
                0 -> ExtendedFloatingActionButton(
                    onClick = { viewModel.abrirFormulario(null) },
                    text = { Text("Novo fornecedor") },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                )
                1 -> ExtendedFloatingActionButton(
                    onClick = {
                        scope.launch {
                            val msg = try {
                                val caminho = CsvExporter.exportarMatrizPrecos(
                                    context = context,
                                    produtos = state.produtos,
                                    fornecedores = state.fornecedores,
                                    precos = state.matriz,
                                )
                                "CSV exportado para $caminho"
                            } catch (t: Throwable) {
                                "Falha ao exportar: ${t.message ?: t::class.simpleName}"
                            }
                            snackbar.showSnackbar(msg)
                        }
                    },
                    text = { Text("Exportar CSV") },
                    icon = { Icon(Icons.Filled.OpenInBrowser, contentDescription = null) },
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { inner ->
        Column(modifier = Modifier.fillMaxSize().padding(inner)) {
            TabRow(selectedTabIndex = aba) {
                Tab(selected = aba == 0, onClick = { aba = 0 }, text = { Text("Lista") })
                Tab(selected = aba == 1, onClick = { aba = 1 }, text = { Text("Pesquisa de preços") })
            }
            when (aba) {
                0 -> ListaFornecedores(
                    fornecedores = state.fornecedores,
                    onEditar = { viewModel.abrirFormulario(it) },
                    onExcluir = { confirmarExcluir = it },
                    onAbrirUrl = { url ->
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        } catch (_: Throwable) {
                            // sem app pra abrir — silencioso
                        }
                    },
                )
                1 -> MatrizPrecosTela(
                    produtos = state.produtos,
                    fornecedores = state.fornecedores,
                    precos = state.matriz,
                    onAtualizarPreco = viewModel::atualizarPreco,
                )
            }
        }
    }

    formAberto?.let {
        FormularioFornecedorDialog(
            inicial = it,
            onCancelar = viewModel::fecharFormulario,
            onConfirmar = viewModel::salvarFornecedor,
        )
    }

    confirmarExcluir?.let { f ->
        AlertDialog(
            onDismissRequest = { confirmarExcluir = null },
            title = { Text("Excluir ${f.nome}?") },
            text = { Text("A matriz de preços deste fornecedor também será removida.") },
            confirmButton = {
                TextButton(onClick = {
                    val ref = f
                    confirmarExcluir = null
                    viewModel.excluirFornecedor(ref)
                }) { Text("Excluir") }
            },
            dismissButton = {
                TextButton(onClick = { confirmarExcluir = null }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun ListaFornecedores(
    fornecedores: List<FornecedorEntity>,
    onEditar: (FornecedorEntity) -> Unit,
    onExcluir: (FornecedorEntity) -> Unit,
    onAbrirUrl: (String) -> Unit,
) {
    if (fornecedores.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Nenhum fornecedor cadastrado.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items = fornecedores, key = { it.id }) { f ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(f.nome, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        f.url?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                    f.url?.let { url ->
                        IconButton(onClick = { onAbrirUrl(url) }) {
                            Icon(Icons.Filled.OpenInBrowser, contentDescription = "Abrir no navegador")
                        }
                    }
                    IconButton(onClick = { onEditar(f) }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Editar")
                    }
                    IconButton(onClick = { onExcluir(f) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Excluir")
                    }
                }
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun FormularioFornecedorDialog(
    inicial: FornecedorEntity,
    onCancelar: () -> Unit,
    onConfirmar: (nome: String, url: String?) -> Unit,
) {
    var nome by remember(inicial.id) { mutableStateOf(inicial.nome) }
    var url by remember(inicial.id) { mutableStateOf(inicial.url.orEmpty()) }
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(if (inicial.id == 0L) "Novo fornecedor" else "Editar fornecedor") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirmar(nome, url.takeIf { it.isNotBlank() }) }) { Text("Salvar") }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar") }
        },
    )
}
