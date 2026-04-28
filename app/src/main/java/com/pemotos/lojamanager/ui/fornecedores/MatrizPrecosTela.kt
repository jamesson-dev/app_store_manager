package com.pemotos.lojamanager.ui.fornecedores

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pemotos.lojamanager.data.local.entity.FornecedorEntity
import com.pemotos.lojamanager.data.local.entity.ProdutoEntity
import com.pemotos.lojamanager.ui.format.parseDecimalBr
import com.pemotos.lojamanager.ui.format.toBrTexto
import com.pemotos.lojamanager.ui.theme.StatusOk

private val LARGURA_NOME = 140.dp
private val LARGURA_CELULA = 96.dp
private val ALTURA_CELULA = 44.dp

/**
 * Matriz produto × fornecedor. Linha por NOME de produto (sem distinguir tamanho —
 * matriz é por modelo, ver Seção 2.4 do CLAUDE.md). Toque numa célula abre dialog
 * de edição. Menor preço da linha em verde negrito; vazias em cinza claro.
 */
@Composable
fun MatrizPrecosTela(
    produtos: List<ProdutoEntity>,
    fornecedores: List<FornecedorEntity>,
    precos: Map<Pair<Long, Long>, Double>, // (produtoId, fornecedorId) -> preço
    onAtualizarPreco: (produtoIds: List<Long>, fornecedorId: Long, preco: Double?) -> Unit,
) {
    if (produtos.isEmpty() || fornecedores.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "Cadastre produtos e fornecedores para usar a matriz.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(24.dp),
            )
        }
        return
    }

    // Agrupar produtos por nome — a edição se aplica a todas as variantes.
    val produtosPorNome = remember(produtos) {
        produtos.groupBy { it.nome }.mapValues { (_, variantes) ->
            variantes.sortedBy { it.tipo }
        }
            .toSortedMap()
    }
    val fornsOrdenados = remember(fornecedores) { fornecedores.sortedBy { it.nome } }

    var celulaEditando: Triple<Long, Long, Double?>? by remember { mutableStateOf(null) }
    val scrollHorizontal = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Cabeçalho fixo
        Row(modifier = Modifier.horizontalScroll(scrollHorizontal)) {
            Box(modifier = Modifier.width(LARGURA_NOME).height(ALTURA_CELULA), contentAlignment = Alignment.CenterStart) {
                Text(
                    "Produto",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }
            fornsOrdenados.forEach { f ->
                Box(modifier = Modifier.width(LARGURA_CELULA).height(ALTURA_CELULA), contentAlignment = Alignment.Center) {
                    Text(
                        text = f.nome,
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        maxLines = 2,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }
        }
        HorizontalDivider()
        // Corpo: scroll vertical, mantendo o horizontal sincronizado.
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp),
        ) {
            items(items = produtosPorNome.entries.toList(), key = { it.key }) { (nomeProduto, variantes) ->
                val representante = variantes.first()
                val produtoIds = variantes.map { it.id }
                val precosLinha = fornsOrdenados.map { f -> precos[representante.id to f.id] }
                val menor = precosLinha.filterNotNull().filter { it > 0.0 }.minOrNull()
                Row(modifier = Modifier.horizontalScroll(scrollHorizontal)) {
                    Box(modifier = Modifier.width(LARGURA_NOME).height(ALTURA_CELULA), contentAlignment = Alignment.CenterStart) {
                        Text(nomeProduto, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 12.dp))
                    }
                    fornsOrdenados.forEachIndexed { idx, f ->
                        val preco = precosLinha[idx]
                        CelulaPreco(
                            preco = preco,
                            ehMenor = preco != null && preco == menor,
                            onClick = { celulaEditando = Triple(produtoIds, f.id, preco) },
                        )
                    }
                }
                HorizontalDivider()
            }
        }
    }

    celulaEditando?.let { (produtoIds, fornecedorId, atual) ->
        EditarPrecoDialog(
            valorInicial = atual,
            onCancelar = { celulaEditando = null },
            onConfirmar = { novoPreco ->
                onAtualizarPreco(produtoIds, fornecedorId, novoPreco)
                celulaEditando = null
            },
            onLimpar = {
                onAtualizarPreco(produtoIds, fornecedorId, null)
                celulaEditando = null
            },
        )
    }
}

@Composable
private fun CelulaPreco(preco: Double?, ehMenor: Boolean, onClick: () -> Unit) {
    val texto = preco?.let { "R$ ${it.toBrTexto()}" } ?: "—"
    Box(
        modifier = Modifier
            .width(LARGURA_CELULA)
            .height(ALTURA_CELULA)
            .clickable(onClick = onClick)
            .background(
                color = if (ehMenor) StatusOk.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(4.dp),
            )
            .padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (ehMenor) FontWeight.Bold else FontWeight.Normal,
            color = when {
                ehMenor -> StatusOk
                preco == null -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                else -> MaterialTheme.colorScheme.onSurface
            },
        )
    }
}

@Composable
private fun EditarPrecoDialog(
    valorInicial: Double?,
    onCancelar: () -> Unit,
    onConfirmar: (preco: Double) -> Unit,
    onLimpar: () -> Unit,
) {
    var texto by remember { mutableStateOf(valorInicial?.toBrTexto().orEmpty()) }
    var erro by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Editar preço") },
        text = {
            OutlinedTextField(
                value = texto,
                onValueChange = { texto = it; erro = null },
                label = { Text("Preço (R$)") },
                singleLine = true,
                isError = erro != null,
                supportingText = { erro?.let { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            Button(onClick = {
                val parsed = texto.parseDecimalBr()
                if (parsed == null || parsed <= 0.0) {
                    erro = "Informe um preço válido."
                } else {
                    onConfirmar(parsed)
                }
            }) { Text("Salvar") }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (valorInicial != null) {
                    TextButton(onClick = onLimpar) { Text("Remover") }
                }
                TextButton(onClick = onCancelar) { Text("Cancelar") }
            }
        },
    )
}
