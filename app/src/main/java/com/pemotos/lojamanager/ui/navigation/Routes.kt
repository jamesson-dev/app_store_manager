package com.pemotos.lojamanager.ui.navigation

/** Rotas do NavHost (parametrizadas por id quando aplicável). */
object Routes {
    const val ARG_PRODUTO_ID = "produtoId"
    const val ARG_PEDIDO_ID = "pedidoId"

    const val PRODUTO_NOVO = "produto/novo"
    const val PRODUTO_DETALHE = "produto/{${ARG_PRODUTO_ID}}"
    const val PRODUTO_EDITAR = "produto/{${ARG_PRODUTO_ID}}/editar"

    fun produtoDetalhe(id: Long) = "produto/$id"
    fun produtoEditar(id: Long) = "produto/$id/editar"

    const val PEDIDO_NOVO = "pedido/novo"
    const val PEDIDO_EDITAR = "pedido/{${ARG_PEDIDO_ID}}/editar"
    fun pedidoEditar(id: Long) = "pedido/$id/editar"

    const val VENDA_NOVA = "venda/nova"
}
