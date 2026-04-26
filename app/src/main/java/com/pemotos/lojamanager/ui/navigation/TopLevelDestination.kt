package com.pemotos.lojamanager.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.ui.graphics.vector.ImageVector

enum class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    Painel("painel", "Painel", Icons.Filled.Dashboard),
    Estoque("estoque", "Estoque", Icons.Filled.Inventory2),
    Pedidos("pedidos", "Pedidos", Icons.Filled.LocalShipping),
    Vendas("vendas", "Vendas", Icons.Filled.PointOfSale),
    Fornecedores("fornecedores", "Fornecedores", Icons.Filled.Storefront);

    companion object {
        const val START_ROUTE = "painel"
    }
}
