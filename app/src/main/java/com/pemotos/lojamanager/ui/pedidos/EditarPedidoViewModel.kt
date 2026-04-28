package com.pemotos.lojamanager.ui.pedidos

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pemotos.lojamanager.data.local.entity.FornecedorEntity
import com.pemotos.lojamanager.data.local.entity.PedidoEntity
import com.pemotos.lojamanager.data.local.entity.PedidoItemEntity
import com.pemotos.lojamanager.data.local.entity.ProdutoEntity
import com.pemotos.lojamanager.data.repository.EstoqueRepository
import com.pemotos.lojamanager.data.repository.FornecedorRepository
import com.pemotos.lojamanager.data.repository.PedidoRepository
import com.pemotos.lojamanager.domain.model.StatusPedido
import com.pemotos.lojamanager.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class ItemPedidoEditavel(
    val produtoId: Long,
    val produtoNome: String,
    val produtoTipo: String,
    val qtd: Int,
    val precoUnitCusto: Double,
) {
    val total: Double get() = qtd * precoUnitCusto
}

data class EditarPedidoUiState(
    val carregando: Boolean = true,
    val ehNovo: Boolean = true,
    val numeroPedido: Int = 0,
    val data: LocalDate = hoje(),
    val fornecedorId: Long? = null,
    val status: StatusPedido = StatusPedido.Pendente,
    val observacao: String = "",
    val itens: List<ItemPedidoEditavel> = emptyList(),
    val fornecedores: List<FornecedorEntity> = emptyList(),
    val produtosDisponiveis: List<ProdutoEntity> = emptyList(),
    val erro: String? = null,
    val salvo: Boolean = false,
) {
    val totalCusto: Double get() = itens.sumOf { it.total }
    val totalQtd: Int get() = itens.sumOf { it.qtd }
}

