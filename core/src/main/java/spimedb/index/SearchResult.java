package spimedb.index;

import com.google.common.collect.Iterators;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DocumentStoredFieldVisitor;
import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.function.BiPredicate;

/**
 * Created by me on 2/3/17.
 */
public final class SearchResult {

    public final static Logger logger = LoggerFactory.getLogger(SearchResult.class);

    private final TopDocs docs;
    private final IndexSearcher searcher;
    private final org.apache.lucene.search.Query query;
    public final FacetResult facets;

    public SearchResult(Query q, IndexSearcher searcher, @Nullable TopDocs docs, FacetResult facetResults) {
        this.query = q;
        this.searcher = searcher;
        this.facets = facetResults;
        this.docs = docs;
        logger.info("query({}) hits={}", query, docs != null ? docs.totalHits : 0);
    }

    public void forEach(BiPredicate<DObject, ScoreDoc> each) {
        forEachDocument((d, s) -> each.test(DObject.get(d), s));
    }

    public void forEachDocument(BiPredicate<Document, ScoreDoc> each) {
        if (docs == null)
            return;

        final DocumentStoredFieldVisitor visitor = new DocumentStoredFieldVisitor();

        IndexReader reader = searcher.getIndexReader();
        Document d = visitor.getDocument();

        for (ScoreDoc x : docs.scoreDocs) {
            d.clear();
            try {
                reader.document(x.doc, visitor);
                if (!each.test(d, x))
                    break;
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


        return new SearchResultIterator(d, reader, visitor);
    }

    public void close() {
        synchronized (query) {
            if (searcher == null)
                return;

            try {

                searcher.getIndexReader().close();
            } catch (IOException e) {
                logger.error("{}", e);
            }
        }
    }

    /** must be iterated completely for auto-close, or closed manually */
    private class SearchResultIterator implements Iterator<Document> {

        private final Document d;
        private final IndexReader reader;
        private final DocumentStoredFieldVisitor visitor;
        Iterator<Document> ii;

        public SearchResultIterator(Document d, IndexReader reader, DocumentStoredFieldVisitor visitor) {
            this.d = d;
            this.reader = reader;
            this.visitor = visitor;
            ii = Iterators.transform(Iterators.forArray(docs.scoreDocs), sd -> {
                d.clear();
                try {
                    reader.document(sd.doc, visitor);
                } catch (IOException e) {
                    logger.error("{} {}", sd, e);
                }
                return d;
            });
        }

        @Override
        public boolean hasNext() {
            if (!ii.hasNext()) {
                close();
                return false;
            }
            return true;
        }

        @Override
        public Document next() {
            return ii.next();
        }
    }
}
