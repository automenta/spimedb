package spimedb.io;

import ch.qos.logback.classic.Level;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import org.jsoup.nodes.Element;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Whitelist;
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
import java.nio.file.Files;
import java.nio.file.Paths;
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

            case "dc:title":  return null;  //duplicates
            case "Last-Modified":  return null; //duplicates
            case "pdf:docinfo:created":  return null; //duplicates
            case "Creation-Date":  return null; //duplicates
            case "created":  return null; //duplicates

            case "creator": return null;
            case "meta:author": return null;
            case "meta:creation-date": return null;
            case "pdf:PDFVersion": return null;
            case "access_permission:can_modify": return null;
            case "access_permission:extract_for_accessibility": return null;
            case "access_permission:assemble_document": return null;
            case "access_permission:extract_content": return null;
            case "access_permission:fill_in_form": return null;

            case "producer": return "generator";

            case "pdf:docinfo:producer": return null;
            case "modified": return null;
            case "Last-Save-Date": return null;
            case "pdf:docinfo:modified": return null;
            case "meta:save-date": return null;
            case "meta:keyword": return null;
            case "cp:subject": return null;
            case "dc:creator": return null;

            case "dc:description": return null;
            case "dc:subject": return null;

            case "pdf:docinfo:creator": return null;
            case "pdf:docinfo:subject": return null;
            case "X-Parsed-By": return null;
            case "pdf:encrypted": return null;
            case "access_permission:modify_annotations": return null;
            case "access_permission:can_print_degraded": return null;
            case "access_permission:can_print": return null;
            case "pdf:docinfo:keywords": return null;

            case "Keywords": return "keywords";

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
    static final String solrURL = "http://ea:8983/solr/collection1";

    public static void main(String[] args)  {

        SpimeDB db = new SpimeDB();


        final Parser tika = new AutoDetectParser();
        final ContentHandlerFactory tikaFactory = new BasicContentHandlerFactory( BasicContentHandlerFactory.HANDLER_TYPE.HTML, -1);
        db.on((NObject x, SpimeDB d) -> {

            String url = x.get("url_in");
            if (url != null && !x.has("contentType")) {


                Metadata metadata = new Metadata();
                ParseContext context = new ParseContext();

                MutableNObject y = new MutableNObject(x);
                try {
                    URL uu = new URL(url);
                    InputStream stream = uu.openStream();
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



                    db.addAsync(y);

                } catch (Exception e) {
                    //resource.put("ParseException", e);
                    e.printStackTrace();
                }


            }
        });

        Cleaner cleaner = new Cleaner(Whitelist.basic());
        db.on((NObject x, SpimeDB d) -> {

            if ("application/pdf".equals(x.get("contentType")) && x.has("pageCount") && x.has("text") && (d.graph.isLeaf(x.id())) /* leaf */) {

                String parentContent = x.get("text");
                Document parentDOM = Jsoup.parse(parentContent);

                Elements pagesHTML = parentDOM.select(".page");

                int pageCount = x.get("pageCount");
                for (int _page = 0; _page < pageCount; _page++) {

                    final int page = _page;
                    d.runLater(()->{

                        Document pd = Document.createShell("");
                        pd.body().appendChild(pagesHTML.get(page).removeAttr("class"));
                        Elements cc = cleaner.clean(pd).body().children();
                        String[] pdb = cc.stream()
                                .filter(xx -> !xx.children().isEmpty() || xx.hasText())
                                .map(xx -> xx.tagName().equals("p") ? xx.text() : xx ) //just use <p> contents
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


                        d.addAsync(
                            new MutableNObject(x.id() + "#" + page)
                                .name(docTitle + " - (page " + (page+1) + ")")
                                .withTags(x.id())
                                .put("author", x.get("author"))
                                .put("url", x.get("url") + "#" + page) //browser loads the specific page when using the '#' anchor
                                .put("url_in", x.get("url_in"))
                                .put("contentType", "application/pdf")
                                .put("page", page)
                                .put("text", pdb.length > 0 ? Joiner.on('\n').join(pdb) : null)
                                .put("textParse",
                                        (pdb!=null)  ? Stream.of(pdb).map(
                                            t -> NLP.toString(NLP.parse(t))
                                        ).collect( Collectors.joining("\n")) : null)
                        );
                    });
                }

                //clean and update parent DOM

                d.addAsync(new MutableNObject(x)
                        //.put("subject", x.get("subject")!=null && !x.get("subject").equals(x.get("description") ?  x.get("subject") : null))
                        .put("text", null)
                        .put("textParse", x.name() != null ? NLP.toString(NLP.parse(
                                Joiner.on("\n").skipNulls().join(x.name(), x.get("description"))
                        )) : null) //parse the title + description
                );


            }
        });


        SpimeDB.LOG("org.apache.pdfbox.rendering.CIDType0Glyph2D", Level.ERROR);
        SpimeDB.LOG("org.apache.pdfbox", Level.ERROR);
        java.util.logging.Logger.getLogger("org.apache.pdfbox.rendering.CIDType0Glyph2D").setLevel(java.util.logging.Level.SEVERE);
        IIORegistry.getDefaultInstance().registerServiceProvider(new JBIG2ImageReaderSpi());
        db.on((NObject n, SpimeDB d) -> {

            if (n.has("page") && !n.has("pageCount") && "application/pdf".equals(n.get("contentType")) && !n.has("image")) {

                int page = n.get("page");
                String id = n.id();
                String pageFile = (id.substring(0, id.lastIndexOf('#'))) + ".page" + page + "." + pdfPageImageDPI + ".jpg";
                        //img.getWidth() + "x" + img.getHeight() +

                String outputFile = pdfPageImageOutputPath + "/" + pageFile;

                if (!Files.exists(Paths.get(outputFile))) {
                    try {
                        PDDocument document = PDDocument.load(new URL(n.get("url_in")).openStream());

                        try {

                            PDFRenderer renderer = new PDFRenderer(document);


                            BufferedImage img = renderer.renderImageWithDPI(page, (float) pdfPageImageDPI, ImageType.RGB);


                            boolean result = ImageIOUtil.writeImage(img, outputFile, pdfPageImageDPI);



                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        document.close();
                    } catch (IOException f) {
                        f.printStackTrace();
                    }

                }

                d.addAsync(new MutableNObject(n).put("image", pageFile));

            }
        });


//        db.on((x, d) -> {
//            d.run(x.id(), ()-> {
//                try {
//                    Solr.solrUpdate(solrURL + "/update", x); //l.toArray(new NObject[l.size()]));
//                } catch (IOException e) {
//                    logger.error("solr update: {}", e);
//                }
//            });
//        });

        FileDirectory.load("/home/me/d/eadocsmall", db);


        db.sync(1000 * 60);

        db.forEach(x -> System.out.println(x.toJSONString(true)));


    }

    private static JsonNode html2json(Element e) {

        ObjectNode n = JSON.json.createObjectNode();
        boolean hasChildren = e.children().isEmpty();
        if (hasChildren)
            n.set(e.tagName(), JSON.json.valueToTree(e.children().stream().map(Multimedia::html2json).toArray(x -> new JsonNode[x])));
        if (e.hasText()) {
            n.set(hasChildren ? "_" : e.tagName(), JSON.json.valueToTree(e.textNodes().stream().map(x -> x.text()).toArray(x -> new String[x])));
        }
        return n;
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
