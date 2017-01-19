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
@JsonSerialize(using= AbstractNObject.NObjectSerializer.class)
public interface AbstractNObject extends Serializable {


    String id();
    String name();
    String[] tags();

    void forEach(BiConsumer<String, Object> each);

    default String description() {
        Object d = get("_");
        return d == null ? "" : d.toString();
    }

    <X> X get(String tag);

    PointND min();

    PointND max();

    boolean bounded();


    final class NObjectSerializer extends JsonSerializer<AbstractNObject> {

        @Override
        public void serialize(AbstractNObject o, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
            jsonGenerator.writeStartObject();
            {
                jsonGenerator.writeStringField("I", o.id());

                String name = o.name();
                if (name != null)
                    jsonGenerator.writeStringField("N", name);

                String[] tag = o.tags();
                if (tag != null && tag.length > 0) {
                    jsonGenerator.writeObjectField(">", tag);
                }


                o.forEach((fieldName, pojo) -> {
                    if (pojo==null)
                        return;
                    try {
                        jsonGenerator.writeObjectField(fieldName, pojo);
                    } catch (IOException e) {
                        //e.printStackTrace();
                    }
                });


                //zip the min/max bounds
                PointND min = o.min();
                if (min!=null) {

                    PointND max = o.max();
                    boolean point = max==null || min.equals(max);
                    if (!point || !min.isNaN()) {
                        jsonGenerator.writeFieldName("@");
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
            jsonGenerator.writeEndObject();
        }

    }
}
