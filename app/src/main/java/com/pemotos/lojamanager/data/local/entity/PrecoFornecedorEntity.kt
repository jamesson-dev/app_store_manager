package com.pemotos.lojamanager.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "preco_fornecedor",
    foreignKeys = [
        ForeignKey(
            entity = FornecedorEntity::class,
            parentColumns = ["id"],
            childColumns = ["fornecedorId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ProdutoEntity::class,
            parentColumns = ["id"],
            childColumns = ["produtoId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["fornecedorId", "produtoId"], unique = true),
        Index(value = ["produtoId"]),
    ]
)
data class PrecoFornecedorEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fornecedorId: Long,
    val produtoId: Long,
    val preco: Double,
)
