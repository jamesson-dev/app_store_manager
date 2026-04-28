package com.pemotos.lojamanager.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/**
 * Dropdown padrão Material 3 — escolha única em uma lista de opções tipadas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SingleSelectDropdown(
    label: String,
    options: List<T>,
    selecionado: T?,
    rotuloDe: (T) -> String,
    onChange: (T) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Selecione",
) {
    var expandido by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expandido,
        onExpandedChange = { expandido = !expandido },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = selecionado?.let(rotuloDe).orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable, true)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expandido,
            onDismissRequest = { expandido = false },
        ) {
            options.forEach { opcao ->
                DropdownMenuItem(
                    text = { Text(rotuloDe(opcao)) },
                    onClick = {
                        onChange(opcao)
                        expandido = false
                    },
                )
            }
        }
    }
}
