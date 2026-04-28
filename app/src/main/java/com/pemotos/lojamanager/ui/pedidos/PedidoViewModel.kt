package com.pemotos.lojamanager.ui.pedidos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pemotos.lojamanager.data.repository.PedidoRepository
import com.pemotos.lojamanager.domain.model.PedidoResumo
import com.pemotos.lojamanager.domain.model.StatusPedido
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class PedidosUiState(
    val porStatus: Map<StatusPedido, List<PedidoResumo>> = emptyMap(),
    val carregando: Boolean = true,
)

@HiltViewModel
class PedidoViewModel @Inject constructor(
    private val repository: PedidoRepository,
) : ViewModel() {

    val state: StateFlow<PedidosUiState> = repository.observarResumo()
        .map { lista ->
            val grupos = StatusPedido.entries.associateWith { status ->
                lista.filter { it.status == status.label }
            }
            PedidosUiState(porStatus = grupos, carregando = false)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PedidosUiState(),
        )
}
