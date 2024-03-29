package spimedb.index;

import org.apache.commons.lang3.ArrayUtils;
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
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

/** manages and stores a collection process of search results */
public class Search {

    public final static Logger logger = LoggerFactory.getLogger(Search.class);

    public final TopDocs localDocs;
    private final IndexSearcher searcher;
    public final org.apache.lucene.search.Query query;

    @Nullable
    public FacetResult facets = null;

    public final String id;
    private final SpimeDB db;

    /**
     * cached
     */
    transient public final String[] tagsInc;

    public Search(Query q, @NotNull IndexSearcher searcher, SpimeDB db, @Nullable TopDocs docs) {
        this.query = q;
        this.id = SpimeDB.uuidString();
        this.searcher = searcher;
        this.db = db;
        this.localDocs = docs;

        Set<String> tagsInc = new TreeSet<>();
        collectTags(q, tagsInc);
        int ts = tagsInc.size();
        this.tagsInc = (ts > 0) ? tagsInc.toArray(new String[ts]) : ArrayUtils.EMPTY_STRING_ARRAY;

        db.onSearch.accept(this);

        logger.debug("query({}) hits={}", query, docs != null ? docs.totalHits : 0);
    }

    public void setFacets(@Nullable FacetResult facets) {
        this.facets = facets;
    }

    @Nullable public FacetResult facets() {
        return facets;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }


    /** async, object only */
    public void forEachObject(Predicate<NObject> each) {
        forEach((x,ignored)->each.test(x));
    }

    /** async */
    public void forEach(BiPredicate<NObject,ScoreDoc> each) {
        forEach(each, null);
    }

    public void forEach(BiPredicate<NObject,ScoreDoc> each, @Nullable Runnable onFinished) {
        forEach(DObject::get, each, onFinished);
    }

    public <Y> void forEach(Function<Document, Y> f, BiPredicate<Y,ScoreDoc> each, @Nullable Runnable onFinished) {
        forEach(f, each, 0, onFinished);
    }

    /** async  */
    public <Y> void forEach(Function<Document, Y> f, BiPredicate<Y,ScoreDoc> each, long waitMS, @Nullable Runnable onFinished) {

        Thread t = Thread.currentThread();
        AtomicBoolean continuing = new AtomicBoolean(true);

//        Consumer<NObject> recv = (x) -> {
//            if (!each.test(x,null)) {
//                continuing.set(false);
//                t.interrupt();
//            }
//        };
//
//        db.onTag.on(id, recv);
//        for (String x : tagsInc)
//            db.onTag.on(x, recv);

        Throwable ee = null;
        try {
            if (!forEachLocal(f, each))
                return;

            if (continuing.get() && waitMS > 0) {
                try {
                    Thread.sleep(waitMS);
                } catch (InterruptedException ignored) {
                }
            }
        } catch (Exception e) {
            ee = e;
        } finally {

//            for (String x : tagsInc)
//                db.onTag.off(x, recv);
//            db.onTag.off(id, recv);

            try {
                if (onFinished != null)
                    onFinished.run();
            } catch (Throwable e) {
                if (ee!=null)
                    ee = new RuntimeException(ee + " " + e);
                else
                    ee = e;
            }
        }
        if (ee!=null)
            throw new RuntimeException(ee);

    }


    public <Y> boolean forEachLocal(Function<Document,Y> f, BiPredicate<Y, ScoreDoc> each) {
        return forEachLocalDoc((d, s) -> each.test(f.apply(d), s));
    }

    /**
     * return false if canceled via the predicate
     */
    public boolean forEachLocalDoc(BiPredicate<Document, ScoreDoc> each) {
        if (localDocs == null)
            return true;

        IndexReader reader = searcher.getIndexReader();

        DocumentStoredFieldVisitor visitor = new DocumentStoredFieldVisitor();
        Document d = visitor.getDocument();

        boolean result = true;
        for (ScoreDoc x : localDocs.scoreDocs) {
            d.clear();
            try {
                reader.document(x.doc, visitor);
                if (!each.test(d, x)) {
                    result = false;
                    break;
                }
            } catch (IOException e) {
                Search.logger.error("search result error", e);
            }
        }
        close();
        return result;
    }

    /** TODO collect a HashTagSet not only a list */
    static void collectTags(org.apache.lucene.search.Query q, Collection<String> tagsInc) {
        if (q instanceof BooleanQuery bq) {
            bq.forEach(b -> {
                org.apache.lucene.search.Query bqq = b.getQuery();

                if (b.getOccur() == BooleanClause.Occur.MUST_NOT) {
                    //TODO handle negative tags
                } else {

                    if (bqq instanceof DisjunctionMaxQuery dqq) {
                        dqq.forEach(x -> collectTags(x, tagsInc));
                    } else if (bqq instanceof BoostQuery bbq) {
                        collectTags(bbq.getQuery(), tagsInc);
                    } else if (bqq instanceof BooleanQuery) {
                        collectTags(bqq, tagsInc);
                    }
                }


            });
        } else if (q instanceof TermQuery tq) {
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
            logger.error("close", e);
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
