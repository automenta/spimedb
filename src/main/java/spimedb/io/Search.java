package spimedb.io;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.RAMDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spimedb.NObject;
import spimedb.SpimeDB;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

/**
 * Lucene free-text search plugin
 */
public class Search implements BiConsumer<NObject, SpimeDB>, Function<String, Document> {

    public final static Logger logger = LoggerFactory.getLogger(Search.class);

    private final RAMDirectory dir;
    private final Set<String> out = new ConcurrentSkipListSet<>();
    private final AtomicBoolean writing = new AtomicBoolean(false);
    private final SpimeDB db;
    private final StandardAnalyzer analyzer;

    public Search(SpimeDB db) {
        this.db = db;

        this.dir = new RAMDirectory();
        analyzer = new StandardAnalyzer();

        db.on(this);
    }

    private void commit(String id) {
        if (out.add(id)) {
            commit();
        }
    }

    static <X> Iterable<X> drain(Iterable<X> x) {
        return () -> new Iterator<X>() {

            final Iterator<X> ii = x.iterator();

            @Override
            public boolean hasNext() {
                return ii.hasNext();
            }

            @Override
            public X next() {
                X n = ii.next();
                ii.remove();
                return n;
            }
        };
    }

    private void commit() {
        if (writing.compareAndSet(false, true)) {
            db.exe.execute( () -> {
                try {
                    IndexWriterConfig writerConf = new IndexWriterConfig(analyzer);
                    writerConf.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
                    writerConf.setRAMBufferSizeMB(256.0);

                    int s = out.size();
                    IndexWriter writer = new IndexWriter(dir, writerConf);
                    long seq = writer.addDocuments(Iterables.transform(drain(out), this));
                    writer.commit();
                    writer.close();
                    logger.info("{} indexed", s);

                } catch (IOException e) {
                    logger.error("indexing error: {}", e);
                }
                writing.set(false);
            });
        }

    }

    @Override
    public void accept(NObject x, SpimeDB db) {
        commit(x.id());
    }

    @Override
    public Document apply(String nid) {
        NObject n = db.get(nid);
        Document d = new Document();
        d.add(new StringField("I", nid, Field.Store.YES));
        n.forEach((k,v)->{
            d.add(new StringField(k, v.toString(), Field.Store.YES));
        });
        return d;
    }
}
