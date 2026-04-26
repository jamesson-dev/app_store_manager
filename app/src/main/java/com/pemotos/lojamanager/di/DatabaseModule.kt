package com.pemotos.lojamanager.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pemotos.lojamanager.data.local.LojaDatabase
import com.pemotos.lojamanager.data.local.dao.FornecedorDao
import com.pemotos.lojamanager.data.local.dao.PedidoDao
import com.pemotos.lojamanager.data.local.dao.ProdutoDao
import com.pemotos.lojamanager.data.local.dao.VendaDao
import com.pemotos.lojamanager.data.local.seed.DatabaseSeeder
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        seederLazy: Lazy<DatabaseSeeder>,
    ): LojaDatabase {
        // O seeder depende do próprio LojaDatabase (Lazy quebra o ciclo).
        val seedScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        return Room.databaseBuilder(
            context.applicationContext,
            LojaDatabase::class.java,
            LojaDatabase.NAME,
        )
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    seedScope.launch { seederLazy.get().seed() }
                }
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    // Garante seed em bancos abertos sem onCreate (apps atualizados, etc.)
                    seedScope.launch { seederLazy.get().seed(forcar = false) }
                }
            })
            .build()
    }

    @Provides fun provideProdutoDao(db: LojaDatabase): ProdutoDao = db.produtoDao()
    @Provides fun provideFornecedorDao(db: LojaDatabase): FornecedorDao = db.fornecedorDao()
    @Provides fun providePedidoDao(db: LojaDatabase): PedidoDao = db.pedidoDao()
    @Provides fun provideVendaDao(db: LojaDatabase): VendaDao = db.vendaDao()
}
