package spimedb.io;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    public static JsonNode nobject2solrUpdate(NObject... xx) {
        ObjectMapper j = JSON.json;
        ObjectNode a = j.createObjectNode();

        ArrayNode da = a.withArray("delete");
        for (NObject x : xx) {
            da.add(x.id());
        }
        a.with("commit");

        for (NObject x : xx) {
            ObjectNode y = j.createObjectNode();
            y.put("id", x.id());
            y.put("name", x.name());
            x.forEach((k,v)->{
                if ((k.equals("I")) || (k.equals("N")) || k.equals("inh") || (k.equals(">")))
                    return;
                if (v instanceof String)
                    y.put(k,(String)v);
                else if (v instanceof Integer)
                    y.put(k,(Integer)v);
                else if (v instanceof String[]) {
                    String[] vv = (String[])v;
                    ArrayNode va = y.putArray(k);
                    for (String s : vv) {
                        va.add(s);
                    }
                }
            });

            a.with("add")
                    .put("overwrite", true)
                    .put("doc",y);
        }

        a.with("commit");

        //System.out.println(JSON.toJSONString(a, true));
        return a;
    }

    public static void solrUpdate(String url, NObject... xx) throws IOException {

        logger.info("updating {} with {} objects", url, xx.length);

        String data = Solr.nobject2solrUpdate(xx).toString();
//        Solr.POST("http://ea:8983/solr/x/update/json/docs", Maps.mutable.with(
//            "Content-type:", "application/json"
//        ), u);

        //System.out.println(data);

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
