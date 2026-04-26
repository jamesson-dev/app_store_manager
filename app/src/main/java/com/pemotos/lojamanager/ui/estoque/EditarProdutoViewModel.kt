package com.pemotos.lojamanager.ui.estoque

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pemotos.lojamanager.data.local.entity.ProdutoEntity
import com.pemotos.lojamanager.data.repository.EstoqueRepository
import com.pemotos.lojamanager.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditarProdutoUiState(
    val carregando: Boolean = true,
    val nome: String = "",
    val tipo: String = "",
    val precoCustoTexto: String = "",
    val markupPctTexto: String = "40",   // markup default 40%
    val erroNome: String? = null,
    val erroTipo: String? = null,
    val erroPrecoCusto: String? = null,
    val erroMarkup: String? = null,
    val erroSalvar: String? = null,
    val salvo: Boolean = false,
    val ehNovo: Boolean = true,
)

@HiltViewModel
class EditarProdutoViewModel @Inject constructor(
    private val repository: EstoqueRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val produtoId: Long? = savedStateHandle.get<Long>(Routes.ARG_PRODUTO_ID)
        ?.takeIf { it > 0L }

    private val _state = MutableStateFlow(EditarProdutoUiState(ehNovo = produtoId == null))
    val state: StateFlow<EditarProdutoUiState> = _state.asStateFlow()

    init { carregar() }

    private fun carregar() {
        if (produtoId == null) {
            _state.update { it.copy(carregando = false) }
            return
        }
        viewModelScope.launch {
            val produto = repository.obterPorId(produtoId)
            if (produto == null) {
                _state.update { it.copy(carregando = false, erroSalvar = "Produto não encontrado.") }
                return@launch
            }
            _state.update {
                it.copy(
                    carregando = false,
                    nome = produto.nome,
                    tipo = produto.tipo,
                    precoCustoTexto = produto.precoCusto.toBrTexto(),
                    markupPctTexto = (produto.markup * 100).toBrTexto(),
                    ehNovo = false,
                )
            }
        }
    }

    fun atualizarNome(novo: String) = _state.update { it.copy(nome = novo, erroNome = null, erroSalvar = null) }
    fun atualizarTipo(novo: String) = _state.update { it.copy(tipo = novo.uppercase(), erroTipo = null, erroSalvar = null) }
    fun atualizarPrecoCusto(novo: String) = _state.update { it.copy(precoCustoTexto = novo, erroPrecoCusto = null, erroSalvar = null) }
    fun atualizarMarkup(novo: String) = _state.update { it.copy(markupPctTexto = novo, erroMarkup = null, erroSalvar = null) }

    fun salvar() {
        val atual = _state.value
        val nome = atual.nome.trim()
        val tipo = atual.tipo.trim().uppercase()
        val custo = atual.precoCustoTexto.trim().parseDecimalBr()
        val markupPct = atual.markupPctTexto.trim().parseDecimalBr()

        var ok = true
        if (nome.isBlank()) { _state.update { it.copy(erroNome = "Informe o nome.") }; ok = false }
        if (tipo.isBlank()) { _state.update { it.copy(erroTipo = "Informe o tamanho.") }; ok = false }
        if (custo == null || custo <= 0.0) { _state.update { it.copy(erroPrecoCusto = "Preço de custo inválido.") }; ok = false }
        if (markupPct == null || markupPct < 0.0) { _state.update { it.copy(erroMarkup = "Markup inválido.") }; ok = false }
        if (!ok) return

        val markup = markupPct!! / 100.0
        viewModelScope.launch {
            try {
                if (produtoId == null) {
                    val existente = repository.obterPorNomeTipo(nome, tipo)
                    if (existente != null) {
                        _state.update { it.copy(erroSalvar = "Já existe um produto com esse nome e tamanho.") }
                        return@launch
                    }
                    repository.inserir(
                        ProdutoEntity(nome = nome, tipo = tipo, precoCusto = custo!!, markup = markup)
                    )
                } else {
                    val atualEntity = repository.obterPorId(produtoId)
                        ?: return@launch _state.update { it.copy(erroSalvar = "Produto não encontrado.") }
                    repository.atualizar(
                        atualEntity.copy(nome = nome, tipo = tipo, precoCusto = custo!!, markup = markup)
                    )
                }
                _state.update { it.copy(salvo = true) }
            } catch (t: Throwable) {
                _state.update { it.copy(erroSalvar = t.message ?: "Erro ao salvar.") }
            }
        }
    }
}

private fun Double.toBrTexto(): String =
    if (this == this.toLong().toDouble()) this.toLong().toString()
    else "%.2f".format(this).replace('.', ',')

private fun String.parseDecimalBr(): Double? =
    if (isBlank()) null
    else replace(".", "").replace(',', '.').toDoubleOrNull()
