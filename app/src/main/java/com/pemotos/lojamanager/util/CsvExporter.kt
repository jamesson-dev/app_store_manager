package com.pemotos.lojamanager.util

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.pemotos.lojamanager.data.local.entity.FornecedorEntity
import com.pemotos.lojamanager.data.local.entity.ProdutoEntity
import java.io.File
import java.io.FileOutputStream
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

object CsvExporter {

    /**
     * Exporta a matriz de preços em CSV. Em Android 10+ usa MediaStore (Downloads/);
     * em APIs anteriores cai pra File API direto. Retorna o caminho/uri amigável,
     * ou lança IOException em caso de falha.
     */
    fun exportarMatrizPrecos(
        context: Context,
        produtos: List<ProdutoEntity>,
        fornecedores: List<FornecedorEntity>,
        precos: Map<Pair<Long, Long>, Double>,
    ): String {
        val nomeArquivo = "loja_manager_precos_${dataAtualSlug()}.csv"
        val csv = montarCsv(produtos, fornecedores, precos)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            salvarViaMediaStore(context, nomeArquivo, csv)
        } else {
            @Suppress("DEPRECATION")
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!dir.exists()) dir.mkdirs()
            val arquivo = File(dir, nomeArquivo)
            FileOutputStream(arquivo).use { it.write(csv.toByteArray(Charsets.UTF_8)) }
            arquivo.absolutePath
        }
    }

    private fun salvarViaMediaStore(context: Context, nomeArquivo: String, csv: String): String {
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, nomeArquivo)
            put(MediaStore.Downloads.MIME_TYPE, "text/csv")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(collection, values)
            ?: error("Falha ao criar arquivo no Downloads.")
        try {
            resolver.openOutputStream(uri)?.use { it.write(csv.toByteArray(Charsets.UTF_8)) }
                ?: error("Falha ao abrir output stream do arquivo.")
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        } catch (t: Throwable) {
            resolver.delete(uri, null, null)
            throw t
        }
        return "Downloads/$nomeArquivo"
    }

    private fun montarCsv(
        produtos: List<ProdutoEntity>,
        fornecedores: List<FornecedorEntity>,
        precos: Map<Pair<Long, Long>, Double>,
    ): String {
        // Agrupar produtos por nome (sem duplicar o tipo nas colunas — matriz é por nome).
        val produtosPorNome = produtos.groupBy { it.nome }.toSortedMap()
        val fornsOrdenados = fornecedores.sortedBy { it.nome }
        val sep = ";"
        val sb = StringBuilder()
        // Cabeçalho
        sb.append("Produto").append(sep)
            .append(fornsOrdenados.joinToString(sep) { escapar(it.nome) })
            .append('\n')
        // Linhas — uma por nome de produto. Pegamos o "primeiro" produto-id de cada
        // nome como representante (matriz não distingue tamanho).
        for ((nomeProduto, lista) in produtosPorNome) {
            val representante = lista.first().id
            sb.append(escapar(nomeProduto)).append(sep)
            sb.append(fornsOrdenados.joinToString(sep) { f ->
                val preco = precos[representante to f.id]
                preco?.let { "%.2f".format(it).replace('.', ',') } ?: ""
            })
            sb.append('\n')
        }
        return sb.toString()
    }

    private fun escapar(valor: String): String =
        if (valor.contains(';') || valor.contains(',') || valor.contains('"') || valor.contains('\n')) {
            "\"" + valor.replace("\"", "\"\"") + "\""
        } else valor

    private fun dataAtualSlug(): String {
        val now: LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        return "%04d%02d%02d".format(now.year, now.monthNumber, now.dayOfMonth)
    }
}
