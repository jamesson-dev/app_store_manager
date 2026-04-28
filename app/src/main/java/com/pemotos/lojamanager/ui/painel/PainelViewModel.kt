package com.pemotos.lojamanager.ui.painel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pemotos.lojamanager.data.repository.PainelRepository
import com.pemotos.lojamanager.domain.model.PainelKpis
import com.pemotos.lojamanager.domain.model.ProdutoResumoEstoque
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.LocalDate

data class PainelUiState(
    val carregando: Boolean = true,
    val kpis: PainelKpis = PainelKpis(0, 0.0, 0.0, 0.0, 0.0, 0),
    val topBaixoEstoque: List<ProdutoResumoEstoque> = emptyList(),
    val vendasUltimosDias: List<Pair<LocalDate, Double>> = emptyList(),
)

@HiltViewModel
class PainelViewModel @Inject constructor(
    private val repository: PainelRepository,
) : ViewModel() {

    val state: StateFlow<PainelUiState> = combine(
        repository.observarKpis(),
        repository.observarTopBaixoEstoque(limite = 5, maxQtd = 2),
        repository.observarVendasPorDia(dias = 7),
    ) { kpis, top, vendas ->
        PainelUiState(
            carregando = false,
            kpis = kpis,
            topBaixoEstoque = top,
            vendasUltimosDias = vendas,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PainelUiState(),
    )
}
