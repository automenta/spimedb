package spimedb;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jcog.tree.rtree.point.DoubleND;
import spimedb.util.JSON;

import java.io.IOException;
import java.io.Serializable;
import java.util.function.BiConsumer;

/**
 * Created by me on 1/18/17.
 */
@JsonSerialize(using = NObject.NObjectSerializer.class)
public interface NObject extends Serializable {

    //public final static String POINT = ".";

    String id();

    String name();

    String[] tags();

    void forEach(BiConsumer<String, Object> each);

    <X> X get(String tag);

    default <X> X getOr(String key, X ifMissing) {
        Object val = get(key);
        if (val == null)
            return ifMissing;
        return (X)val;
    }

    DoubleND min();

    DoubleND max();

    default String toJSONString() {
        return toJSONString(false);
    }

    default String toJSONString(boolean pretty) {
        return new String(JSON.toJSONBytes(this, pretty));
    }

    default boolean bounded() {
        DoubleND min = min();
        return (min != null && min.dim() > 0 /*&& !min.isInfNeg() && !max().isInfPos()*/);
    }


    default String description() {
        Object d = get(NObject.DESC);
        return d == null ? "" : d.toString();
    }


    String ID = "I";
    String NAME = "N";

    String QUERY = "?";


    /** relevance score (dynamic, context-dependent) */
    String SCORE = "*";

    String URL = "url"; //TODO shorten to 'u'

    String ICON = "icon"; //icon, binary data
    String DATA = "data"; //blob data

    String TAG = ">";
    String CONTENT = "<";

    String BOUND = "@";
    String DESC = "_";

    String LINESTRING = "g-";
    String POLYGON = "g*";

    /** content-type, like MIME */
    String TYPE = "T";

    /** intensional inheritance */
    String INH = "inh";

    String TAG_PUBLIC = "public";

    default boolean has(String key) {
        return get(key)!=null;
    }

    default boolean equalsDeep(NObject n) {
        //TODO more efficient comparison

        return toString().equals(n.toString());
        //return toString().equals(n.toString());
    }


    static MutableNObject fromJSON(String json) throws IOException {
        JsonNode x = JSON.fromJSON(json, JsonNode.class);
        JsonNode idNode = x.get(ID);
        if (idNode == null)
            throw new IOException("invalid nobject JSON: " +  json);

        MutableNObject y = new MutableNObject(idNode.textValue());
        x.fields().forEachRemaining(e -> {
            String k = e.getKey();

            if (k.equals(ID)) {
                return; //ID already processed
            }

            JsonNode v = e.getValue();
            Object w = v;
            if (v.isTextual())
                w = v.textValue();
            else if (v.isNumber())
                w = v.numberValue();
            //TODO else ...

            y.put(k, w);
        });

        return y;
    }

    class NObjectSerializer extends JsonSerializer<NObject> {

        @Override
        public void serialize(NObject o, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
            jsonGenerator.writeStartObject();
            {

                jsonGenerator.writeStringField(ID, o.id());

                o.forEach((fieldName, pojo) -> {
                    if (pojo == null)
                        return;
                    try {
                        jsonGenerator.writeObjectField(fieldName, pojo);
                    } catch (IOException e) {
                        //e.printStackTrace();
                    }
                });

                writeBounds(o, jsonGenerator);

            }
            jsonGenerator.writeEndObject();
        }

        protected static void writeBounds(NObject o, JsonGenerator jsonGenerator) throws IOException {
            //zip the min/max bounds
            if (o.bounded()) {
                DoubleND min = o.min();
                DoubleND max = o.max();
                boolean point = max == null || min.equals(max);
                if (!point) {
                    jsonGenerator.writeFieldName(BOUND);
                    jsonGenerator.writeStartArray();

                    if (!max.equals(min)) {
                        int dim = min.dim();
                        for (int i = 0; i < dim; i++) {
                            double a = min.coord[i];
                            double b = max.coord[i];
                            if (a == b) {
                                jsonGenerator.writeNumber(a);
                            } else {
                                if (a == Float.NEGATIVE_INFINITY && b == Float.POSITIVE_INFINITY) {
                                    jsonGenerator.writeNumber(Float.NaN);
                                } else {
                                    jsonGenerator.writeStartArray();
                                    jsonGenerator.writeNumber(a);
                                    jsonGenerator.writeNumber(b);
                                    jsonGenerator.writeEndArray();
                                }
                            }
                        }
                    } else {
                        JSON.writeArrayValues(min.coord, jsonGenerator);
                    }
                    jsonGenerator.writeEndArray();
                }

            }
        }

    }
}
