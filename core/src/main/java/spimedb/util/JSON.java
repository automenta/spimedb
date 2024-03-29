
package spimedb.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper;
import com.google.common.primitives.Longs;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Base64;
import java.util.Random;

public class JSON {
    final public static ObjectMapper json = new ObjectMapper()
            //.disable(SerializationFeature.CLOSE_CLOSEABLE)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, false)
            .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
            .configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
            .configure(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS, true)
            .configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false)
            .configure(JsonGenerator.Feature.QUOTE_FIELD_NAMES, true)
            .configure(JsonGenerator.Feature.ESCAPE_NON_ASCII, true)
            .configure(JsonGenerator.Feature.QUOTE_NON_NUMERIC_NUMBERS, true)
            ;



    final public static ObjectMapper jsonSafe = json.copy()
            .enable(SerializationFeature.WRAP_EXCEPTIONS)
            .configure(JsonGenerator.Feature.IGNORE_UNKNOWN, true);

    public static final org.slf4j.Logger logger = LoggerFactory.getLogger(JSON.class);

    final static ThreadLocal<ObjectWriter> jsonWritersCondensed = ThreadLocal.withInitial(json::writer);
    final static ThreadLocal<ObjectWriter> jsonWritersPretty = ThreadLocal.withInitial(json::writerWithDefaultPrettyPrinter);

    static final Random uuidRandom = new Random();

    public static void writeArrayValues(float[] xx, JsonGenerator jsonGenerator) throws IOException {
        for (float x : xx) {
            float y;
            /*if (x == Float.POSITIVE_INFINITY || x == Float.NEGATIVE_INFINITY)
                y = Float.NaN; //as string, "NaN" is shorter than "Infinity"
            else*/
                y = x;

            jsonGenerator.writeNumber(y);
        }
    }

    public static void writeArrayValues(double[] xx, JsonGenerator jsonGenerator) throws IOException {
        for (double x : xx) {
            double y;
            /*if (x == Float.POSITIVE_INFINITY || x == Float.NEGATIVE_INFINITY)
                y = Float.NaN; //as string, "NaN" is shorter than "Infinity"
            else*/
            y = x;

            jsonGenerator.writeNumber(y);
        }
    }

    public static String toJSONString(Object x) {
        return toJSONString(x, false);
    }
    public static String toJSONString(Object x, boolean pretty) {
        return new String(toJSONBytes(x, pretty));
    }

    public static byte[] toJSONBytes(Object x) {
        return toJSONBytes(x, false);
    }
    public static byte[] toJSONBytes(Object x, boolean pretty) {
        return toJSONBytes(x, pretty ? jsonWritersPretty.get() : jsonWritersCondensed.get());
    }

    public static byte[] toJSONBytes(Object x, ObjectWriter writer) {
        try {
            return writer.writeValueAsBytes(x);
        } catch (JsonProcessingException ex) {
            System.err.println(ex);
            return x.toString().getBytes();
        }
    }

    public final static ObjectMapper mapper = new CBORMapper();

    public static byte[] toBytes(Object x, Class c) {
        try {
            return mapper.writerFor(c).writeValueAsBytes(x);
        } catch (JsonProcessingException e) {
            logger.error("{}", e);
            return null;
        }
    }

    public static <X> X fromBytes(byte[] b, Class<? extends X> type) {
        try {
            return mapper.reader(type).readValue(b);
        } catch (IOException e) {
            logger.error("{}", e);
            return null;
        }
    }

    public static boolean toJSON(Object x, OutputStream out) {
        return toJSON(x, out, (char)0);
    }

    public static boolean toJSON(Object x, OutputStream out, char suffix) {
        try {
            json.writer().writeValue(out, x);
            if (suffix!=0)
                out.write(suffix);
            return true;
        } catch (Exception ex) {
            logger.error("toJSON", ex);
            return false;
        }
    }

    public static JsonNode fromJSON(String x) throws JsonProcessingException {
        return fromJSON(x, JsonNode.class);
    }

    public static <X> X fromJSON(String x, Class<? extends X> type) throws JsonProcessingException {
//        try {
            return json.readValue(x, type);
//        } catch (IOException ex) {
//            ex.printStackTrace();
//            return null;
//        }
    }
    public static <X> X fromJSON(byte[] j, Class<? extends X> type) {
        try {
            return json.readValue(j, type);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static String uuid64() {
        //return (String)jcog.Util.uuid128(); //escaped unicode results in long JSON strings, bad
        //return (String)jcog.Util.uuid64();
        return Base64.getEncoder().encodeToString(Longs.toByteArray(uuidRandom.nextLong()));
    }

    @NotNull
    public static String[] toStrings(ArrayNode a) {
        String[] ids = new String[a.size()];
        int j = 0;
        for (JsonNode x : a) {
            ids[j++] = x.textValue();
        }
        return ids;
    }
}


//    final public static ObjectMapper jsonAnnotated = new ObjectMapper()
//            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
//            .configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
//            .configure(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS, false)
//            .configure(JsonGenerator.Feature.ESCAPE_NON_ASCII, true)
//            .configure(JsonGenerator.Feature.QUOTE_NON_NUMERIC_NUMBERS, false)
//            .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
//
//    final public static ObjectMapper jsonFields = new ObjectMapper()
//            .enableDefaultTyping()
//            .configure(SerializationFeature.INDENT_OUTPUT, true)
//            .configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
//            .configure(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS, false)
//            .configure(JsonGenerator.Feature.QUOTE_FIELD_NAMES, true)
//            .configure(JsonGenerator.Feature.ESCAPE_NON_ASCII, true)
//            .configure(JsonGenerator.Feature.QUOTE_NON_NUMERIC_NUMBERS, true)
//            .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);


//    public static class BatchObjectMapper extends ObjectMapper {
//
//        /**
//         * re-uses the same JSON generator for an iterator of objects to be serialized
//         */
//        public void writeValuesNOTWORKINGYET(OutputStream out, Iterator<Object> objs, char separator) throws IOException {
//
//            if (!objs.hasNext()) return;
//
//            JsonGenerator gen = _jsonFactory.createGenerator(out, JsonEncoding.UTF8);
//
//            DefaultSerializerProvider pr = _serializerProvider(getSerializationConfig());
//
//
//            boolean hasNext;
//            do {
//                getSerializerProvider().defaultSerializeValue(objs.next(), gen);
//                //ser.serialize(objs.next(), gen, pr);
//
//                hasNext = objs.hasNext();
//                if (hasNext)
//                    gen.writeRaw(separator);
//
//            } while (hasNext);
//
//        }
//
//        public byte[] writeValueAsBytes(Object value, ByteArrayBuilder bb)
//                throws JsonProcessingException
//        {
//            try {
//                _configAndWriteValue(_jsonFactory.createGenerator(bb, JsonEncoding.UTF8), value);
//            } catch (JsonProcessingException e) { // to support [JACKSON-758]
//                throw e;
//            } catch (IOException e) { // shouldn't really happen, but is declared as possibility so:
//                throw JsonMappingException.fromUnexpectedIOE(e);
//            }
//            byte[] result = bb.toByteArray();
//            bb.reset();
//            return result;
//        }
//
//        public void writeValues(OutputStream out, Iterator<Object> objs, char separator) throws IOException {
//
//            if (!objs.hasNext()) return;
//
//            ByteArrayBuilder bb = new ByteArrayBuilder(_jsonFactory._getBufferRecycler());
//
//            boolean hasNext;
//            do {
//                out.write(writeValueAsBytes(objs.next(), bb));
//
//                hasNext = objs.hasNext();
//                if (hasNext)
//                    out.write(separator);
//
//            } while (hasNext);
//
//            bb.release();
//
//
//        }
//
//
//        public void writeArrayValues(OutputStream out, Iterator objs, char separator) throws IOException {
//            out.write('[');
//            writeValues(out, objs, separator);
//            out.write(']');
//        }
//    }

