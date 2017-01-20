package spimedb.input;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;
import org.jpedal.jbig2.jai.JBIG2ImageReaderSpi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spimedb.SpimeDB;
import spimedb.plan.AtomicGoal;

import javax.imageio.spi.IIORegistry;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;


public class PDF {


    public static class ToImage extends AtomicGoal<SpimeDB> {

        public final static Logger logger = LoggerFactory.getLogger(ToImage.class);

        static {
            IIORegistry.getDefaultInstance().registerServiceProvider(new JBIG2ImageReaderSpi());
        }

        private final String uri;
        private final Supplier<InputStream> stream;
        private final int page;

        int dpi = 32;

        public ToImage(String uri, Supplier<InputStream> stream, int page) {
            super(uri, page);
            this.uri = uri;
            this.stream = stream;
            this.page = page;
        }


        @Override
        protected void run(SpimeDB context) throws IOException {
            //PDFToImage

            PDDocument document = PDDocument.load(stream.get());
            PDFRenderer renderer = new PDFRenderer(document);


            BufferedImage img = renderer.renderImageWithDPI(page, (float) dpi, ImageType.RGB);
            String filename = "/tmp/eadoc/\"" + uri + "\".page" + page + "." + dpi + ".jpg";
            boolean result = ImageIOUtil.writeImage(img, filename, dpi);
            System.out.println(filename + " " + result);


        }
    }


}