@HiltViewModel
class EditarPedidoViewModel @Inject constructor(
    private val pedidoRepository: PedidoRepository,
    private val fornecedorRepository: FornecedorRepository,
    private val estoqueRepository: EstoqueRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val pedidoId: Long? = savedStateHandle.get<Long>(Routes.ARG_PEDIDO_ID)
        ?.takeIf { it > 0L }

    private val _state = MutableStateFlow(EditarPedidoUiState(ehNovo = pedidoId == null))
    val state: StateFlow<EditarPedidoUiState> = _state.asStateFlow()

    init { carregar() }

    private fun carregar() {
        viewModelScope.launch {
            val fornecedores = fornecedorRepository.observarTodos().first()
            val produtos = estoqueRepository.observarProdutos().first()
            if (pedidoId == null) {
                _state.update {
                    it.copy(
                        carregando = false,
                        fornecedores = fornecedores,
                        produtosDisponiveis = produtos,
                        numeroPedido = pedidoRepository.proximoNumeroPedido(),
                        fornecedorId = fornecedores.firstOrNull()?.id,
                    )
                }
            } else {
                val pedido = pedidoRepository.obterPorId(pedidoId)
                    ?: return@launch _state.update {
                        it.copy(carregando = false, erro = "Pedido não encontrado.")
                    }
                val itensEntity = pedidoRepository.obterItens(pedidoId)
                val porId = produtos.associateBy { it.id }
                val itens = itensEntity.map { item ->
                    val produto = porId[item.produtoId]
                    ItemPedidoEditavel(
                        produtoId = item.produtoId,
                        produtoNome = produto?.nome.orEmpty(),
                        produtoTipo = produto?.tipo.orEmpty(),
                        qtd = item.qtd,
                        precoUnitCusto = item.precoUnitCusto,
                    )
                }
                _state.update {
                    it.copy(
                        carregando = false,
                        ehNovo = false,
                        fornecedores = fornecedores,
                        produtosDisponiveis = produtos,
                        numeroPedido = pedido.numeroPedido,
                        data = pedido.data,
                        fornecedorId = pedido.fornecedorId,
                        status = StatusPedido.fromLabel(pedido.status),
                        observacao = pedido.observacao.orEmpty(),
                        itens = itens,
                    )
                }
            }
        }
    }

    fun atualizarNumero(novo: Int) = _state.update { it.copy(numeroPedido = novo, erro = null) }
    fun atualizarData(novo: LocalDate) = _state.update { it.copy(data = novo, erro = null) }
    fun atualizarFornecedor(id: Long) = _state.update { it.copy(fornecedorId = id, erro = null) }
    fun atualizarStatus(novo: StatusPedido) = _state.update { it.copy(status = novo, erro = null) }
    fun atualizarObservacao(novo: String) = _state.update { it.copy(observacao = novo) }

    fun adicionarItem(produtoId: Long, qtd: Int, precoUnitCusto: Double) {
        val produto = _state.value.produtosDisponiveis.firstOrNull { it.id == produtoId } ?: return
        val novo = ItemPedidoEditavel(
            produtoId = produto.id,
            produtoNome = produto.nome,
            produtoTipo = produto.tipo,
            qtd = qtd,
            precoUnitCusto = precoUnitCusto,
        )
        _state.update { current ->
            // se já existir item desse produto, soma qtd e mantém último preço
            val idxExistente = current.itens.indexOfFirst { it.produtoId == produtoId }
            val itens = if (idxExistente >= 0) {
                current.itens.toMutableList().also {
                    val antigo = it[idxExistente]
                    it[idxExistente] = antigo.copy(qtd = antigo.qtd + qtd, precoUnitCusto = precoUnitCusto)
                }
            } else current.itens + novo
            current.copy(itens = itens, erro = null)
        }
    }

    fun atualizarItem(index: Int, qtd: Int, precoUnitCusto: Double) {
        _state.update { current ->
            val itens = current.itens.toMutableList()
            if (index !in itens.indices) return@update current
            itens[index] = itens[index].copy(qtd = qtd, precoUnitCusto = precoUnitCusto)
            current.copy(itens = itens)
        }
    }

    fun removerItem(index: Int) = _state.update { current ->
        if (index !in current.itens.indices) return@update current
        current.copy(itens = current.itens.toMutableList().also { it.removeAt(index) })
    }

    fun salvar() {
        val s = _state.value
        if (s.numeroPedido <= 0) {
            return _state.update { it.copy(erro = "Número do pedido inválido.") }
        }
        val fornecedorId = s.fornecedorId
            ?: return _state.update { it.copy(erro = "Selecione um fornecedor.") }
        if (s.itens.isEmpty()) {
            return _state.update { it.copy(erro = "Adicione pelo menos um item.") }
        }
        viewModelScope.launch {
            try {
                val itens = s.itens.map { i ->
                    PedidoItemEntity(
                        pedidoId = pedidoId ?: 0L,
                        produtoId = i.produtoId,
                        qtd = i.qtd,
                        precoUnitCusto = i.precoUnitCusto,
                    )
                }
                if (pedidoId == null) {
                    pedidoRepository.criar(
                        pedido = PedidoEntity(
                            numeroPedido = s.numeroPedido,
                            data = s.data,
                            fornecedorId = fornecedorId,
                            status = s.status.label,
                            observacao = s.observacao.takeIf { it.isNotBlank() },
                        ),
                        itens = itens,
                    )
                } else {
                    pedidoRepository.atualizar(
                        pedido = PedidoEntity(
                            id = pedidoId,
                            numeroPedido = s.numeroPedido,
                            data = s.data,
                            fornecedorId = fornecedorId,
                            status = s.status.label,
                            observacao = s.observacao.takeIf { it.isNotBlank() },
                        ),
                        itens = itens,
                    )
                }
                _state.update { it.copy(salvo = true) }
            } catch (t: Throwable) {
                _state.update { it.copy(erro = t.message ?: "Erro ao salvar pedido.") }
            }
        }
    }

    fun excluir() {
        val id = pedidoId ?: return
        viewModelScope.launch {
            try {
                val pedido = pedidoRepository.obterPorId(id) ?: return@launch
                pedidoRepository.excluir(pedido)
                _state.update { it.copy(salvo = true) }
            } catch (t: Throwable) {
                _state.update { it.copy(erro = t.message ?: "Erro ao excluir pedido.") }
            }
        }
    }
}

private fun hoje(): LocalDate =
    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
