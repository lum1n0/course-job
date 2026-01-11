package com.knowledge.base.service

import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.StringField
import org.apache.lucene.document.TextField
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.index.Term
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.store.MMapDirectory
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean

data class LuceneHit(val articleId: Long, val title: String, val score: Float)

@Service
class LuceneService {
    private val analyzer: Analyzer = StandardAnalyzer()

    private val indexPath: Path = Files.createTempDirectory("knowledge_lucene")
    private val directory = MMapDirectory(indexPath)

    private val writer: IndexWriter by lazy {
        val cfg = IndexWriterConfig(analyzer)
        IndexWriter(directory, cfg)
    }

    private val ready = AtomicBoolean(false)

    fun rebuildIndex(docs: List<Map<String, Any>>) {
        writer.deleteAll()
        docs.forEach { addDocumentInternal(it) }
        writer.commit()
        ready.set(true)
    }

    private fun addDocumentInternal(meta: Map<String, Any>) {
        val doc = Document().apply {
            val id = (meta["articleId"] as Long).toString()
            val title = meta["title"] as String
            val body = meta["body"] as String
            add(StringField("articleId", id, Field.Store.YES))
            add(TextField("title", title, Field.Store.YES))
            add(TextField("body", body, Field.Store.NO))
        }
        writer.addDocument(doc)
    }

    fun addDocument(meta: Map<String, Any>) {
        addDocumentInternal(meta)
        writer.commit()
        ready.set(true)
    }

    fun deleteDocument(articleId: Long) {
        writer.deleteDocuments(Term("articleId", articleId.toString()))
        writer.commit()
    }

    fun search(query: String, topK: Int = 10): List<LuceneHit> {
        if (!ready.get() || query.isBlank()) return emptyList()
        DirectoryReader.open(directory).use { reader ->
            val searcher = IndexSearcher(reader)

            val qpTitle = QueryParser("title", analyzer)
            val qpBody = QueryParser("body", analyzer)

            val qTitle = qpTitle.parse(QueryParser.escape(query))
            val qBody = qpBody.parse(QueryParser.escape(query))

            val bq = BooleanQuery.Builder()
                .add(qTitle, BooleanClause.Occur.SHOULD)
                .add(qBody, BooleanClause.Occur.SHOULD)
                .build()

            val topDocs = searcher.search(bq, topK)
            return topDocs.scoreDocs.map { sd ->
                val d = searcher.doc(sd.doc)
                LuceneHit(d.get("articleId").toLong(), d.get("title"), sd.score)
            }
        }
    }
}