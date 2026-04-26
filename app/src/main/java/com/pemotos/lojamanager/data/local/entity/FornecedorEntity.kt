package com.pemotos.lojamanager.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "fornecedores",
    indices = [Index(value = ["nome"], unique = true)]
)
data class FornecedorEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    val url: String? = null,
)
