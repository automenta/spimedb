package spimedb;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import spimedb.graph.MapGraph;
import spimedb.graph.VertexContainer;
import spimedb.graph.VertexIncidence;

import java.util.Map;
import java.util.function.BiConsumer;

/**
 * graph decorator
 */
@JsonSerialize(using = NObject.NObjectSerializer.class)
public class GraphedNObject extends ProxyNObject {

    public final MapGraph<String, String> graph;


    GraphedNObject(MapGraph<String, String> graph) {
        this.graph = graph;
    }

    GraphedNObject(MapGraph<String, String> graph, NObject n) {
        this(graph);
        set(n);
    }

    protected static boolean includeKey(String key) {
        return !key.equals(TAG);
    }

    @Override
    public void forEach(BiConsumer<String, Object> each) {
        n.forEach((k, v) -> {
            if (includeKey(k)) //HACK filter out tag field because the information will be present in the graph
                each.accept(k, v);
        });

        VertexContainer<String, String> v = graph.vertex(id(), false);
        if (v != null) {
            Map<String, VertexIncidence<String>> boundary = v.incidence();
            boundary.forEach(each);
        }
    }

}
