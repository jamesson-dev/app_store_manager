package com.pemotos.lojamanager.ui.estoque

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pemotos.lojamanager.data.repository.EstoqueRepository
import com.pemotos.lojamanager.domain.model.Movimentacao
import com.pemotos.lojamanager.domain.model.ProdutoResumoEstoque
import com.pemotos.lojamanager.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DetalheProdutoUiState(
    val carregando: Boolean = true,
    val resumo: ProdutoResumoEstoque? = null,
    val movimentacoes: List<Movimentacao> = emptyList(),
)

sealed interface ExclusaoEvento {
    data object Sucesso : ExclusaoEvento
    data class Bloqueada(val mensagem: String) : ExclusaoEvento
    data class Erro(val mensagem: String) : ExclusaoEvento
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DetalheProdutoViewModel @Inject constructor(
    private val repository: EstoqueRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val produtoId: Long = checkNotNull(savedStateHandle.get<Long>(Routes.ARG_PRODUTO_ID)) {
        "produtoId obrigatório na rota"
    }

    private val _eventos = MutableSharedFlow<ExclusaoEvento>(extraBufferCapacity = 1)
    val eventos = _eventos.asSharedFlow()

    val state: StateFlow<DetalheProdutoUiState> =
        combine(
            repository.observarResumo().map { lista -> lista.firstOrNull { it.id == produtoId } },
            repository.observarMovimentacoes(produtoId),
        ) { resumo, movs ->
            DetalheProdutoUiState(carregando = false, resumo = resumo, movimentacoes = movs)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DetalheProdutoUiState(),
        )

    fun excluir() {
        viewModelScope.launch {
            try {
                val produto = repository.obterPorId(produtoId)
                    ?: return@launch _eventos.emit(ExclusaoEvento.Erro("Produto não encontrado."))
                val ok = repository.excluirSeSemVinculos(produto)
                _eventos.emit(
                    if (ok) ExclusaoEvento.Sucesso
                    else ExclusaoEvento.Bloqueada(
                        "Produto possui pedidos ou vendas vinculados — exclusão bloqueada."
                    )
                )
            } catch (t: Throwable) {
                _eventos.emit(ExclusaoEvento.Erro(t.message ?: "Erro ao excluir."))
            }
        }
    }
}
