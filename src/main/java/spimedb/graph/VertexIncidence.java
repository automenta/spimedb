package spimedb.graph;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.eclipse.collections.impl.list.mutable.FastList;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

/**
 * Created by me on 1/19/17.
 */
@JsonSerialize(using = VertexIncidence.Serializer.class)
public class VertexIncidence<V> implements Serializable {
    @JsonProperty(">")
    public final List<V> in = new FastList();
    @JsonProperty("<")
    public final List<V> out = new FastList();

    static class Serializer extends JsonSerializer<VertexIncidence> {

        @Override public void serialize(VertexIncidence x, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            boolean inEmpty = x.in.isEmpty();
            boolean outEmpty = x.out.isEmpty();
            if (!inEmpty || !outEmpty) {
                gen.writeStartObject();
                if (!inEmpty)
                    gen.writeObjectField(">", x.in);
                if (!outEmpty)
                    gen.writeObjectField("<", x.out);
                gen.writeEndObject();
            } else {
                gen.writeNull();
            }
        }
    }
}
