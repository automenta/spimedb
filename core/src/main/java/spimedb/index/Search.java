package spimedb.index;

import com.google.common.collect.Iterators;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DocumentStoredFieldVisitor;
import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spimedb.SpimeDB;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.function.BiPredicate;


public class Search {

    public final static Logger logger = LoggerFactory.getLogger(Search.class);

    private final TopDocs docs;
    @NotNull private final IndexSearcher searcher;
    public final org.apache.lucene.search.Query query;
    public final FacetResult facets;
    private final String id;
    private final SearcherManager searcherMgr;


    public Search(Query q, @NotNull IndexSearcher searcher, SearcherManager searchMgr, @Nullable TopDocs docs, FacetResult facetResults) {
        this.query = q;
        this.id = SpimeDB.uuidString();
        this.searcher = searcher;
        this.searcherMgr = searchMgr;
        this.facets = facetResults;
        this.docs = docs;
        logger.info("query({}) hits={}", query, docs != null ? docs.totalHits : 0);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
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
                Search.logger.error("{}", e.getMessage());
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
        try {
            searcherMgr.release(searcher);
        } catch (IOException e) {
            logger.error("{}", e);
        }
    }

    /**
     * must be iterated completely for auto-close, or closed manually
     */
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
