package com.pemotos.lojamanager.data.repository

import com.pemotos.lojamanager.data.local.dao.VendaDao
import com.pemotos.lojamanager.data.local.entity.VendaEntity
import com.pemotos.lojamanager.domain.model.VendaDetalhe
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

@Singleton
class VendaRepository @Inject constructor(
    private val vendaDao: VendaDao,
) {
    fun observarTodas(): Flow<List<VendaDetalhe>> = vendaDao.observarTodas()

    fun observarPorPeriodo(inicio: LocalDate, fim: LocalDate): Flow<List<VendaDetalhe>> =
        vendaDao.observarPorPeriodo(inicio, fim)

    suspend fun obterPorId(id: Long): VendaEntity? = vendaDao.obterPorId(id)

    suspend fun inserir(venda: VendaEntity): Long = vendaDao.inserir(venda)

    suspend fun excluir(id: Long) = vendaDao.excluirPorId(id)
}
