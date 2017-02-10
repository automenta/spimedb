package spimedb.index;

import com.google.common.collect.Iterators;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DocumentStoredFieldVisitor;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spimedb.SpimeDB;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.function.BiConsumer;

/**
 * Created by me on 2/3/17.
 */
public final class SearchResult {

    public final static Logger logger = LoggerFactory.getLogger(SearchResult.class);

    private final TopDocs docs;
    private final IndexSearcher searcher;
    private final org.apache.lucene.search.Query query;

    public SearchResult(org.apache.lucene.search.Query q, IndexSearcher searcher, TopDocs docs) {
        this.query = q;
        this.searcher = searcher;
        this.docs = docs;
        SpimeDB.logger.info("query({}) hits={}", query, docs != null ? docs.totalHits : 0);
    }

    public void forEach(BiConsumer<Document,ScoreDoc> each) {
        final DocumentStoredFieldVisitor visitor = new DocumentStoredFieldVisitor();

        IndexReader reader = searcher.getIndexReader();
        Document d = visitor.getDocument();

        for ( ScoreDoc x : docs.scoreDocs) {
            d.clear();
            try {
                reader.document(x.doc, visitor);
                each.accept(d, x);
            } catch (IOException e) {
                SearchResult.logger.error("{}", e.getMessage());
            }
        }
    }

    public Iterator<Document> docs() {
        if (docs == null)
            return Collections.emptyIterator();

        final DocumentStoredFieldVisitor visitor = new DocumentStoredFieldVisitor();

        IndexReader reader = searcher.getIndexReader();
        Document d = visitor.getDocument();

        return Iterators.transform(Iterators.forArray(docs.scoreDocs), sd -> {
            d.clear();
            try {
                reader.document(sd.doc, visitor);
            } catch (IOException e) {
                SpimeDB.logger.error("{} {}", sd, e);
            }
            return d;
        });
    }

    public void close() {
        if (searcher == null)
            return;

        try {

            searcher.getIndexReader().close();
        } catch (IOException e) {
            SpimeDB.logger.error("{}", e);
        }
    }

}
