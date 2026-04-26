package com.pemotos.lojamanager.ui.estoque

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pemotos.lojamanager.domain.model.TAMANHOS_PRODUTO

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarProdutoScreen(
    onVoltar: () -> Unit,
    viewModel: EditarProdutoViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.salvo) { if (state.salvo) onVoltar() }
    LaunchedEffect(state.erroSalvar) { state.erroSalvar?.let { snackbar.showSnackbar(it) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.ehNovo) "Novo produto" else "Editar produto") },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { inner ->
        if (state.carregando) {
            Column(
                modifier = Modifier.fillMaxSize().padding(inner),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) { CircularProgressIndicator() }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .padding(inner)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = state.nome,
                onValueChange = viewModel::atualizarNome,
                label = { Text("Nome") },
                isError = state.erroNome != null,
                supportingText = { state.erroNome?.let { Text(it) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.tipo,
                onValueChange = viewModel::atualizarTipo,
                label = { Text("Tamanho (ex: P, M, G)") },
                isError = state.erroTipo != null,
                supportingText = { state.erroTipo?.let { Text(it) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TAMANHOS_PRODUTO.forEach { t ->
                    AssistChip(
                        onClick = { viewModel.atualizarTipo(t) },
                        label = { Text(t) },
                        colors = if (state.tipo == t) AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                        ) else AssistChipDefaults.assistChipColors(),
                    )
                }
            }
            OutlinedTextField(
                value = state.precoCustoTexto,
                onValueChange = viewModel::atualizarPrecoCusto,
                label = { Text("Preço de custo (R$)") },
                isError = state.erroPrecoCusto != null,
                supportingText = { state.erroPrecoCusto?.let { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.markupPctTexto,
                onValueChange = viewModel::atualizarMarkup,
                label = { Text("Markup (%)") },
                isError = state.erroMarkup != null,
                supportingText = {
                    if (state.erroMarkup != null) Text(state.erroMarkup!!)
                    else Text("Preço de venda = custo × (1 + markup/100)")
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = viewModel::salvar,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (state.ehNovo) "Cadastrar" else "Salvar alterações") }
        }
    }
}
