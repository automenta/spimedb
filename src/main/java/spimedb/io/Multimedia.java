package spimedb.io;

import ch.qos.logback.classic.Level;
import com.google.common.base.Joiner;
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
import org.jetbrains.annotations.Nullable;
import org.jpedal.jbig2.jai.JBIG2ImageReaderSpi;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.helpers.DefaultHandler;
import spimedb.MutableNObject;
import spimedb.NObject;
import spimedb.SpimeDB;

import javax.imageio.spi.IIORegistry;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Detects document and multimedia metadata, and schedules further processing
 * <p>
 * https://svn.apache.org/repos/asf/tika/trunk/tika-example/src/main/java/org/apache/tika/example/LuceneIndexerExtended.java
 * https://svn.apache.org/repos/asf/tika/trunk/tika-example/src/main/java/org/apache/tika/example/SimpleTextExtractor.java
 * https://github.com/apache/pdfbox/tree/trunk/examples/src/main/java/org/apache/pdfbox/examples
 */
public class Multimedia {


    public final static Logger logger = LoggerFactory.getLogger(Multimedia.class);

    private static final int BUFFER_SIZE = 1024 * 128;

    final Parser tika = new AutoDetectParser();
    final ContentHandlerFactory tikaFactory = new BasicContentHandlerFactory(BasicContentHandlerFactory.HANDLER_TYPE.HTML, -1);

    static final Cleaner cleaner = new Cleaner(Whitelist.basic());


    public Multimedia(SpimeDB db) {

        db.on((NObject p, NObject x) -> {
            String url = x.get("url_in");

            if (url != null) {


                try {
                    URL uu = new URL(url);
                    URLConnection con = uu.openConnection();
                    long exp = con.getExpiration();
                    if (exp== 0)
                        exp = con.getLastModified();

                    //logger.info("in: {} {} {}", url, p!=null ? p.get("url_cached") : "null", x.get("url_cached"));

                    if (p!=null) {
                        String whenCached = p.get("url_cached");
                        if (!(whenCached == null || Long.valueOf(whenCached) < exp)) {
                            logger.info("cached: {}", url);
                            return p; //still valid
                        }
                    }

                    logger.info("load: {}", url);

                    MutableNObject y = new MutableNObject(x);

                    y.put("url_cached", Long.toString(exp));


                    Metadata metadata = new Metadata();
                    ParseContext context = new ParseContext();

                    InputStream stream = con.getInputStream();
                    if (stream == null) {
                        throw new FileNotFoundException();
                    }

                    final RecursiveParserWrapper tikaWrapper = new RecursiveParserWrapper(tika, tikaFactory);

                    tikaWrapper.parse(new BufferedInputStream(stream, BUFFER_SIZE), new DefaultHandler(), metadata, context);

                    stream.close();

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


                    //db.addAsync(y).get();

                    //HACK run these after the updated 'y' is submitted in case these want to modify it when they run

                    if ((url.endsWith(".kml") || url.endsWith(".kmz"))) {
                        new KML(db,y).url(url).run();
                    } else if (url.endsWith(".geojson")) {
                        GeoJSON.load(url, GeoJSON.baseGeoJSONBuilder, db);
                    }


                    return y;

                } catch (Exception e) {
                    logger.error("url_in removal: {}", e);
                }

            }

            return x;

        });

        db.on((NObject p, NObject x) -> {

            if ("application/pdf".equals(x.get("contentType")) && x.has("pageCount") && x.has("text") && (db.graph.isLeaf(x.id())) /* leaf */) {

                String parentContent = x.get("text");
                Document parentDOM = Jsoup.parse(parentContent);

                Elements pagesHTML = parentDOM.select(".page");

                int pageCount = x.get("pageCount");
                for (int _page = 0; _page < pageCount; _page++) {

                    final int page = _page;
                    db.runLater(() -> {

                        logger.info("load: {} page {}", x.id(), page);

                        Document pd = Document.createShell("");
                        pd.body().appendChild(pagesHTML.get(page).removeAttr("class"));
                        Elements cc = cleaner.clean(pd).body().children();
                        String[] pdb = cc.stream()
                                .filter(xx -> !xx.children().isEmpty() || xx.hasText())
                                .map(xx -> xx.tagName().equals("p") ? xx.text() : xx) //just use <p> contents
                                .map(Object::toString).toArray(String[]::new);
                        if (pdb.length == 0)
                            pdb = null;

//                    List<JsonNode> jdb = new ArrayList(pdb.size());
//                    pdb.forEach(e -> {
//                        if (e.children().isEmpty() && e.text().isEmpty())
//                            return;
//                        jdb.add(html2json(e));
//                    });

                        String docTitle = x.name();
                        if (docTitle == null) {
                            docTitle = x.id();
                        }


                        db.addAsync(
                                new MutableNObject(x.id() + "." + page)
                                        .name(docTitle + " - (" + (page + 1) + " of " + (pageCount+1) + ")")
                                        .withTags(x.id())
                                        .put("author", x.get("author"))
                                        .put("url", x.get("url_in")) //HACK browser loads the specific page when using the '#' anchor
                                        .put("contentType", "application/pdf")
                                        .put("page", page)
                                        .put("text", pdb.length > 0 ? Joiner.on('\n').join(pdb) : null)
                                        .put("textParse",
                                                (pdb != null) ? Stream.of(pdb).map(
                                                        t -> NLP.toString(NLP.parse(t))
                                                ).collect(Collectors.joining("\n")) : null)
                        );
                    });
                }

                //clean and update parent DOM

                return new MutableNObject(x)
                        //.put("subject", x.get("subject")!=null && !x.get("subject").equals(x.get("description") ?  x.get("subject") : null))
                        .put("text", null)
                        .put("textParse", x.name() != null ? NLP.toString(NLP.parse(
                                Joiner.on("\n").skipNulls().join(x.name(), x.get("description"))
                        )) : null) //parse the title + description
                ;
            } else {
                return x;
            }
        });


        SpimeDB.LOG("org.apache.pdfbox.rendering.CIDType0Glyph2D", Level.ERROR);
        SpimeDB.LOG("org.apache.pdfbox", Level.ERROR);
        //java.util.logging.Logger.getLogger("org.apache.pdfbox.rendering.CIDType0Glyph2D").setLevel(java.util.logging.Level.SEVERE);
        IIORegistry.getDefaultInstance().registerServiceProvider(new JBIG2ImageReaderSpi());
        db.on((NObject p, NObject x) -> {

            if (x.has("page") && !x.has("pageCount") && "application/pdf".equals(x.get("contentType")) && !x.has("image")) {

                String id = x.id();
                //String pageFile = (id.substring(0, id.lastIndexOf('#'))) + ".page" + page + "." + pdfPageImageDPI + ".jpg";
                //img.getWidth() + "x" + img.getHeight() +

                //String outputFile = pdfPageImageOutputPath + "/" + pageFile;

                MutableNObject y = new MutableNObject(x);
                int page = y.get("page");

                if (y.get("thumbnail") == null) {
                    try {
                        PDDocument document = PDDocument.load(new URL(y.get("url")).openStream());

                        try {

                            PDFRenderer renderer = new PDFRenderer(document);


                            BufferedImage img = renderer.renderImageWithDPI(page, (float) pdfPageImageDPI, ImageType.RGB);


                            //boolean result = ImageIOUtil.writeImage(img, outputFile, pdfPageImageDPI);
                            ByteArrayOutputStream os = new ByteArrayOutputStream(img.getWidth() * img.getHeight() * 3 /* estimate */);
                            boolean result = ImageIOUtil.writeImage(img, "jpg", os, pdfPageImageDPI);

                            y.put("thumbnail", os.toByteArray());


                            return y;

                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            document.close();
                        }

                    } catch (IOException f) {
                        f.printStackTrace();
                    }

                }


            }

            return x;
        });



    }

