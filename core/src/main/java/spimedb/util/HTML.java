package spimedb.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by me on 12/26/16.
 */
public final class HTML {
//
//    public static final HtmlCompressor compressor = new HtmlCompressor();
//
//    static {
//        //https://code.google.com/p/htmlcompressor/wiki/Documentation#Using_HTML_Compressor_from_Java_API
//
//        compressor.setRemoveComments(true);            //if false keeps HTML comments (default is true)
//        compressor.setRemoveMultiSpaces(true);         //if false keeps multiple whitespace characters (default is true)
//        compressor.setRemoveIntertagSpaces(true);      //removes iter-tag whitespace characters
//        compressor.setRemoveQuotes(true);              //removes unnecessary tag attribute quotes
//        compressor.setRemoveScriptAttributes(true);    //remove optional attributes from script tags
//        compressor.setRemoveLinkAttributes(true);      //remove optional attributes from link tags
//        compressor.setRemoveJavaScriptProtocol(true);      //remove optional attributes from link tags
//        compressor.setRemoveHttpProtocol(true);        //replace "http://" with "//" inside tag attributes
//        compressor.setRemoveHttpsProtocol(true);       //replace "https://" with "//" inside tag attributes
//        compressor.setRemoveSurroundingSpaces("br,p"); //remove spaces around provided tags
//        compressor.setRemoveStyleAttributes(true);
//
//        compressor.setSimpleDoctype(true);             //simplify existing doctype
//        compressor.setCompressCss(true);               //compress inline css
//
//
//    }

    private static final Safelist whitelist = Safelist.basicWithImages();
    private static final Cleaner cleaner = new Cleaner(whitelist);
    private static final Document.OutputSettings outputSettings = new Document.OutputSettings().prettyPrint(false);

    private static final Logger logger = LoggerFactory.getLogger(HTML.class);

    public static String filterHTML(String html) {
        if (html.indexOf('>')!=-1 && html.indexOf('<')!=-1) {
            try {
                Document clean = cleaner.clean(Jsoup.parseBodyFragment(html));
                clean.outputSettings(outputSettings);
                return clean.body().html();

//            String compressedHtml = compressor.compress(html);
//            return compressedHtml;
            } catch (Exception e) {
                logger.error("filterHTML {}: \"{}\"", e, html);
            }
        }
        return html;
    }

}
