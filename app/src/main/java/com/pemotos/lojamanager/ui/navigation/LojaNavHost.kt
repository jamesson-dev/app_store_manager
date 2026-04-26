package com.pemotos.lojamanager.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

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
        composable(TopLevelDestination.Estoque.route) { PlaceholderScreen("Estoque") }
        composable(TopLevelDestination.Pedidos.route) { PlaceholderScreen("Pedidos") }
        composable(TopLevelDestination.Vendas.route) { PlaceholderScreen("Vendas") }
        composable(TopLevelDestination.Fornecedores.route) { PlaceholderScreen("Fornecedores") }
    }
}

@Composable
private fun PlaceholderScreen(title: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "$title — em construção")
    }
}
