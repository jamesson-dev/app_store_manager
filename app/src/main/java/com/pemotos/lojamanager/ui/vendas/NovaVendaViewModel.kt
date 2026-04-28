package com.pemotos.lojamanager.ui.vendas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pemotos.lojamanager.data.local.entity.VendaEntity
import com.pemotos.lojamanager.data.repository.EstoqueRepository
import com.pemotos.lojamanager.data.repository.VendaRepository
import com.pemotos.lojamanager.domain.model.FormaPagamento
import com.pemotos.lojamanager.domain.model.ProdutoResumoEstoque
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class NovaVendaUiState(
    val carregando: Boolean = true,
    val produtosDisponiveis: List<ProdutoResumoEstoque> = emptyList(),
    val produtoSelecionado: ProdutoResumoEstoque? = null,
    val data: LocalDate = hoje(),
    val qtdTexto: String = "1",
    val precoTexto: String = "",
    val forma: FormaPagamento = FormaPagamento.Pix,
    val cliente: String = "",
    val telefone: String = "",
    val erroProduto: String? = null,
    val erroQtd: String? = null,
    val erroPreco: String? = null,
    val erroCliente: String? = null,
    val erroTelefone: String? = null,
    val erroSalvar: String? = null,
    val totalCalculado: Double = 0.0,
)

sealed interface NovaVendaEvento {
    data class Salva(val id: Long, val total: Double) : NovaVendaEvento
}

@HiltViewModel
class NovaVendaViewModel @Inject constructor(
    private val vendaRepository: VendaRepository,
    private val estoqueRepository: EstoqueRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(NovaVendaUiState())
    val state: StateFlow<NovaVendaUiState> = _state.asStateFlow()

    private val _eventos = MutableSharedFlow<NovaVendaEvento>(extraBufferCapacity = 1)
    val eventos: SharedFlow<NovaVendaEvento> = _eventos.asSharedFlow()

    init {
        viewModelScope.launch {
            val resumos = estoqueRepository.observarResumo().first()
                .filter { it.estoqueAtual > 0 }
            _state.update { it.copy(carregando = false, produtosDisponiveis = resumos) }
        }
    }

    fun selecionarProduto(p: ProdutoResumoEstoque) = _state.update { current ->
        current.copy(
            produtoSelecionado = p,
            precoTexto = if (current.precoTexto.isBlank()) p.precoVenda.toBrTexto() else current.precoTexto,
            erroProduto = null,
        ).recalcularTotal()
    }

    fun atualizarData(novo: LocalDate) = _state.update { it.copy(data = novo) }

    fun atualizarQtd(novo: String) = _state.update { it.copy(qtdTexto = novo, erroQtd = null).recalcularTotal() }

    fun atualizarPreco(novo: String) = _state.update { it.copy(precoTexto = novo, erroPreco = null).recalcularTotal() }

    fun atualizarForma(f: FormaPagamento) = _state.update { it.copy(forma = f, erroCliente = null, erroTelefone = null) }

    fun atualizarCliente(novo: String) = _state.update { it.copy(cliente = novo, erroCliente = null) }

    fun atualizarTelefone(novo: String) = _state.update { it.copy(telefone = novo, erroTelefone = null) }

    fun salvar() {
        val s = _state.value
        val p = s.produtoSelecionado ?: return _state.update { it.copy(erroProduto = "Selecione um produto.") }
        val qtd = s.qtdTexto.trim().toIntOrNull()?.takeIf { it > 0 }
            ?: return _state.update { it.copy(erroQtd = "Quantidade inválida.") }
        if (qtd > p.estoqueAtual) {
            return _state.update {
                it.copy(erroQtd = "Estoque insuficiente. Disponível: ${p.estoqueAtual}.")
            }
        }
        val preco = s.precoTexto.parseDecimalBr()?.takeIf { it > 0.0 }
            ?: return _state.update { it.copy(erroPreco = "Preço inválido.") }
        if (s.forma == FormaPagamento.Fiado) {
            if (s.cliente.isBlank()) {
                return _state.update { it.copy(erroCliente = "Obrigatório para Fiado.") }
            }
            if (s.telefone.isBlank()) {
                return _state.update { it.copy(erroTelefone = "Obrigatório para Fiado.") }
            }
        }
        viewModelScope.launch {
            try {
                val id = vendaRepository.inserir(
                    VendaEntity(
                        data = s.data,
                        produtoId = p.id,
                        qtd = qtd,
                        precoUnit = preco,
                        formaPgto = s.forma.label,
                        cliente = s.cliente.takeIf { it.isNotBlank() },
                        telefone = s.telefone.takeIf { it.isNotBlank() },
                    )
                )
                _eventos.emit(NovaVendaEvento.Salva(id = id, total = qtd * preco))
            } catch (t: Throwable) {
                _state.update { it.copy(erroSalvar = t.message ?: "Erro ao salvar venda.") }
            }
        }
    }

    fun desfazer(id: Long) = viewModelScope.launch {
        vendaRepository.excluir(id)
    }
}

private fun NovaVendaUiState.recalcularTotal(): NovaVendaUiState {
    val qtd = qtdTexto.trim().toIntOrNull() ?: 0
    val preco = precoTexto.parseDecimalBr() ?: 0.0
    return copy(totalCalculado = qtd * preco)
}

private fun String.parseDecimalBr(): Double? =
    if (isBlank()) null else trim().replace(".", "").replace(',', '.').toDoubleOrNull()

private fun Double.toBrTexto(): String =
    if (this == this.toLong().toDouble()) this.toLong().toString()
    else "%.2f".format(this).replace('.', ',')

private fun hoje(): LocalDate =
    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
