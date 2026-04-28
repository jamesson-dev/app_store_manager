package com.pemotos.lojamanager.ui.vendas

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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pemotos.lojamanager.domain.model.FormaPagamento
import com.pemotos.lojamanager.domain.model.ProdutoResumoEstoque
import com.pemotos.lojamanager.ui.components.DateField
import com.pemotos.lojamanager.ui.components.SingleSelectDropdown
import com.pemotos.lojamanager.ui.format.formatBrl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovaVendaScreen(
    onVoltar: () -> Unit,
    viewModel: NovaVendaViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.eventos.collect { ev ->
            when (ev) {
                is NovaVendaEvento.Salva -> {
                    val result = snackbar.showSnackbar(
                        message = "Venda registrada — ${ev.total.formatBrl()}",
                        actionLabel = "Desfazer",
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.desfazer(ev.id)
                    }
                    onVoltar()
                }
            }
        }
    }
    LaunchedEffect(state.erroSalvar) { state.erroSalvar?.let { snackbar.showSnackbar(it) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nova venda") },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DateField(
                label = "Data",
                valor = state.data,
                onChange = viewModel::atualizarData,
                modifier = Modifier.fillMaxWidth(),
            )
            SingleSelectDropdown(
                label = "Produto (apenas com estoque > 0)",
                options = state.produtosDisponiveis,
                selecionado = state.produtoSelecionado,
                rotuloDe = { p: ProdutoResumoEstoque -> "${p.nome} (${p.tipo}) — estoque ${p.estoqueAtual}" },
                onChange = viewModel::selecionarProduto,
                modifier = Modifier.fillMaxWidth(),
                placeholder = if (state.produtosDisponiveis.isEmpty())
                    "Sem produtos com estoque" else "Selecione",
            )
            state.erroProduto?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = state.qtdTexto,
                    onValueChange = viewModel::atualizarQtd,
                    label = { Text("Qtd") },
                    isError = state.erroQtd != null,
                    supportingText = { state.erroQtd?.let { Text(it) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = state.precoTexto,
                    onValueChange = viewModel::atualizarPreco,
                    label = { Text("Preço unit (R$)") },
                    isError = state.erroPreco != null,
                    supportingText = { state.erroPreco?.let { Text(it) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1.5f),
                )
            }
            Text(
                "Total: ${state.totalCalculado.formatBrl()}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )

            FormasPagamentoRadios(
                selecionada = state.forma,
                onChange = viewModel::atualizarForma,
            )

            if (state.forma == FormaPagamento.Fiado) {
                OutlinedTextField(
                    value = state.cliente,
                    onValueChange = viewModel::atualizarCliente,
                    label = { Text("Cliente") },
                    isError = state.erroCliente != null,
                    supportingText = { state.erroCliente?.let { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.telefone,
                    onValueChange = viewModel::atualizarTelefone,
                    label = { Text("Telefone") },
                    isError = state.erroTelefone != null,
                    supportingText = { state.erroTelefone?.let { Text(it) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = viewModel::salvar,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.produtosDisponiveis.isNotEmpty(),
            ) { Text("Salvar venda") }
        }
    }
}

@Composable
private fun FormasPagamentoRadios(
    selecionada: FormaPagamento,
    onChange: (FormaPagamento) -> Unit,
) {
    Column {
        Text("Forma de pagamento", style = MaterialTheme.typography.titleSmall)
        FormaPagamento.entries.forEach { f ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = f == selecionada, onClick = { onChange(f) })
                Text(f.label)
            }
        }
    }
}
