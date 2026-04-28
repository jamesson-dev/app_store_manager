package com.pemotos.lojamanager.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.pemotos.lojamanager.ui.format.formatBr
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

/**
 * OutlinedTextField clicável que abre um DatePicker e devolve LocalDate selecionado.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    label: String,
    valor: LocalDate,
    onChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    var aberto by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }

    LaunchedEffect(interaction) {
        interaction.interactions.collect {
            if (it is PressInteraction.Release) aberto = true
        }
    }

    OutlinedTextField(
        value = valor.formatBr(),
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        trailingIcon = { Icon(Icons.Filled.CalendarToday, contentDescription = "Selecionar data") },
        modifier = modifier,
        interactionSource = interaction,
    )

    if (aberto) {
        val initialMillis = valor.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { aberto = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val d = Instant.fromEpochMilliseconds(millis)
                            .toLocalDateTime(TimeZone.UTC).date
                        onChange(d)
                    }
                    aberto = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { aberto = false }) { Text("Cancelar") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}
