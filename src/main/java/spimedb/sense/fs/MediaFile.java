package spimedb.sense.fs;

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
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.helpers.DefaultHandler;
import spimedb.util.JSON;

import javax.imageio.spi.IIORegistry;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * https://svn.apache.org/repos/asf/tika/trunk/tika-example/src/main/java/org/apache/tika/example/LuceneIndexerExtended.java
 * https://svn.apache.org/repos/asf/tika/trunk/tika-example/src/main/java/org/apache/tika/example/SimpleTextExtractor.java
 * https://github.com/apache/pdfbox/tree/trunk/examples/src/main/java/org/apache/pdfbox/examples
 */
public class MediaFile {

    static {
        IIORegistry.getDefaultInstance().registerServiceProvider(new JBIG2ImageReaderSpi());
    }

    final File file;
    final Map<String, Object> meta;
    public final static Logger logger = LoggerFactory.getLogger(MediaFile.class);

    //"Title" -> "N"
    //"X-TIKA:content" -> "_"

    @Nullable
    static String meta(String k) {

        String m;
        switch (k) {
            case "Title": m = "N"; break;
            case "X-TIKA:content": m = "_"; break;

            case "pdf:docinfo:title": m = null; break; //duplicates "Title"
            //TODO other duplcates

            default: m = k; break;
        }
        return m;
    }

    public MediaFile(String path) {
        file = new File(path);

        Parser p = new AutoDetectParser();
        ContentHandlerFactory factory = new BasicContentHandlerFactory(
                BasicContentHandlerFactory.HANDLER_TYPE.HTML, -1);

        RecursiveParserWrapper wrapper = new RecursiveParserWrapper(p, factory);
        Metadata metadata = new Metadata();
        //metadata.set(Metadata.RESOURCE_NAME_KEY, "test_recursive_embedded.docx");
        ParseContext context = new ParseContext();

        meta = new HashMap();

        try {
            InputStream stream = new FileInputStream(file);
            wrapper.parse(stream, new DefaultHandler(), metadata, context);

            List<Metadata> m = wrapper.getMetadata();
            m.forEach(d -> {
                for (String k : d.names()) {
                    String[] v = d.getValues(k);

                    String kk = meta(k);
                    if (kk!=null)
                        this.meta.put(kk, v.length > 1 ? v : v[0]);
                }
            });

        } catch (Exception e) {
            meta.put("ParseException", e);
        }

        System.out.println( JSON.toJSONString(meta) );
        try {
            //new XML(meta.get("_").toString());
            Element body = Jsoup.parse(meta.get("_").toString()).body();
            System.out.println(
                    body.toString()
            );
        } catch (Exception e) {
            e.printStackTrace();
        }

        if ("application/pdf".equals(meta.get("Content-Type"))) {
            //PDFToImage
            try {
                PDDocument document = PDDocument.load(file);
                PDFRenderer renderer = new PDFRenderer(document);


                int page = 0;
                int dpi = 32;

                BufferedImage img = renderer.renderImageWithDPI(page, (float)dpi, ImageType.RGB);
                String filename = "/tmp/page" + page + "." + dpi + ".jpg";
                boolean result = ImageIOUtil.writeImage(img, filename, dpi);
                System.out.println(filename + " " + result);

            } catch (IOException e) {
                logger.error("pdf to image: {}", e);
            }

        }


    }

    public static void main(String[] args) {
        new MediaFile("/tmp/pdf.pdf");
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
