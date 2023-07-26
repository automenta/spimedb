package spimedb.media;

import ch.qos.logback.classic.Level;
import com.google.common.base.Joiner;
import org.apache.commons.io.IOUtils;
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
import org.jetbrains.annotations.NotNull;
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
import spimedb.Plugin;
import spimedb.SpimeDB;

import javax.imageio.spi.IIORegistry;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.List;
import java.util.function.BiFunction;

/**
 * Detects document and multimedia metadata, and schedules further processing
 * <p>
 * https://svn.apache.org/repos/asf/tika/trunk/tika-example/src/main/java/org/apache/tika/example/LuceneIndexerExtended.java
 * https://svn.apache.org/repos/asf/tika/trunk/tika-example/src/main/java/org/apache/tika/example/SimpleTextExtractor.java
 * https://github.com/apache/pdfbox/tree/trunk/examples/src/main/java/org/apache/pdfbox/examples
 */
public class Multimedia implements Plugin, BiFunction<NObject, NObject, NObject> {


    public final static Logger logger = LoggerFactory.getLogger(Multimedia.class);


    static final Cleaner cleaner = new Cleaner(Whitelist.basic());

    static final float thumbnailQuality = 0.75f;
    static final int pdfPageImageDPI = 32;

    static {
        for (String s : new String[]{
                "org.apache.pdfbox.rendering.CIDType0Glyph2D",
                "org.apache.pdfbox.pdmodel.font.PDSimpleFont",
                "org.apache.pdfbox.pdmodel.font.PDTrueTypeFont",
                "org.apache.pdfbox.pdmodel.font.PDType0Font",
                "org.apache.pdfbox.pdmodel.font.PDCIDFontType0",
                "org.apache.pdfbox.pdmodel.font.PDCIDFontType2",
                "org.apache.pdfbox.io.ScratchFileBuffer",
                "org.apache.pdfbox.pdfparser.PDFObjectStreamParser",
                "org.apache.pdfbox.pdmodel.font.FileSystemFontProvider",
                "org.apache.pdfbox.tools.imageio.MetaUtil"
        }) {
            SpimeDB.LOG(LoggerFactory.getLogger(s), Level.ERROR);
            //((SLF4JLocationAwareLog) (LogFactory.getLog(s))).setLevel(Level.SEVERE);
        }
        IIORegistry.getDefaultInstance().registerServiceProvider(new JBIG2ImageReaderSpi());
    }

    private final SpimeDB db;

    public Multimedia(SpimeDB db) {
        this.db = db;

        db.on(this);
        logger.info("{} enabled {}", this, db.onChange);

        //process existing items
        db.forEach((xx) -> xx.forEach(x -> {
            NObject y = apply(x, x);
            if (y != x) {
                db.addAsync(0.5f, y);
            }
        }), db.exe.concurrency-1);
    }

