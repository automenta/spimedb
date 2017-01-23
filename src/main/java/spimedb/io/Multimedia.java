package spimedb.io;

import ch.qos.logback.classic.Level;
import com.google.common.collect.Iterables;
import com.rometools.rome.io.impl.Base64;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.RecursiveParserWrapper;
import org.apache.tika.sax.BasicContentHandlerFactory;
import org.apache.tika.sax.ContentHandlerFactory;
import org.eclipse.collections.impl.block.factory.Comparators;
import org.jetbrains.annotations.Nullable;
import org.jpedal.jbig2.jai.JBIG2ImageReaderSpi;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.helpers.DefaultHandler;
import spimedb.MutableNObject;
import spimedb.NObject;
import spimedb.SpimeDB;
import spimedb.util.JSON;

import javax.imageio.spi.IIORegistry;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.TreeSet;

/**
 * Detects document and multimedia metadata, and schedules further processing
 * <p>
 * https://svn.apache.org/repos/asf/tika/trunk/tika-example/src/main/java/org/apache/tika/example/LuceneIndexerExtended.java
 * https://svn.apache.org/repos/asf/tika/trunk/tika-example/src/main/java/org/apache/tika/example/SimpleTextExtractor.java
 * https://github.com/apache/pdfbox/tree/trunk/examples/src/main/java/org/apache/pdfbox/examples
 */
public class Multimedia  {


    public final static Logger logger = LoggerFactory.getLogger(Multimedia.class);

    private static final int BUFFER_SIZE = 1024 * 128;





    //    public static void fromURL(String url, Consumer<Multimedia> with) throws IOException {
//        //this(url, new URL(url).openStream());
//        new HTTP().asStream(url, s -> {
//            with.accept( new Multimedia(url, s) );
//        });
//    }



    //"Title" -> "N"
    //"X-TIKA:content" -> "_"

    @Nullable
    static String tikiToField(String k) {

        String m;
        switch (k) {
            case "Title":
                m = "N";
                break;
            case "X-TIKA:content":
                m = "body";
                break;
            case "Author":
                m = "author";
                break;
            case "Content-Type":
                m = "contentType";
                break;
            case "xmpTPg:NPages":
                m = "pageCount";
                break;

            case "pdf:docinfo:title":
                m = null;
                break; //duplicates "Title"
            //TODO other duplcates

            default:
                m = k;
                break;
        }
        return m;
    }

    public static void main(String[] args)  {

        SpimeDB db = new SpimeDB();;


        final Parser tika = new AutoDetectParser();
        final ContentHandlerFactory tikaFactory = new BasicContentHandlerFactory( BasicContentHandlerFactory.HANDLER_TYPE.HTML, -1);
        final RecursiveParserWrapper tikaWrapper = new RecursiveParserWrapper(tika, tikaFactory);
        db.on((NObject x, SpimeDB d) -> {

            String url = x.get("url");
            if (url.startsWith("file:/") && !x.has("contentType")) {


                Metadata metadata = new Metadata();
                ParseContext context = new ParseContext();

                MutableNObject y = new MutableNObject(x);
                try {
                    InputStream stream = new URL(url).openStream();
                    if (stream == null) {
                        throw new FileNotFoundException();
                    }

                    tikaWrapper.parse(new BufferedInputStream(stream, BUFFER_SIZE), new DefaultHandler(), metadata, context);


                    List<Metadata> m = tikaWrapper.getMetadata();
                    m.forEach(md -> {
                        for (String k : md.names()) {
                            String[] v = md.getValues(k);

                            String kk = tikiToField(k);
                            if (kk != null) {
                                Object vv = v.length > 1 ? v : v[0];
                                if (vv instanceof String) {
                                    try {
                                        int ivv = Integer.parseInt((String) vv);
                                        vv = ivv;
                                    } catch (Exception e) {
                                        //not an int
                                    }
                                }
                                y.put(kk, vv);
                            }
                        }
                    });

                    stream.close();
                } catch (Exception e) {
                    //resource.put("ParseException", e);
                    e.printStackTrace();
                }


                db.add(y);
            }
        });

        db.on((NObject x, SpimeDB d) -> {

            if ("application/pdf".equals(x.get("contentType")) && (d.graph.inDegreeOf(x.id())==0)) {

                String parentContent = x.get("body");
                Document parentDOM = Jsoup.parse(parentContent);

                Elements pagesHTML = parentDOM.select(".page");

                int pageCount = x.get("pageCount");
                for (int page = 0; page < pageCount; page++) {
                    d.add(
                        new MutableNObject(x.id() + "#" + page)
                            .withTags(x.id())
                            .put("url", x.get("url") + "#" + page)
                            .put("contentType", "page/pdf")
                            .put("page", page)
                            .put("body", pagesHTML.get(page).removeClass("page").toString())
                    );
                }

                //clean and update parent DOM
                pagesHTML.remove();
                parentDOM.select("meta").remove();
                d.add(new MutableNObject(x).put("body", parentDOM.toString()));
            }
        });


        SpimeDB.LOG("org.apache.pdfbox.rendering.CIDType0Glyph2D", Level.ERROR);
        SpimeDB.LOG("org.apache.pdfbox", Level.ERROR);
        java.util.logging.Logger.getLogger("org.apache.pdfbox.rendering.CIDType0Glyph2D").setLevel(java.util.logging.Level.SEVERE);
        IIORegistry.getDefaultInstance().registerServiceProvider(new JBIG2ImageReaderSpi());
        db.on((NObject n, SpimeDB d) -> {
            int dpi = 32;

            if ("page/pdf".equals(n.get("contentType")) && !n.has("image")) {

                try {
                    PDDocument document = PDDocument.load(new URL(n.get("url")).openStream());

                    try {

                        PDFRenderer renderer = new PDFRenderer(document);

                        int page = n.get("page");

                        BufferedImage img = renderer.renderImageWithDPI(page, (float) dpi, ImageType.RGB);


                        String resourceURL = Base64.encode(n.id()) + ".page" + page + "." + img.getWidth() + "x" + img.getHeight() + ".jpg"; //relative
                        String filename = "/tmp/eadoc/" + resourceURL;
                        boolean result = ImageIOUtil.writeImage(img, filename, dpi);

                        d.add(new MutableNObject(n).put("image", resourceURL));


                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    document.close();
                } catch (IOException f) {
                    f.printStackTrace();
                }

            }
        });


        FileDirectory.createFileNodes("/home/me/d/eadocsmall", db);


        db.sync(1000 * 60);

        TreeSet<NObject> t = new TreeSet(Comparators.byFunction((NObject d) -> d.id()));
        Iterables.addAll(t, db);

        t.forEach(x -> System.out.println(JSON.toJSONString(x, true)));


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
