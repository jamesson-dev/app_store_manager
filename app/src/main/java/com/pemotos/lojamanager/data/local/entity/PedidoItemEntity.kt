package com.pemotos.lojamanager.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pedido_itens",
    foreignKeys = [
        ForeignKey(
            entity = PedidoEntity::class,
            parentColumns = ["id"],
            childColumns = ["pedidoId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ProdutoEntity::class,
            parentColumns = ["id"],
            childColumns = ["produtoId"],
        ),
    ],
    indices = [
        Index(value = ["pedidoId"]),
        Index(value = ["produtoId"]),
    ]
)
data class PedidoItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pedidoId: Long,
    val produtoId: Long,
    val qtd: Int,
    val precoUnitCusto: Double,   // pode diferir do custo "padrão" do produto
)
