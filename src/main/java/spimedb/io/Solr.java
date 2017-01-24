package spimedb.io;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spimedb.NObject;
import spimedb.util.JSON;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * https://cwiki.apache.org/confluence/display/solr/Uploading+Data+with+Index+Handlers
 *
 */
public class Solr {

    public final static Logger logger = LoggerFactory.getLogger(Solr.class);

    public static ArrayNode nobject2solrUpdate(NObject... xx) {
        ArrayNode a = JSON.json.createArrayNode();
        for (NObject x : xx) {
            ObjectNode y = JSON.json.createObjectNode();
            y.put("id", x.id());
            y.put("_text_", x.name());
            //y.put("_text_", y.remove("text"));
            a.add(y);
        }
        return a;
    }

    public static void solrUpdate(String url, NObject... xx) throws IOException {

        logger.info("updating {} with {} objects", url, xx.length);

        String data = Solr.nobject2solrUpdate(xx).toString();
//        Solr.POST("http://ea:8983/solr/x/update/json/docs", Maps.mutable.with(
//            "Content-type:", "application/json"
//        ), u);

        //System.out.println(url);

        URL uu = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) uu.openConnection();
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");


        OutputStream s = conn.getOutputStream();
        IOUtils.write(data, s);
        s.write('\n');
        s.flush();

        String res = IOUtils.toString(conn.getInputStream());
        s.close();
        //IOUtils.copy(new FileInputStream(flacAudioFile), conn.getOutputStream());

        System.out.println(res);
    }



        /*
   [
    {
        "id": "1",
            "title": "Solr adds block join support",
            "content_type": "parentDocument",
            "_childDocuments_": [
        {
            "id": "2",
                "comments": "SolrCloud supports it too!"
        }
    ]
    },
    {
        "id": "3",
            "title": "New Lucene and Solr release is out",
            "content_type": "parentDocument",
            "_childDocuments_": [
        {
            "id": "4",
                "comments": "Lots of new features"
        }
    ]
    }
]
    */


}
