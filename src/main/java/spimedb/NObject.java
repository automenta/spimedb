package spimedb;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import spimedb.index.rtree.PointND;
import spimedb.util.JSON;

import java.io.IOException;
import java.io.Serializable;
import java.util.function.BiConsumer;

/**
 * Created by me on 1/18/17.
 */
@JsonSerialize(using = NObject.NObjectSerializer.class)
public interface NObject extends Serializable {


    String id();

    String name();

    String[] tags();

    void forEach(BiConsumer<String, Object> each);

    <X> X get(String tag);

    PointND min();

    PointND max();

    default String stringify() {
        return new String(JSON.toJSON(this));
    }

    default boolean bounded() {
        PointND min = min();
        return (min != null && !min.isNegativeInfinity() && !max().isPositiveInfinity());
    }

    static boolean equalsDeep(NObject a, NObject b) {
        return a.toString().equals(b.toString());
    }

    default String description() {
        Object d = get("_");
        return d == null ? "" : d.toString();
    }


    String ID = "I";
    String NAME = "N";
    String TAG = ">";
    String BOUND = "@";
    String DESC = "_";

    /** intensional inheritance */
    String INH = "inh";

    class NObjectSerializer extends JsonSerializer<NObject> {

        @Override
        public void serialize(NObject o, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
            jsonGenerator.writeStartObject();
            {

                jsonGenerator.writeStringField(ID, o.id());

                String name = o.name();
                if (name != null)
                    jsonGenerator.writeStringField(NAME, name);


                writeOtherFields(o, jsonGenerator);

                writeBounds(o, jsonGenerator);

            }
            jsonGenerator.writeEndObject();
        }

        protected void writeBounds(NObject o, JsonGenerator jsonGenerator) throws IOException {
            //zip the min/max bounds
            if (o.bounded()) {
                PointND min = o.min();


                PointND max = o.max();
                boolean point = max == null || min.equals(max);
                if (!point) {
                    jsonGenerator.writeFieldName(BOUND);
                    jsonGenerator.writeStartArray();

                    if (!max.equals(min)) {
                        int dim = min.dim();
                        for (int i = 0; i < dim; i++) {
                            float a = min.coord[i];
                            float b = max.coord[i];
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

        protected void writeOtherFields(NObject o, JsonGenerator jsonGenerator) {
            o.forEach((fieldName, pojo) -> {
                if (pojo == null)
                    return;
                try {
                    jsonGenerator.writeObjectField(fieldName, pojo);
                } catch (IOException e) {
                    //e.printStackTrace();
                }
            });
        }

    }
}
