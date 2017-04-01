package spimedb.media;


import org.jsoup.Jsoup;

/**
 * https://docs.oracle.com/javase/tutorial/jaxp/dom/readingXML.html
 */
public class XML {

    //final DocumentBuilderFactory dbf = DocumentBuilderFactory.newDefaultInstance();

    public XML(String s) {
//        this(IOUtils.toInputStream(s, Charset.defaultCharset()));
//    }
//
//    public XML(InputStream in) throws ParserConfigurationException, IOException, SAXException {

        org.jsoup.nodes.Document doc = Jsoup.parse(s);

        
        //OutputStreamWriter errorWriter = new OutputStreamWriter(System.err, Charset.defaultCharset());
        //db.setErrorHandler(new MyErrorHandler (new PrintWriter(errorWriter, true)));
        //org.w3c.dom.Document doc = db.parse(in);

        System.out.println(doc);

    }

}
