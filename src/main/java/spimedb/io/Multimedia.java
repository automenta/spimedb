package spimedb.io;

import com.google.common.base.Joiner;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.Jdk14Logger;
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
import java.net.URLDecoder;
import java.util.List;
import java.util.logging.Level;

/**
 * Detects document and multimedia metadata, and schedules further processing
 * <p>
 * https://svn.apache.org/repos/asf/tika/trunk/tika-example/src/main/java/org/apache/tika/example/LuceneIndexerExtended.java
 * https://svn.apache.org/repos/asf/tika/trunk/tika-example/src/main/java/org/apache/tika/example/SimpleTextExtractor.java
 * https://github.com/apache/pdfbox/tree/trunk/examples/src/main/java/org/apache/pdfbox/examples
 */
public class Multimedia {


    public final static Logger logger = LoggerFactory.getLogger(Multimedia.class);


    final Parser tika = new AutoDetectParser();
    final ContentHandlerFactory tikaFactory = new BasicContentHandlerFactory(BasicContentHandlerFactory.HANDLER_TYPE.HTML, -1);

    static final Cleaner cleaner = new Cleaner(Whitelist.basic());

    private final float thumbnailQuality = 0.75f;


    public Multimedia(SpimeDB db) {

        for (String s : new String[]{"org.apache.pdfbox.rendering.CIDType0Glyph2D", "org.apache.pdfbox.pdmodel.font.PDTrueTypeFont"}) {
            ((Jdk14Logger) LogFactory.getLog(s)).getLogger().setLevel(Level.SEVERE);
        }

        IIORegistry.getDefaultInstance().registerServiceProvider(new JBIG2ImageReaderSpi());

        db.on((NObject p, NObject x) -> {
            String url_in = x.get("url_in");
            String url = url_in;

            String xid = x.id();

            if (url != null) {


                try {
                    URL uu = new URL(url);
                    URLConnection con = uu.openConnection();
                    long exp = con.getExpiration();
                    if (exp == 0)
                        exp = con.getLastModified();

                    //logger.info("in: {} {} {}", url, p!=null ? p.get("url_cached") : "null", x.get("url_cached"));

                    if (p != null) {
                        String whenCached = p.get("url_cached");
                        if (!(whenCached == null || Long.valueOf(whenCached) < exp)) {
                            logger.debug("cached: {}", url);
                            return p; //still valid
                        }
                    }

                    logger.info("load: {}", url);

                    MutableNObject y = new MutableNObject(x);

                    y.put("url_cached", Long.toString(exp));

                    boolean isKMLorKMZ = url.endsWith(".kml") || url.endsWith(".kmz");
                    boolean isGeoJSON = url.endsWith(".geojson");

                    if (!isKMLorKMZ && !isGeoJSON  /* handled separately below */) {

                        Metadata metadata = new Metadata();
                        ParseContext context = new ParseContext();

                        InputStream stream;
                        try {
                            stream = con.getInputStream();
                        } catch (FileNotFoundException e) {
                            logger.error("not found: {}", url, e.getMessage());
                            return null;
                        }
                        if (stream == null) {
                            throw new FileNotFoundException();
                        }

                        final RecursiveParserWrapper tikaWrapper = new RecursiveParserWrapper(tika, tikaFactory);

                        int fileSize = con.getContentLength();

                        byte[] bytes = IOUtils.readFully(stream, fileSize);
                        InputStream is = new ByteArrayInputStream(bytes);
                        tikaWrapper.parse(is, new DefaultHandler(), metadata, context);

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

                        y.put("data", bytes);
                    }


                    //db.addAsync(y).get();

                    //HACK run these after the updated 'y' is submitted in case these want to modify it when they run

                    if (isKMLorKMZ) {
                        new KML(db, y).url(url).run();
                    } else if (isGeoJSON) {
                        GeoJSON.load(url, GeoJSON.baseGeoJSONBuilder, db);
                    }


                    x = y;

                } catch (Exception e) {
                    logger.error("url_in removal: {}", e);
                }

            }

            if ("application/pdf".equals(x.get(NObject.TYPE)) && x.has("pageCount") && x.has(NObject.DESC) && (db.graph.isLeaf(x.id())) /* leaf */) {

                String parentContent = x.get(NObject.DESC);
                Document parentDOM = Jsoup.parse(parentContent);

                Elements pagesHTML = parentDOM.select(".page");

                String author = x.get("author");

                int pageCount = x.get("pageCount");
                for (int _page = 0; _page < pageCount; _page++) {

                    final int page = _page+1;
                    db.runLater(0.75f, () -> {

                        logger.info("paginate: {} {}", xid, page);

                        Document pd = Document.createShell("");
                        pd.body().appendChild(pagesHTML.get(page).removeAttr("class"));
                        Elements cc = cleaner.clean(pd).body().children();
                        String[] pdb = cc.stream()
                                .filter(xx -> !xx.children().isEmpty() || xx.hasText())
                                .map(xx -> xx.tagName().equals("p") ? xx.text() : xx) //just use <p> contents
                                .map(Object::toString).toArray(String[]::new);


//                    List<JsonNode> jdb = new ArrayList(pdb.size());
//                    pdb.forEach(e -> {
//                        if (e.children().isEmpty() && e.text().isEmpty())
//                            return;
//                        jdb.add(html2json(e));
//                    });

                        String docTitle = parentDOM.title(); //x.name();
                        if (docTitle == null || docTitle.isEmpty()) {
                            docTitle = titleify(xid);
                        }


                        db.add(
                                new MutableNObject(xid + "/" + page)
                                        .name(docTitle + " - (" + page + " of " + (pageCount + 1) + ")")
                                        .withTags(xid)
                                        .put("author", author)
                                        .put("url", url_in) //HACK browser loads the specific page when using the '#' anchor
                                        .put(NObject.TYPE, "application/pdf")
                                        .put("data", "/data?I=" + xid + "#page=" + page)
                                        .put("page", page)
                                        .put(NObject.DESC, pdb.length > 0 ? Joiner.on('\n').join(pdb) : null)
                                        /*.putLater("textParse", 0.1f, ()-> {
                                            return (pdb.length > 0) ? Stream.of(pdb).map(
                                                    t -> NLP.toString(NLP.parse(t))
                                            ).collect(Collectors.joining("\n")) : null;
                                        })*/
                                        .putLater("thumbnail", 0.5f, () -> {

                                            PDDocument document = null;

                                            logger.info("thumbnail: {} {}", xid, page);

                                            try {


                                                document = PDDocument.load(new URL(url_in).openStream());


                                                PDFRenderer renderer = new PDFRenderer(document);


                                                BufferedImage img = renderer.renderImageWithDPI(page, (float) pdfPageImageDPI, ImageType.RGB);


                                                //boolean result = ImageIOUtil.writeImage(img, outputFile, pdfPageImageDPI);
                                                ByteArrayOutputStream os = new ByteArrayOutputStream(img.getWidth() * img.getHeight() * 3 /* estimate */);
                                                boolean result = ImageIOUtil.writeImage(img, "jpg", os, pdfPageImageDPI, thumbnailQuality);

                                                return os.toByteArray();

                                            } catch (IOException f) {
                                                logger.error("thumbnail: {} {} {}", xid, page, f.getMessage());
                                                f.printStackTrace();
                                            } finally {
                                                if (document!=null)
                                                    try {
                                                        document.close();
                                                    } catch (IOException e) {

                                                    }
                                            }


                                            return null;

                                        })

                        );
                    });
                }

                //clean and update parent DOM

                //String xname = x.name();
                //String desc = x.get(NObject.DESC);
                x = new MutableNObject(x)
                        .name(titleify(xid))
                        .put(NObject.DESC, null)
                        /*.putLater("textParse", 0.15f, () -> {
                            return xname != null ? NLP.toString(NLP.parse(
                                    Joiner.on("\n").skipNulls().join(xname, desc)
                            )) : null;
                        }) //parse the title + description
                        */
                ;
            }

            return x;

        });


    }

    private static String titleify(String id) {
        return URLDecoder.decode(id).replace("_", " ").trim();
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
                m = NObject.DESC;
                break;
            case "Author":
                m = "author";
                break;
            case "Content-Type":
                m = NObject.TYPE;
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
