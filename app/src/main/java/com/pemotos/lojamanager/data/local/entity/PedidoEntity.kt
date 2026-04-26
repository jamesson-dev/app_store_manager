package com.pemotos.lojamanager.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate

@Entity(
    tableName = "pedidos",
    foreignKeys = [
        ForeignKey(
            entity = FornecedorEntity::class,
            parentColumns = ["id"],
            childColumns = ["fornecedorId"],
        ),
    ],
    indices = [
        Index(value = ["fornecedorId"]),
        Index(value = ["status"]),
    ]
)
data class PedidoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val numeroPedido: Int,        // exibido como "Nº Pedido"
    val data: LocalDate,
    val fornecedorId: Long,
    val status: String,           // "Pendente", "Recebido", "Cancelado"
    val observacao: String? = null,
)
