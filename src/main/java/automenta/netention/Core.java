package automenta.netention;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.undercouch.bson4jackson.BsonFactory;

import java.io.IOException;
import java.util.Base64;

/**
 * Created by me on 5/21/15.
 */
public class Core {

    final public static ObjectMapper json = new ObjectMapper()
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
            .configure(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS, false)
            .configure(JsonGenerator.Feature.QUOTE_FIELD_NAMES, true)
            .configure(JsonGenerator.Feature.ESCAPE_NON_ASCII, true)
            .configure(JsonGenerator.Feature.QUOTE_NON_NUMERIC_NUMBERS, true)
            .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);


    final public static JsonNodeFactory newJson = new JsonNodeFactory(false);

    public final static ObjectMapper bson = new ObjectMapper(new BsonFactory());

    public static String toJSON(Object o) {
        try {
            return json.writeValueAsString(o);
        } catch (JsonProcessingException ex) {
            System.out.println(ex.toString());
            try {
                return json.writeValueAsString( o.toString() );
            } catch (JsonProcessingException ex1) {
                return null;
            }
        }
    }



    public static ObjectNode fromJSON(String x) {
        try {

            return json.readValue(x, ObjectNode.class);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }



    public static String uuid() {
        int bits = 8 * 16;
        byte[] bytes = new byte[bits/8];
        for (int i = 0; i < bytes.length; i++)
            bytes[i] = (byte)(Math.random() * 256); //TODO use xorshfitrandom

        return Base64.getEncoder().encodeToString(bytes);
    }
}