    @Override
    public NObject apply(NObject p, NObject x) {

        logger.debug("multimedia {}", p, x);

        final String url = x.get("url_in");

        String docID = x.id();

        if (url == null) {
            return x;
        }

        try {
            long exp;
            InputStream stream;
            long fileSize;
            if (url.startsWith("file:")) {
                File f = new File(url.substring(5));
                exp = f.lastModified();
                stream = new FileInputStream(f);
                fileSize = f.length();
            } else {
                URL uu = new URL(url);
                URLConnection con = uu.openConnection();
                exp = con.getExpiration();
                if (exp == 0)
                    exp = con.getLastModified();
                fileSize = con.getContentLengthLong();
                stream = con.getInputStream();
            }

            if (stream == null) {
                throw new FileNotFoundException();
            }

            //logger.info("in: {} {} {}", url, p!=null ? p.get("url_cached") : "null", x.get("url_cached"));

            //TODO use a separate url_cached for each instance of a sibling class like Multimedia that does only one processing
            //this way they can be enabled/disabled separately without interfering with each other
            //TODO store a hashcode of the data as well as the time for additional integrity

            Long whenCached = x.get("url_cached");
            if (whenCached != null && whenCached <= exp) {
                logger.debug("cached: {}", url);
                return x; //still valid
            }


            logger.info("load: {}", url);

            x = new GeoNObject(x);

            ((MutableNObject) x).put("url_cached", exp);


            boolean isKMLorKMZ = url.endsWith(".kml") || url.endsWith(".kmz");
            boolean isGeoJSON = url.endsWith(".geojson");

            if (!isKMLorKMZ && !isGeoJSON  /* handled separately below */) {

                Metadata metadata = new Metadata();
                ParseContext context = new ParseContext();

                final Parser tika = new AutoDetectParser();
                final ContentHandlerFactory tikaFactory = new BasicContentHandlerFactory(BasicContentHandlerFactory.HANDLER_TYPE.HTML, -1);

                final RecursiveParserWrapper tikaWrapper = new RecursiveParserWrapper(tika, tikaFactory);

                if (stream instanceof FileInputStream) {
                    ((MutableNObject) x).put(NObject.DATA, url);
                } else {
                    //buffer the bytes for saving
                    byte[] bytes = IOUtils.readFully(stream, (int) fileSize);
                    ByteArrayInputStream stream2 = new ByteArrayInputStream(bytes);
                    stream.close();
                    stream = stream2;
                    ((MutableNObject) x).put(NObject.DATA, bytes);
                }

                tikaWrapper.parse(stream, new DefaultHandler(), metadata, context);

                stream.close();


                List<Metadata> m = tikaWrapper.getMetadata();
                NObject finalX = x;
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
                            ((MutableNObject) finalX).put(kk, vv);
                        }
                    }
                });

            }


            //db.addAsync(y).get();

            //HACK run these after the updated 'y' is submitted in case these want to modify it when they run

            if (isKMLorKMZ) {
                new KML(db, ((GeoNObject) x)).url(url).run();
            } else if (isGeoJSON) {
                GeoJSON.load(url, GeoJSON.baseGeoJSONBuilder, db);
            }

        } catch (Exception e) {
            logger.error("url_in removal: {}", e);
        }


        Object mime = x.get(NObject.TYPE);

        if (mime != null && (mime.equals("image/jpeg") || mime.equals("image/png") /* ... */)) {
            x = new MutableNObject(x)
                    .name(titleify(docID))
                    .put(NObject.DESC, null)
                    .put(NObject.ICON, NObject.DATA /* redirect to the data field which already has the byte[] image */)
            ;

        }

        if ("application/pdf".equals(mime) && x.has("pageCount") && x.has(NObject.DESC)  /* leaf */) {

            int pageCount = x.get("pageCount");

            //float docPri = Util.lerp(1f / (pageCount), 0.75f, 0.25f);

            String parentContent = x.get(NObject.DESC);
            String author = x.get("author");

            //db.runLater(docPri, () -> {

            Document parentDOM = Jsoup.parse(parentContent);

            Elements pagesHTML = parentDOM.select(".page");


            try {


                InputStream is;
                if (url.startsWith("file:")) {
                    is = fileStream(url);
                } else {
                    is = new URL(url).openStream();
                }

                PDDocument document = PDDocument.load(is);

                PDFRenderer renderer = new PDFRenderer(document);


                for (int _page = 0; _page < pageCount; _page++) {

                    final int pageActual = _page;
                    final int page = _page + 1;
                    logger.info("paginate: {} {}", docID, page);

                    Document pd = Document.createShell("");
                    pd.body().appendChild(pagesHTML.get(pageActual).removeAttr("class"));
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
                        docTitle = titleify(docID);
                    }

                    BufferedImage img = renderer.renderImageWithDPI(pageActual, (float) pdfPageImageDPI, ImageType.RGB);

                    //boolean result = ImageIOUtil.writeImage(img, outputFile, pdfPageImageDPI);
                    ByteArrayOutputStream os = new ByteArrayOutputStream(img.getWidth() * img.getHeight() * 3 /* estimate */);
                    boolean result = ImageIOUtil.writeImage(img, "jpg", os, pdfPageImageDPI, thumbnailQuality);
                    os.close();

                    byte[] thumbnail = os.toByteArray();

                    String text = pdb.length > 0 ? Joiner.on('\n').join(pdb) : null;

                    String pageID = docID + "/" + page;
                    db.add(
                            new MutableNObject(pageID)
                                    .name(docTitle + " - (" + page + " of " + (pageCount + 1) + ")")
                                    .withTags(docID)
                                    .put("author", author)
                                    .put("url", url) //HACK browser loads the specific page when using the '#' anchor
                                    .put(NObject.TYPE, "application/pdf")
                                    .put(NObject.DATA, docID)
                                    .put("page", page)
                                    .put(NObject.DESC, text)
                                            /*.putLater("textParse", 0.1f, ()-> {
                                                return (pdb.length > 0) ? Stream.of(pdb).map(
                                                        t -> NLP.toString(NLP.parse(t))
                                                ).collect(Collectors.joining("\n")) : null;
                                            })*/
                                    .put(NObject.ICON, thumbnail)
                    );
                }


                document.close();

            } catch (IOException f) {
                logger.error("error: {} {}", docID, f);
            }

            //clean and update parent DOM

            //String xname = x.name();
            //String desc = x.get(NObject.DESC);
            x = new MutableNObject(x)
                    .name(titleify(docID))
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

    }


    @NotNull
    private static FileInputStream fileStream(String url) throws FileNotFoundException {
        return new FileInputStream(url.substring(5));
    }

    private static String titleify(String id) {
        return URLDecoder.decode(id).replace("_", " ").trim();
    }

    @Nullable
    static String tikiToField(String k) {

        String m;
        switch (k) {
            case "dc:title", "pdf:docinfo:keywords", "access_permission:can_print", "access_permission:can_print_degraded", "access_permission:modify_annotations", "pdf:encrypted", "X-Parsed-By", "pdf:docinfo:subject", "pdf:docinfo:creator", "dc:subject", "dc:description", "dc:creator", "cp:subject", "meta:keyword", "meta:save-date", "pdf:docinfo:modified", "Last-Save-Date", "modified", "pdf:docinfo:producer", "access_permission:fill_in_form", "access_permission:extract_content", "access_permission:assemble_document", "access_permission:extract_for_accessibility", "access_permission:can_modify", "pdf:PDFVersion", "meta:creation-date", "meta:author", "creator", "created", "Creation-Date", "pdf:docinfo:created", "Last-Modified" -> {
                return null;  //duplicates
            }
            case "producer" -> {
                return "generator";
            }
            case "Keywords" -> {
                return "keywords";
            }
            case "title" -> m = "N";
            case "X-TIKA:content" -> m = NObject.DESC;
            case "Author" -> m = "author";
            case "Content-Type" -> m = NObject.TYPE;
            case "xmpTPg:NPages" -> m = "pageCount";
            case "pdf:docinfo:title" -> m = null;
            //duplicates "Title"
            //TODO other duplcates

            default -> m = k;
        }

        return m;
    }


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
