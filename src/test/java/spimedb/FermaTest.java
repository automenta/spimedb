//package spimedb;
//
//import com.syncleus.ferma.FramedGraph;
//import org.apache.tinkerpop.gremlin.structure.Element;
//import org.junit.Test;
//
//import static spimedb.graph.NGraph.*;
//
///**
// * Created by me on 12/29/16.
// */
//public class FermaTest {
//
//    @Test
//    public void test1() {
//
//        try {
//            Class.forName("spimedb.NObject"); //hack
//        } catch (ClassNotFoundException e) {
//            e.printStackTrace();
//        }
//
//        FramedGraph g = newGraph();
//
//        NTag a = g.addFramedVertex(NTag.class);
//        NData b = g.addFramedVertex(NData.class);
//
//        a.setName("x");
//        b.setData(new NObject("y"));
//
//        a.addInstance(b);
//
//        System.out.println(a + " " + a.traverse(v -> v.outE().otherV()).toList(Element.class));
//        System.out.println(b);
//
//
//    }
//}
//
////package spimedb.graph;
////
////import com.syncleus.ferma.AbstractVertexFrame;
////import com.syncleus.ferma.DelegatingFramedGraph;
////import com.syncleus.ferma.FramedGraph;
////import com.syncleus.ferma.TEdge;
////import com.syncleus.ferma.annotations.Adjacency;
////import com.syncleus.ferma.annotations.Incidence;
////import com.syncleus.ferma.annotations.Property;
////import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
////import spimedb.NObject;
////
////import java.util.Iterator;
////
/////**
//// * Created by me on 12/29/16.
//// */
////public class NGraph {
////
////    public static FramedGraph newGraph() {
////
////        TinkerGraph g = TinkerGraph.open();
////
////        FramedGraph fg = new DelegatingFramedGraph<>(g, true,true);
////                //true, true);
////
////        return fg;
////    }
////
////
////
////
////    abstract public static class NData extends AbstractVertexFrame {
////        @Property("data")
////        public abstract NObject getData();
////        @Property("data")
////        public abstract void setData(NObject data);
////
////        @Override
////        public String toString() {
////            return super.toString() + "=" + getData().toString();
////        }
////    }
////
////    abstract public static class NTag extends AbstractVertexFrame {
////
////        @Property("name")
////        public abstract String getName();
////
////        @Property("name")
////        public abstract void setName(String name);
////        //
////        @Adjacency(label = "instance")
////        public abstract Iterator<NData> getInstances();
////
////        @Incidence(label = "instance")
////        public abstract TEdge addInstance(NData friend);
////
//////        @Incidence("knows")
//////        public abstract Iterator<Knows> getKnows();
////
//////
//////        public List<Person> getFriendsNamedBill() {
//////            return traverse((v) -> v.out("knows").has("name", "bill").toList(Person.class);
//////        }
////    }
////
////
////}
