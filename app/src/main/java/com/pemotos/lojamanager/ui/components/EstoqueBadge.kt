package com.pemotos.lojamanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pemotos.lojamanager.ui.format.formatPecas
import com.pemotos.lojamanager.ui.theme.StatusDanger
import com.pemotos.lojamanager.ui.theme.StatusOk
import com.pemotos.lojamanager.ui.theme.StatusWarn

/** Cor do badge de estoque conforme regra da Seção 6.2: verde >5, amarelo 1..5, vermelho 0. */
fun corEstoque(qtd: Int): Color = when {
    qtd <= 0 -> StatusDanger
    qtd <= 5 -> StatusWarn
    else -> StatusOk
}

@Composable
fun EstoqueBadge(qtd: Int, modifier: Modifier = Modifier) {
    Text(
        text = qtd.formatPecas(),
        style = MaterialTheme.typography.labelLarge,
        color = Color.White,
        modifier = modifier
            .background(color = corEstoque(qtd), shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}
