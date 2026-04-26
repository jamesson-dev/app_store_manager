package com.pemotos.lojamanager.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.pemotos.lojamanager.data.local.converter.DateConverters
import com.pemotos.lojamanager.data.local.dao.FornecedorDao
import com.pemotos.lojamanager.data.local.dao.PedidoDao
import com.pemotos.lojamanager.data.local.dao.ProdutoDao
import com.pemotos.lojamanager.data.local.dao.VendaDao
import com.pemotos.lojamanager.data.local.entity.FornecedorEntity
import com.pemotos.lojamanager.data.local.entity.PedidoEntity
import com.pemotos.lojamanager.data.local.entity.PedidoItemEntity
import com.pemotos.lojamanager.data.local.entity.PrecoFornecedorEntity
import com.pemotos.lojamanager.data.local.entity.ProdutoEntity
import com.pemotos.lojamanager.data.local.entity.VendaEntity

@Database(
    entities = [
        ProdutoEntity::class,
        FornecedorEntity::class,
        PrecoFornecedorEntity::class,
        PedidoEntity::class,
        PedidoItemEntity::class,
        VendaEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(DateConverters::class)
abstract class LojaDatabase : RoomDatabase() {
    abstract fun produtoDao(): ProdutoDao
    abstract fun fornecedorDao(): FornecedorDao
    abstract fun pedidoDao(): PedidoDao
    abstract fun vendaDao(): VendaDao

    companion object {
        const val NAME = "loja_manager.db"
    }
}
