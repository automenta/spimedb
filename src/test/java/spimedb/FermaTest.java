package spimedb;

import com.google.common.collect.Lists;
import com.syncleus.ferma.AbstractVertexFrame;
import com.syncleus.ferma.DelegatingFramedGraph;
import com.syncleus.ferma.FramedGraph;
import com.syncleus.ferma.TEdge;
import com.syncleus.ferma.annotations.Adjacency;
import com.syncleus.ferma.annotations.Incidence;
import com.syncleus.ferma.annotations.Property;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.junit.Test;

import java.util.Iterator;

/**
 * Created by me on 12/29/16.
 */
public class FermaTest {

    abstract public static class NData extends AbstractVertexFrame {
        @Property("data")
        public abstract NObject getData();
        @Property("data")
        public abstract void setData(NData data);
    }

    abstract public static class NTag extends AbstractVertexFrame {

        @Property("name")
        public abstract String getName();

        @Property("name")
        public abstract void setName(String name);
//
        @Adjacency(label = "instance")
        public abstract Iterator<NData> getInstances();

        @Incidence(label = "instance")
        public abstract TEdge addInstance(NData friend);

//        @Incidence("knows")
//        public abstract Iterator<Knows> getKnows();

//
//        public List<Person> getFriendsNamedBill() {
//            return traverse((v) -> v.out("knows").has("name", "bill").toList(Person.class);
//        }
    }

    @Test
    public void test1() {
        TinkerGraph g = TinkerGraph.open();

        FramedGraph fg = new DelegatingFramedGraph<>(g, Lists.newArrayList(NTag.class, NData.class));

        NTag a = fg.addFramedVertex(NTag.class);
        NData b = fg.addFramedVertex(NData.class);

        a.addInstance(b);

        System.out.println(a + " " + a.traverse(v -> v.outE().otherV()).toList(Object.class));
        System.out.println(b);


    }
}

