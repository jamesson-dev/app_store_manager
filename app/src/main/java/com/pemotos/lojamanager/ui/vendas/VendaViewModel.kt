package com.pemotos.lojamanager.ui.vendas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pemotos.lojamanager.data.repository.VendaRepository
import com.pemotos.lojamanager.domain.model.FormaPagamento
import com.pemotos.lojamanager.domain.model.VendaDetalhe
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

enum class PeriodoFiltro(val label: String) {
    Hoje("Hoje"),
    Sete("7 dias"),
    Trinta("30 dias"),
    Tudo("Tudo"),
    Personalizado("Personalizado"),
}

data class VendasUiState(
    val carregando: Boolean = true,
    val itens: List<VendaDetalhe> = emptyList(),
    val periodo: PeriodoFiltro = PeriodoFiltro.Sete,
    val inicio: LocalDate = hoje().minus(DatePeriod(days = 6)),
    val fim: LocalDate = hoje(),
    val formasSelecionadas: Set<FormaPagamento> = FormaPagamento.entries.toSet(),
) {
    val totalReceita: Double get() = itens.sumOf { it.totalVenda }
    val totalLucro: Double get() = itens.sumOf { it.lucro }
    val totalPecas: Int get() = itens.sumOf { it.qtd }
}

sealed interface VendaEvento {
    data class Excluida(val mensagem: String) : VendaEvento
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class VendaViewModel @Inject constructor(
    private val repository: VendaRepository,
) : ViewModel() {

    private val periodo = MutableStateFlow(PeriodoFiltro.Sete)
    private val inicio = MutableStateFlow(hoje().minus(DatePeriod(days = 6)))
    private val fim = MutableStateFlow(hoje())
    private val formas = MutableStateFlow(FormaPagamento.entries.toSet())

    private val _eventos = MutableSharedFlow<VendaEvento>(extraBufferCapacity = 1)
    val eventos: SharedFlow<VendaEvento> = _eventos.asSharedFlow()

    private data class Filtros(
        val periodo: PeriodoFiltro,
        val inicio: LocalDate,
        val fim: LocalDate,
        val formas: Set<FormaPagamento>,
    )

    val state: StateFlow<VendasUiState> = combine(
        periodo, inicio, fim, formas,
    ) { p, i, f, formasSel -> Filtros(p, i, f, formasSel) }
        .flatMapLatest { ft ->
            val src = if (ft.periodo == PeriodoFiltro.Tudo) repository.observarTodas()
            else repository.observarPorPeriodo(ft.inicio, ft.fim)
            src.map { vendas ->
                VendasUiState(
                    carregando = false,
                    itens = vendas.filter { FormaPagamento.fromLabel(it.formaPgto) in ft.formas },
                    periodo = ft.periodo,
                    inicio = ft.inicio,
                    fim = ft.fim,
                    formasSelecionadas = ft.formas,
                )
            }
        }
        .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = VendasUiState(),
    )

    fun selecionarPeriodo(p: PeriodoFiltro) {
        periodo.value = p
        when (p) {
            PeriodoFiltro.Hoje -> { inicio.value = hoje(); fim.value = hoje() }
            PeriodoFiltro.Sete -> { inicio.value = hoje().minus(DatePeriod(days = 6)); fim.value = hoje() }
            PeriodoFiltro.Trinta -> { inicio.value = hoje().minus(DatePeriod(days = 29)); fim.value = hoje() }
            PeriodoFiltro.Tudo -> { /* nada — flow troca pra observarTodas */ }
            PeriodoFiltro.Personalizado -> { /* mantém intervalo atual; UI controla */ }
        }
    }

    fun ajustarInicio(novo: LocalDate) {
        inicio.value = novo
        periodo.value = PeriodoFiltro.Personalizado
    }

    fun ajustarFim(novo: LocalDate) {
        fim.value = novo
        periodo.value = PeriodoFiltro.Personalizado
    }

    fun alternarForma(forma: FormaPagamento) {
        val atual = formas.value
        formas.value = if (forma in atual) atual - forma else atual + forma
    }

    fun excluir(id: Long) = viewModelScope.launch {
        try {
            repository.excluir(id)
            _eventos.emit(VendaEvento.Excluida("Venda removida."))
        } catch (t: Throwable) {
            _eventos.emit(VendaEvento.Excluida(t.message ?: "Erro ao excluir."))
        }
    }
}

private fun hoje(): LocalDate =
    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
