package com.pemotos.lojamanager.domain.model

/** Valores possíveis para [com.pemotos.lojamanager.data.local.entity.VendaEntity.formaPgto]. */
enum class FormaPagamento(val label: String) {
    Pix("Pix"),
    Dinheiro("Dinheiro"),
    Cartao("Cartão"),
    Fiado("Fiado");

    companion object {
        fun fromLabel(label: String): FormaPagamento =
            entries.firstOrNull { it.label == label }
                ?: error("Forma de pagamento inválida: $label")
    }
}
