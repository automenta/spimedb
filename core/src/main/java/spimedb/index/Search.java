package spimedb.index;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.DocumentStoredFieldVisitor;
import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spimedb.NObject;
import spimedb.SpimeDB;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;


public class Search {

    public final static Logger logger = LoggerFactory.getLogger(Search.class);

    private final TopDocs docs;
    @NotNull
    private final IndexSearcher searcher;
    public final org.apache.lucene.search.Query query;
    public final FacetResult facets;
    public final String id;
    private final SpimeDB db;

    /**
     * cached
     */
    transient public final String[] tagsInc;


    public Search(Query q, @NotNull IndexSearcher searcher, SpimeDB db, @Nullable TopDocs docs, FacetResult facetResults) {
        this.query = q;
        this.id = SpimeDB.uuidString();
        this.searcher = searcher;
        this.db = db;
        this.facets = facetResults;
        this.docs = docs;


        List<String> tagsInc = new ArrayList();
        collectTags(q, tagsInc);
        this.tagsInc = tagsInc.toArray(new String[tagsInc.size()]);

        logger.info("query({}) hits={}", query, docs != null ? docs.totalHits : 0);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    /**
     * return false if canceled via the predicate
     */
    public boolean forEach(BiPredicate<DObject, ScoreDoc> each) {
        return forEachLocal((d, s) -> each.test(DObject.get(d), s));
    }

    /**
     * async
     */
    public void forEach(Predicate<NObject> each, long waitMS, Runnable onFinished) {

        Thread t = Thread.currentThread();
        AtomicBoolean continuing = new AtomicBoolean(true);

        Consumer<NObject> recv = (x) -> {
            if (!each.test(x)) {
                continuing.set(false);
                t.interrupt();
            }
        };

        db.onTag.on(id, recv);
        for (String x : tagsInc)
            db.onTag.on(x, recv);

        try {
            if (!forEach((d, s) -> each.test(d))) {
                return;
            }
            if (continuing.get()) {
                try {
                    Thread.sleep(waitMS);
                } catch (InterruptedException ignored) {
                }
            }
        } finally {

            for (String x : tagsInc)
                db.onTag.off(x, recv);
            db.onTag.off(id, recv);

            onFinished.run();
        }

    }


    /**
     * return false if canceled via the predicate
     */
    private boolean forEachLocal(BiPredicate<Document, ScoreDoc> each) {
        if (docs == null)
            return true;

        final DocumentStoredFieldVisitor visitor = new DocumentStoredFieldVisitor();

        IndexReader reader = searcher.getIndexReader();
        Document d = visitor.getDocument();

        boolean result = true;
        for (ScoreDoc x : docs.scoreDocs) {
            d.clear();
            try {
                reader.document(x.doc, visitor);
                if (!each.test(d, x)) {
                    result = false;
                    break;
                }
            } catch (IOException e) {
                Search.logger.error("{}", e.getMessage());
            }
        }
        close();
        return result;
    }

    static void collectTags(org.apache.lucene.search.Query q, Collection<String> tagsInc) {
        if (q instanceof BooleanQuery) {
            BooleanQuery bq = (BooleanQuery) q;
            bq.forEach(b -> {
                org.apache.lucene.search.Query bqq = b.getQuery();

                if (b.getOccur() == BooleanClause.Occur.MUST_NOT) {
                    //TODO handle negative tags
                } else {

                    if (bqq instanceof DisjunctionMaxQuery) {
                        DisjunctionMaxQuery dqq = (DisjunctionMaxQuery) bqq;
                        dqq.forEach(x -> {
                            collectTags(x, tagsInc);
                        });
                    } else if (bqq instanceof BoostQuery) {
                        BoostQuery bbq = (BoostQuery) bqq;
                        collectTags(bbq, tagsInc);

                    } else {
                        throw new UnsupportedOperationException();
                    }
                }


            });
        } else if (q instanceof TermQuery) {
            TermQuery tq = (TermQuery) q;
            Term term = tq.getTerm();
            if (term.field().equals(NObject.TAG)) {
                tagsInc.add(term.text());
            }
        }
    }

//    public Iterator<Document> docs() {
//        if (docs == null)
//            return Collections.emptyIterator();
//
//        final DocumentStoredFieldVisitor visitor = new DocumentStoredFieldVisitor();
//
//        IndexReader reader = searcher.getIndexReader();
//        Document d = visitor.getDocument();
//
//
//        return new SearchResultIterator(d, reader, visitor);
//    }

    private void close() {
        try {
            db.searcherMgr.release(searcher);
        } catch (IOException e) {
            logger.error("{}", e);
        }
    }

//    /**
//     * must be iterated completely for auto-close, or closed manually
//     */
//    private class SearchResultIterator implements Iterator<Document> {
//
//        private final Document d;
//        private final IndexReader reader;
//        private final DocumentStoredFieldVisitor visitor;
//        Iterator<Document> ii;
//
//        public SearchResultIterator(Document d, IndexReader reader, DocumentStoredFieldVisitor visitor) {
//            this.d = d;
//            this.reader = reader;
//            this.visitor = visitor;
//            ii = Iterators.transform(Iterators.forArray(docs.scoreDocs), sd -> {
//                d.clear();
//                try {
//                    reader.document(sd.doc, visitor);
//                } catch (IOException e) {
//                    logger.error("{} {}", sd, e);
//                }
//                return d;
//            });
//        }
//
//        @Override
//        public boolean hasNext() {
//            if (!ii.hasNext()) {
//                close();
//                return false;
//            }
//            return true;
//        }
//
//        @Override
//        public Document next() {
//            return ii.next();
//        }
//    }
}