    @Nullable
    static String tikiToField(String k) {

        String m;
        switch (k) {

            case "dc:title":
                return null;  //duplicates
            case "Last-Modified":
                return null; //duplicates
            case "pdf:docinfo:created":
                return null; //duplicates
            case "Creation-Date":
                return null; //duplicates
            case "created":
                return null; //duplicates

            case "creator":
                return null;
            case "meta:author":
                return null;
            case "meta:creation-date":
                return null;
            case "pdf:PDFVersion":
                return null;
            case "access_permission:can_modify":
                return null;
            case "access_permission:extract_for_accessibility":
                return null;
            case "access_permission:assemble_document":
                return null;
            case "access_permission:extract_content":
                return null;
            case "access_permission:fill_in_form":
                return null;

            case "producer":
                return "generator";

            case "pdf:docinfo:producer":
                return null;
            case "modified":
                return null;
            case "Last-Save-Date":
                return null;
            case "pdf:docinfo:modified":
                return null;
            case "meta:save-date":
                return null;
            case "meta:keyword":
                return null;
            case "cp:subject":
                return null;
            case "dc:creator":
                return null;

            case "dc:description":
                return null;
            case "dc:subject":
                return null;

            case "pdf:docinfo:creator":
                return null;
            case "pdf:docinfo:subject":
                return null;
            case "X-Parsed-By":
                return null;
            case "pdf:encrypted":
                return null;
            case "access_permission:modify_annotations":
                return null;
            case "access_permission:can_print_degraded":
                return null;
            case "access_permission:can_print":
                return null;
            case "pdf:docinfo:keywords":
                return null;

            case "Keywords":
                return "keywords";

            case "title":
                m = "N";
                break;


            case "X-TIKA:content":
                m = "text";
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

    static final String pdfPageImageOutputPath = "/home/me/ea/res";
    static final int pdfPageImageDPI = 32;


//    private static JsonNode html2json(Element e) {
//
//        ObjectNode n = JSON.json.createObjectNode();
//        boolean hasChildren = e.children().isEmpty();
//        if (hasChildren)
//            n.set(e.tagName(), JSON.json.valueToTree(e.children().stream().map(Multimedia::html2json).toArray(x -> new JsonNode[x])));
//        if (e.hasText()) {
//            n.set(hasChildren ? "_" : e.tagName(), JSON.json.valueToTree(e.textNodes().stream().map(x -> x.text()).toArray(x -> new String[x])));
//        }
//        return n;
//    }

}
