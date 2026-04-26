package com.pemotos.lojamanager.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate

@Entity(
    tableName = "vendas",
    foreignKeys = [
        ForeignKey(
            entity = ProdutoEntity::class,
            parentColumns = ["id"],
            childColumns = ["produtoId"],
        ),
    ],
    indices = [
        Index(value = ["produtoId"]),
        Index(value = ["data"]),
    ]
)
data class VendaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val data: LocalDate,
    val produtoId: Long,
    val qtd: Int,
    val precoUnit: Double,        // congelado no momento da venda
    val formaPgto: String,        // "Pix", "Dinheiro", "Cartão", "Fiado"
    val cliente: String? = null,
    val telefone: String? = null,
)
