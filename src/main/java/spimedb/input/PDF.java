package spimedb.input;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;
import org.jpedal.jbig2.jai.JBIG2ImageReaderSpi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.spi.IIORegistry;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;


public class PDF {

    static {
        IIORegistry.getDefaultInstance().registerServiceProvider(new JBIG2ImageReaderSpi());
    }

    public final static Logger logger = LoggerFactory.getLogger(Multimedia.class);

    public static void expand(InputStream stream) {
            //PDFToImage
            try {
                PDDocument document = PDDocument.load(stream);
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
