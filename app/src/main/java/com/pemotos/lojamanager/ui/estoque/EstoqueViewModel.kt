package com.pemotos.lojamanager.ui.estoque

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pemotos.lojamanager.data.repository.EstoqueRepository
import com.pemotos.lojamanager.domain.model.ProdutoResumoEstoque
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class EstoqueUiState(
    val itens: List<ProdutoResumoEstoque> = emptyList(),
    val busca: String = "",
    val carregando: Boolean = true,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class EstoqueViewModel @Inject constructor(
    private val repository: EstoqueRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val busca = MutableStateFlow(savedStateHandle.get<String>(KEY_BUSCA).orEmpty())

    val uiState: StateFlow<EstoqueUiState> =
        combine(repository.observarResumo(), busca) { itens, termo ->
            val filtrados = if (termo.isBlank()) itens
            else itens.filter { it.nome.contains(termo, ignoreCase = true) || it.tipo.contains(termo, ignoreCase = true) }
            EstoqueUiState(itens = filtrados, busca = termo, carregando = false)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = EstoqueUiState(),
        )

    val buscaState: StateFlow<String> = busca.asStateFlow()

    fun atualizarBusca(novo: String) {
        busca.value = novo
    }

    private companion object {
        const val KEY_BUSCA = "busca"
    }
}
