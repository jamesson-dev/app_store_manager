package com.pemotos.lojamanager.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.pemotos.lojamanager.ui.estoque.DetalheProdutoScreen
import com.pemotos.lojamanager.ui.estoque.EditarProdutoScreen
import com.pemotos.lojamanager.ui.estoque.EstoqueScreen
import com.pemotos.lojamanager.ui.pedidos.EditarPedidoScreen
import com.pemotos.lojamanager.ui.pedidos.PedidosListScreen
import com.pemotos.lojamanager.ui.fornecedores.FornecedoresScreen
import com.pemotos.lojamanager.ui.vendas.NovaVendaScreen
import com.pemotos.lojamanager.ui.vendas.VendasListScreen

@Composable
fun LojaNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = TopLevelDestination.START_ROUTE,
        modifier = modifier,
    ) {
        composable(TopLevelDestination.Painel.route) { PlaceholderScreen("Painel") }

        composable(TopLevelDestination.Estoque.route) {
            EstoqueScreen(
                onAbrirProduto = { id -> navController.navigate(Routes.produtoDetalhe(id)) },
                onNovoProduto = { navController.navigate(Routes.PRODUTO_NOVO) },
            )
        }

        composable(Routes.PRODUTO_NOVO) {
            EditarProdutoScreen(onVoltar = { navController.popBackStack() })
        }

        composable(
            route = Routes.PRODUTO_DETALHE,
            arguments = listOf(navArgument(Routes.ARG_PRODUTO_ID) { type = NavType.LongType }),
        ) {
            DetalheProdutoScreen(
                onVoltar = { navController.popBackStack() },
                onEditar = { id -> navController.navigate(Routes.produtoEditar(id)) },
            )
        }

        composable(
            route = Routes.PRODUTO_EDITAR,
            arguments = listOf(navArgument(Routes.ARG_PRODUTO_ID) { type = NavType.LongType }),
        ) {
            EditarProdutoScreen(onVoltar = { navController.popBackStack() })
        }

        composable(TopLevelDestination.Pedidos.route) {
            PedidosListScreen(
                onNovoPedido = { navController.navigate(Routes.PEDIDO_NOVO) },
                onAbrirPedido = { id -> navController.navigate(Routes.pedidoEditar(id)) },
            )
        }

        composable(Routes.PEDIDO_NOVO) {
            EditarPedidoScreen(onVoltar = { navController.popBackStack() })
        }

        composable(
            route = Routes.PEDIDO_EDITAR,
            arguments = listOf(navArgument(Routes.ARG_PEDIDO_ID) { type = NavType.LongType }),
        ) {
            EditarPedidoScreen(onVoltar = { navController.popBackStack() })
        }

        composable(TopLevelDestination.Vendas.route) {
            VendasListScreen(onNovaVenda = { navController.navigate(Routes.VENDA_NOVA) })
        }
        composable(Routes.VENDA_NOVA) {
            NovaVendaScreen(onVoltar = { navController.popBackStack() })
        }

        composable(TopLevelDestination.Fornecedores.route) { FornecedoresScreen() }
    }
}

@Composable
private fun PlaceholderScreen(title: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "$title — em construção")
    }
}
