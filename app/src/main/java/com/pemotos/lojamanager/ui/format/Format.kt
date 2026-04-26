package com.pemotos.lojamanager.ui.format

import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate

private val LOCALE_BR = Locale("pt", "BR")

private val MOEDA: NumberFormat = NumberFormat.getCurrencyInstance(LOCALE_BR)
private val DATA_BR: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", LOCALE_BR)

fun Double.formatBrl(): String = MOEDA.format(this)
fun Int.formatPecas(): String = NumberFormat.getIntegerInstance(LOCALE_BR).format(this)
fun LocalDate.formatBr(): String = toJavaLocalDate().format(DATA_BR)
