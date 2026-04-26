package com.pemotos.lojamanager.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "produtos",
    indices = [Index(value = ["nome", "tipo"], unique = true)]
)
data class ProdutoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    val tipo: String,            // "S", "P", "M", "G"
    val precoCusto: Double,
    val markup: Double,          // 0.40 = 40%
)
