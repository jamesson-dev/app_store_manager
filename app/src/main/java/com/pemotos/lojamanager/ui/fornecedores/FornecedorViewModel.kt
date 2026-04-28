package com.pemotos.lojamanager.ui.fornecedores

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pemotos.lojamanager.data.local.entity.FornecedorEntity
import com.pemotos.lojamanager.data.local.entity.ProdutoEntity
import com.pemotos.lojamanager.data.repository.EstoqueRepository
import com.pemotos.lojamanager.data.repository.FornecedorRepository
import com.pemotos.lojamanager.domain.model.PrecoMatrizCelula
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FornecedoresUiState(
    val carregando: Boolean = true,
    val fornecedores: List<FornecedorEntity> = emptyList(),
    val produtos: List<ProdutoEntity> = emptyList(),
    val matriz: Map<Pair<Long, Long>, Double> = emptyMap(), // (produtoId, fornecedorId) -> preço
)

sealed interface FornecedorEvento {
    data class Mensagem(val texto: String) : FornecedorEvento
}

@HiltViewModel
class FornecedorViewModel @Inject constructor(
    private val fornecedorRepository: FornecedorRepository,
    private val estoqueRepository: EstoqueRepository,
) : ViewModel() {

    private val _eventos = MutableSharedFlow<FornecedorEvento>(extraBufferCapacity = 1)
    val eventos: SharedFlow<FornecedorEvento> = _eventos.asSharedFlow()

    val state: StateFlow<FornecedoresUiState> = combine(
        fornecedorRepository.observarTodos(),
        estoqueRepository.observarProdutos(),
        fornecedorRepository.observarMatrizPrecos(),
    ) { forns, prods, matriz ->
        FornecedoresUiState(
            carregando = false,
            fornecedores = forns,
            produtos = prods,
            matriz = matriz.associate { (it.produtoId to it.fornecedorId) to (it.preco ?: 0.0) },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FornecedoresUiState(),
    )

    private val _formAberto = MutableStateFlow<FornecedorEntity?>(null)
    val formAberto: StateFlow<FornecedorEntity?> = _formAberto.asStateFlow()

    fun abrirFormulario(fornecedor: FornecedorEntity?) {
        _formAberto.value = fornecedor ?: FornecedorEntity(id = 0, nome = "")
    }

    fun fecharFormulario() { _formAberto.value = null }

    fun salvarFornecedor(nome: String, url: String?) = viewModelScope.launch {
        val nomeTrim = nome.trim()
        val urlTrim = url?.trim()?.takeIf { it.isNotBlank() }
        if (nomeTrim.isBlank()) {
            return@launch _eventos.emit(FornecedorEvento.Mensagem("Informe o nome do fornecedor."))
        }
        try {
            val atual = _formAberto.value
            if (atual == null || atual.id == 0L) {
                fornecedorRepository.inserir(FornecedorEntity(nome = nomeTrim, url = urlTrim))
            } else {
                fornecedorRepository.atualizar(atual.copy(nome = nomeTrim, url = urlTrim))
            }
            _formAberto.value = null
        } catch (t: Throwable) {
            _eventos.emit(FornecedorEvento.Mensagem(t.message ?: "Erro ao salvar."))
        }
    }

    fun excluirFornecedor(fornecedor: FornecedorEntity) = viewModelScope.launch {
        try {
            fornecedorRepository.excluir(fornecedor)
            _eventos.emit(FornecedorEvento.Mensagem("Fornecedor removido."))
        } catch (t: Throwable) {
            _eventos.emit(FornecedorEvento.Mensagem(t.message ?: "Erro ao excluir."))
        }
    }

    fun atualizarPreco(produtoId: Long, fornecedorId: Long, preco: Double?) = viewModelScope.launch {
        fornecedorRepository.upsertPreco(fornecedorId, produtoId, preco)
    }
}
