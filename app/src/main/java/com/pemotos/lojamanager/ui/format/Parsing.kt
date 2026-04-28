package com.pemotos.lojamanager.ui.format

/** Parse de número decimal aceitando formato pt-BR (vírgula) ou en-US (ponto). */
fun String.parseDecimalBr(): Double? =
    if (isBlank()) null
    else trim().replace(".", "").replace(',', '.').toDoubleOrNull()

fun String.parseIntPositivo(): Int? = trim().toIntOrNull()?.takeIf { it > 0 }

fun Double.toBrTexto(): String =
    if (this == this.toLong().toDouble()) this.toLong().toString()
    else "%.2f".format(this).replace('.', ',')
