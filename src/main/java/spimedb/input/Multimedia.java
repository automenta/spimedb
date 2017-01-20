package spimedb.input;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.RecursiveParserWrapper;
import org.apache.tika.sax.BasicContentHandlerFactory;
import org.apache.tika.sax.ContentHandlerFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.helpers.DefaultHandler;
import spimedb.MutableNObject;
import spimedb.SpimeDB;
import spimedb.plan.AbstractGoal;
import spimedb.plan.Goal;

import java.io.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 *
 * Detects document and multimedia metadata, and schedules further processing
 *
 * https://svn.apache.org/repos/asf/tika/trunk/tika-example/src/main/java/org/apache/tika/example/LuceneIndexerExtended.java
 * https://svn.apache.org/repos/asf/tika/trunk/tika-example/src/main/java/org/apache/tika/example/SimpleTextExtractor.java
 * https://github.com/apache/pdfbox/tree/trunk/examples/src/main/java/org/apache/pdfbox/examples
 */
public class Multimedia extends AbstractGoal<SpimeDB> {


    public final static Logger logger = LoggerFactory.getLogger(Multimedia.class);

    private static final int BUFFER_SIZE = 1024 * 128;


    static final Parser p = new AutoDetectParser();
    static final ContentHandlerFactory factory = new BasicContentHandlerFactory(
            BasicContentHandlerFactory.HANDLER_TYPE.HTML, -1);

    private final Supplier<InputStream> stream;
    private final String uri;

    public Multimedia(File f) {
        this(f.toURI().toString(), () -> {
            try {
                return new FileInputStream(f);
            } catch (FileNotFoundException e) {
                return null;
            }
        });
    }

    public Multimedia(String uri, Supplier<InputStream> stream) {
        super(uri);
        this.uri = uri;
        this.stream = stream;
    }

    //    public static void fromURL(String url, Consumer<Multimedia> with) throws IOException {
//        //this(url, new URL(url).openStream());
//        new HTTP().asStream(url, s -> {
//            with.accept( new Multimedia(url, s) );
//        });
//    }


    @NotNull
    @Override
    public void DO(@NotNull SpimeDB db, Consumer<Iterable<Goal<? super SpimeDB>>> next) throws RuntimeException {


        RecursiveParserWrapper wrapper = new RecursiveParserWrapper(p, factory);
        Metadata metadata = new Metadata();
        //metadata.set(Metadata.RESOURCE_NAME_KEY, "test_recursive_embedded.docx");
        ParseContext context = new ParseContext();

        final MutableNObject resource = new MutableNObject(uri);

        try {
            InputStream stream = this.stream.get();
            if (stream == null) {
                throw new FileNotFoundException();
            }

            wrapper.parse(new BufferedInputStream(stream, BUFFER_SIZE), new DefaultHandler(), metadata, context);


            List<Metadata> m = wrapper.getMetadata();
            m.forEach(d -> {
                for (String k : d.names()) {
                    String[] v = d.getValues(k);

                    String kk = meta(k);
                    if (kk != null)
                        resource.put(kk, v.length > 1 ? v : v[0]);
                }
            });

        } catch (Exception e) {
            resource.put("ParseException", e);
        }

        int pages = Integer.parseInt(resource.getOr("pages", "-1"));
        if ( pages > 0) {

        }

        //System.out.println(JSON.toJSONString(resource));

        try {
            //new XML(meta.get("_").toString());
            Element body = Jsoup.parse(resource.get("_").toString()).body();
            System.out.println(
                    body.toString()
            );
        } catch (Exception e) {
            e.printStackTrace();
        }

        db.put(resource);

        if ("application/pdf".equals(resource.get("Content-Type"))) {
            //spawn PDF thumbnail generation
            next.accept( IntStream.range(0, pages-1).mapToObj( page ->
                new PDF.ToImage( uri, stream, page )
            ).collect(Collectors.toList()) );
        }
    }

    //"Title" -> "N"
    //"X-TIKA:content" -> "_"

    @Nullable
    static String meta(String k) {

        String m;
        switch (k) {
            case "Title": m = "N"; break;
            case "X-TIKA:content": m = "_"; break;

            case "xmpTPg:NPages": m = "pages"; break;

            case "pdf:docinfo:title": m = null; break; //duplicates "Title"
            //TODO other duplcates

            default: m = k; break;
        }
        return m;
    }

    public static void main(String[] args) {
        SpimeDB db = new SpimeDB().resources("/tmp/eadoc");

        db.goal(
            new FileDirectory("/home/me/d/eadoc", Multimedia::new)
        );

        db.sync(1000 * 60);

        db.printState(System.out);
        db.obj.forEach((k,v)->System.out.println(v));
    }

    /*
        private Tika tika;

    private IndexWriter writer;

    public MetadataAwareLuceneIndexer(IndexWriter writer, Tika tika) {
        this.writer = writer;
        this.tika = tika;
    }

    public void indexContentSpecificMet(File file) throws Exception {
        Metadata met = new Metadata();
        try (InputStream is = new FileInputStream(file)) {
            tika.parse(is, met);
            Document document = new Document();
            for (String key : met.names()) {
                String[] values = met.getValues(key);
                for (String val : values) {
                    document.add(new Field(key, val, Store.YES, Index.ANALYZED));
                }
                writer.addDocument(document);
            }
        }
    }

    public void indexWithDublinCore(File file) throws Exception {
        Metadata met = new Metadata();
        met.add(Metadata.CREATOR, "Manning");
        met.add(Metadata.CREATOR, "Tika in Action");
        met.set(Metadata.DATE, new Date());
        met.set(Metadata.FORMAT, tika.detect(file));
        met.set(DublinCore.SOURCE, file.toURI().toURL().toString());
        met.add(Metadata.SUBJECT, "File");
        met.add(Metadata.SUBJECT, "Indexing");
        met.add(Metadata.SUBJECT, "Metadata");
        met.set(Property.externalClosedChoise(Metadata.RIGHTS, "public",
                "private"), "public");
        try (InputStream is = new FileInputStream(file)) {
            tika.parse(is, met);
            Document document = new Document();
            for (String key : met.names()) {
                String[] values = met.getValues(key);
                for (String val : values) {
                    document.add(new Field(key, val, Store.YES, Index.ANALYZED));
                }
                writer.addDocument(document);
            }
        }
    }
     */
}
