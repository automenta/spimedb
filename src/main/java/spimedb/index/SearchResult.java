package spimedb.index;

import com.google.common.collect.Iterators;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DocumentStoredFieldVisitor;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TopDocs;
import spimedb.SpimeDB;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;

/**
 * Created by me on 2/3/17.
 */
public final class SearchResult {

    private final TopDocs docs;
    private final IndexSearcher searcher;
    private final org.apache.lucene.search.Query query;

    public SearchResult(org.apache.lucene.search.Query q, IndexSearcher searcher, TopDocs docs) {
        this.query = q;
        this.searcher = searcher;
        this.docs = docs;
        SpimeDB.logger.info("query({}) hits={}", query, docs != null ? docs.totalHits : 0);
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
        try {
            searcher.getIndexReader().close();
        } catch (IOException e) {
            SpimeDB.logger.error("{}", e);
        }
    }

}
