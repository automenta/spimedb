package spimedb.io;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;
import org.jpedal.jbig2.jai.JBIG2ImageReaderSpi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spimedb.NObject;
import spimedb.SpimeDB;
import spimedb.plan.AtomicGoal;

import javax.imageio.spi.IIORegistry;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;


public class PDF {


    public static class ToImage extends AtomicGoal<SpimeDB> {

        public final static Logger logger = LoggerFactory.getLogger(ToImage.class);

        static {
            IIORegistry.getDefaultInstance().registerServiceProvider(new JBIG2ImageReaderSpi());
        }

        private final String id;

        int dpi = 32;

        public ToImage(String id) {
            super(id);
            this.id = id;
        }


        @Override
        protected void run(SpimeDB context) throws IOException {
            //PDFToImage



        }
    }


}
