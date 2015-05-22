//package automenta.climatenet.data.graph;
//
//import com.google.common.collect.Lists;
//import com.google.common.collect.Sets;
//import org.mapdb.*;
//
//import java.io.DataInput;
//import java.io.DataOutput;
//import java.io.File;
//import java.io.IOException;
//import java.util.*;
//
///** adapted from https://github.com/jankotek/blueprints */
//public class MapDBGraph/*<V extends NObject>*/ {// implements DirectedGraph<V, String> {
//
//
//    protected final DB db;
//    protected final Engine engine;
//
//    protected final boolean useUserIds;
//
//    protected final Set<Long> vertices;
//    protected final Set<Long> edges;
//
//    protected final Map<Object,Long> vertices2recid;
//    protected final Map<Object,Long> edges2recid;
//
//
//    protected final NavigableMap<Fun.Tuple2<Long,String>,Object> verticesProps;
//    protected final NavigableMap<Fun.Tuple2<Long,String>,Object> edgesProps;
//    protected final NavigableSet<Fun.Tuple2<String,Long>> edgesLabels;
//
//    protected final NavigableSet<Fun.Tuple3<String,Object,Long>> verticesIndex;
//    protected final NavigableSet<Fun.Tuple3<String,Object,Long>> edgesIndex;
//
//    protected final NavigableSet<Fun.Tuple4<String,String,Object,Long>> verticesIndex2;
//    protected final NavigableSet<Fun.Tuple4<String,String,Object,Long>> edgesIndex2;
//
//    protected final Set<String> verticesKeys;
//    protected final Set<String> edgesKeys;
//
//    protected final Set<String> verticesKeys2;
//    protected final Set<String> edgesKeys2;
//
//
//    /** key:vertice id, direction (out=true), edge label, edge id*/
//    protected final NavigableSet<Fun.Tuple4<Long,Boolean,String,Long>> edges4vertice;
//
//    public MapDBGraph(DBMaker dbMaker) {
//        this(dbMaker, true);
//    }
//
//
//    /**
//     * Direction is used to denote the direction of an edge or location of a vertex on an edge.
//     * For example, gremlin--knows--&gt;rexster is an OUT edge for Gremlin and an IN edge for Rexster.
//     * Moreover, given that edge, Gremlin is the OUT vertex and Rexster is the IN vertex.
//     *
//     * @author Marko A. Rodriguez (http://markorodriguez.com)
//     */
//    public enum Direction {
//
//        OUT, IN, BOTH;
//
//        public static final Direction[] proper = new Direction[]{OUT, IN};
//
//        public Direction opposite() {
//            if (this.equals(OUT))
//                return IN;
//            else if (this.equals(IN))
//                return OUT;
//            else
//                return BOTH;
//        }
//    }
//
//
//    public class Vertex {
//
//
//        protected final Object id;
//
//        public Vertex(Object id) {
//            this.id = id;
//        }
//
//
//        public Iterable<Edge> getEdges(Direction direction, String... labels) {
//            List<Edge> ret = new ArrayList<Edge>();
//            if(labels==null || labels.length==0) labels = new String[]{null};
//            Long recid = vertexRecid(id);
//            assert(recid!=null);
//            for(String label:labels){
//
//                if(Direction.BOTH == direction){
//                    for(Long recid2 : Fun.filter( edges4vertice, recid, true, label)){
//                        ret.add(engine.get(recid2,EDGE_SERIALIZER));
//                    }
//                    for(Long recid2 : Fun.filter( edges4vertice, recid, false, label)){
//                        ret.add(engine.get(recid2,EDGE_SERIALIZER));
//                    }
//                }else{
//                    for(Long recid2 : Fun.filter( edges4vertice, recid, direction == Direction.OUT, label)){
//                        ret.add(engine.get(recid2,EDGE_SERIALIZER));
//                    }
//                }
//            }
//            return ret;
//        }
//
//
//        public Iterable<Vertex> getVertices(final Direction direction, String... labels) {
//            return new VerticesFromEdgesIterable(this,direction,labels);
//        }
//
//        @Override
//        public String toString() {
//            return "V[" + id.toString() + ']';
//        }
//
//        public Edge addEdge(String label, Vertex inVertex) {
//            return MapDBGraph.this.addEdge(null,this,inVertex,label);
//        }
//
//
//
//
//        public void remove() {
//            Long recid = vertexRecid(id);
//            if(!vertices.contains(recid)) throw new IllegalStateException("vertex not found");
//
//            Iterator<Map.Entry<Fun.Tuple2<Long,String>,Object>> propsIter =
//                    ((NavigableMap)verticesProps).subMap(Fun.t2(id,null),Fun.t2(id,Fun.HI())).entrySet().iterator();
//
//            while(propsIter.hasNext()){
//                Map.Entry<Fun.Tuple2<Long,String>,Object> n = propsIter.next();
//                if(verticesKeys.contains(n.getKey().b)){
//                    verticesIndex.remove(Fun.t3(n.getKey().b,n.getValue(),n.getKey().a));
//                }
//                propsIter.remove();
//            }
//
//
//            //remove all relevant recids from indexes
//            //TODO linear scan, add reverse index
//            Iterator<Fun.Tuple4<String,String,Object,Long>> indexIter = verticesIndex2.iterator();
//            while(indexIter.hasNext()){
//                if(indexIter.next().d.equals(id))
//                    indexIter.remove();
//            }
//
//            engine.delete(recid,VERTEX_SERIALIZER);
//            vertices.remove(recid);
//
//            //remove related edges
//            for(Edge e:getEdges(Direction.OUT))e.remove();
//            for(Edge e:getEdges(Direction.IN))e.remove();
//
//        }
//
//
//        public <T> T getProperty(String key) {
//            return (T) verticesProps.get(Fun.t2(vertexRecid(id), key));
//        }
//
//
//        public Set<String> getPropertyKeys() {
//            Set<String> ret = new HashSet<String>();
//            Long recid = vertexRecid(id);
//            for(String s:Fun.filter(verticesProps.navigableKeySet(), recid)){
//                ret.add(s);
//            }
//            return ret;
//        }
//
//
//        public void setProperty(String key, Object value) {
//            if(key==null||"".equals(key)||"id".equals(key)
//                    ||"label".equals(key)) throw new IllegalArgumentException();
//            Long recid = vertexRecid(id);
//            Object oldVal = verticesProps.put(Fun.t2(recid,key),value);
//
//            if(verticesKeys.contains(key)){
//                //remove old value from index if exists
//                if(oldVal!=null) verticesIndex.remove(Fun.t3(key,oldVal,recid));
//                //put new value
//                verticesIndex.add(Fun.t3(key,value,recid));
//            }
//        }
//
//
//        public <T> T removeProperty(String key) {
//            Long recid = vertexRecid(id);
//            T ret = (T) verticesProps.remove(Fun.t2(recid, key));
//            if(verticesKeys.contains(key)){
//                //remove from index
//                //remove old value from index if exists
//                if(ret!=null) verticesIndex.remove(Fun.t3(key,ret,recid));
//            }
//
//            return ret;
//        }
//
//
//
//        public Object getId() {
//            return id;
//        }
//
//
//        public int hashCode() {
//            return id.hashCode();
//        }
//
//
//        public boolean equals(Object o) {
//            if (this == o) return true;
//            //if (!(o instanceof Vertex)) return false;
//            if (!id.equals(((Vertex) o).id)) return false;
//            return true;
//        }
//
//
////        public String toString() {
////            return StringFactory.vertexString(this);
////        }
//
//
//    }
//
//    public class VerticesFromEdgesIterable implements Iterable<Vertex> {
//
//        private final Iterable<Edge> iterable;
//        private final Direction direction;
//        private final Vertex vertex;
//
//        public VerticesFromEdgesIterable(final Vertex vertex, final Direction direction, final String... labels) {
//            this.direction = direction;
//            this.vertex = vertex;
//            this.iterable = vertex.getEdges(direction, labels);
//        }
//
//        public Iterator<Vertex> iterator() {
//            return new Iterator<Vertex>() {
//                final Iterator<Edge> itty = iterable.iterator();
//
//                public void remove() {
//                    this.itty.remove();
//                }
//
//                public boolean hasNext() {
//                    return this.itty.hasNext();
//                }
//
//                public Vertex next() {
//                    if (direction.equals(Direction.OUT)) {
//                        return this.itty.next().getVertex(Direction.IN);
//                    } else if (direction.equals(Direction.IN)) {
//                        return this.itty.next().getVertex(Direction.OUT);
//                    } else {
//                        final Edge edge = this.itty.next();
//                        if (edge.getVertex(Direction.IN).equals(vertex))
//                            return edge.getVertex(Direction.OUT);
//                        else
//                            return edge.getVertex(Direction.IN);
//                    }
//                }
//            };
//        }
//    }
//
//
//    protected final Serializer<Vertex> VERTEX_SERIALIZER = new Serializer<Vertex>() {
//
//        public void serialize(DataOutput out, Vertex value) throws IOException {
//            if(value.id==null) return;
//            Serializer.BASIC.serialize(out,value.id);
//        }
//
//
//        public Vertex deserialize(DataInput in, int available) throws IOException {
//            if(available==0) return VERTEX_EMPTY;
//            return new Vertex(Serializer.BASIC.deserialize(in,available));
//        }
//
//
//        public int fixedSize() {
//            return -1;
//        }
//    };
//
//    public class Edge {
//
//        protected final Object id;
//        protected final long in,out;
//        protected final String label;
//
//
//        public Edge(Object id, long out, long in, String label) {
//            this.id = id;
//            this.out = out;
//            this.in = in;
//            if(label==null && id!=null) throw new IllegalArgumentException();
//            this.label = label;
//        }
//
//
//
//        public Vertex getVertex(Direction direction) throws IllegalArgumentException {
//            if (direction.equals(Direction.IN))
//                return engine.get(in,VERTEX_SERIALIZER);
//            else if (direction.equals(Direction.OUT))
//                return engine.get(out,VERTEX_SERIALIZER);
//            else
//                throw new RuntimeException("Direction BOTH unsupported"); //ExceptionFactory.bothIsNotSupported();
//        }
//
//        public Vertex source() { return getVertex(Direction.OUT);         }
//        public Vertex target() { return getVertex(Direction.IN);        }
//
//
//        public String getLabel() {
//            return label;
//        }
//
//        @Override
//        public String toString() {
//
//
//            return getVertex(Direction.OUT) + "->" + label + "[" + id + "]->" + getVertex(Direction.IN);
//        }
//
//        public void remove() {
//            Long recid = edgeRecid(id);
//            if(!edges.contains(recid)) throw new IllegalStateException("edge not found");
//
//            Iterator<Map.Entry<Fun.Tuple2<Long,String>,Object>> propsIter =
//                    ((NavigableMap)edgesProps).subMap(Fun.t2(id,null),Fun.t2(id,Fun.HI())).entrySet().iterator();
//
//            while(propsIter.hasNext()){
//                Map.Entry<Fun.Tuple2<Long,String>,Object> n = propsIter.next();
//                if(edgesKeys.contains(n.getKey().b)){
//                    edgesIndex.remove(Fun.t3(n.getKey().b,n.getValue(),n.getKey().a));
//                }
//                propsIter.remove();
//            }
//
//
//            //remove all relevant recids from indexes
//            //TODO linear scan, add reverse index
//            Iterator<Fun.Tuple4<String,String,Object,Long>> indexIter = edgesIndex2.iterator();
//            while(indexIter.hasNext()){
//                if(indexIter.next().d.equals(id))
//                    indexIter.remove();
//            }
//
//            engine.delete(recid,EDGE_SERIALIZER);
//            edges.remove(recid);
//            edges4vertice.remove(Fun.t4(out,true,label,id));
//            edges4vertice.remove(Fun.t4(in,false,label,id));
//        }
//
//
//
//        public <T> T getProperty(String key) {
//            return (T) edgesProps.get(Fun.t2(edgeRecid(id), key));
//        }
//
//
//        public Set<String> getPropertyKeys() {
//            Long recid = edgeRecid(id);
//            Set<String> ret = new HashSet<String>();
//            for(String s:Fun.filter(edgesProps.navigableKeySet(),recid)){
//                ret.add(s);
//            }
//            return ret;
//        }
//
//
//        public void setProperty(String key, Object value) {
//            if(key==null||"".equals(key)||"id".equals(key)
//                    ||"label".equals(key)) throw new IllegalArgumentException();
//            Long recid = edgeRecid(id);
//            Object oldVal = edgesProps.put(Fun.t2(recid,key),value);
//
//            if(edgesKeys.contains(key)){
//                //remove old value from index if exists
//                if(oldVal!=null) edgesIndex.remove(Fun.t3(key,oldVal,recid));
//                //put new value
//                edgesIndex.add(Fun.t3(key,value,recid));
//            }
//        }
//
//
//        public <T> T removeProperty(String key) {
//            Long recid = edgeRecid(id);
//            T ret = (T) edgesProps.remove(Fun.t2(recid, key));
//            if(edgesKeys.contains(key)){
//                //remove from index
//                //remove old value from index if exists
//                if(ret!=null) edgesIndex.remove(Fun.t3(key,ret,recid));
//            }
//
//            return ret;
//        }
//
//
//
//        public Object getId() {
//            return id;
//        }
//
//
//        public int hashCode() {
//            return id.hashCode();
//        }
//
//
//        public boolean equals(Object o) {
//            if (this == o) return true;
//            //if (!(o instanceof Edge)) return false;
//            if (!id.equals(((Edge) o).id)) return false;
//            return true;
//        }
//
//
////        public String toString() {
////            return StringFactory.edgeString(this);
////        }
//
//    }
//
//    protected final Serializer<Edge> EDGE_SERIALIZER = new Serializer<Edge>() {
//
//        public void serialize(DataOutput out, Edge value) throws IOException {
//            if(value.id==null) return;
//            Serializer.BASIC.serialize(out,value.id);
//            DataOutput2.packLong(out,value.out);
//            DataOutput2.packLong(out,value.in);
//            out.writeUTF(value.getLabel());
//        }
//
//
//        public Edge deserialize(DataInput in, int available) throws IOException {
//            if(available==0) return EDGE_EMPTY;
//            return new Edge(
//                    Serializer.BASIC.deserialize(in,available),
//                    DataInput2.unpackLong(in),DataInput2.unpackLong(in),in.readUTF());
//        }
//
//
//        public int fixedSize() {
//            return -1;
//        }
//    };
//
//    protected final Edge EDGE_EMPTY = new Edge(null,0L,0L,null);
//
//
//    public MapDBGraph(String fileName, boolean useUserIds) {
//        this( new File(fileName).getParentFile().mkdirs() || true? //always true, but necessary to mkdirts in constructor
//                DBMaker.newFileDB(new File(fileName)) :null
//                //.transactionDisable()
//                ,useUserIds);
//
//    }
//
//    public MapDBGraph(DBMaker dbMaker, boolean useUserIds) {
//        db = dbMaker.make();
//        engine = db.getEngine();
//        this.useUserIds= useUserIds;
//
//        vertices2recid = !useUserIds? null:
//                db.createHashMap("vertices2recid").<Object, Long>makeOrGet();
//
//        edges2recid = !useUserIds? null:
//                db.createHashMap("edges2recid").<Object, Long>makeOrGet();
//
//
//        vertices = db.createTreeSet("vertices")
//                .counterEnable()
//                .serializer(BTreeKeySerializer.ZERO_OR_POSITIVE_LONG)
//                .makeOrGet();
//
//        edges = db.createTreeSet("edges")
//                .counterEnable()
//                .serializer(BTreeKeySerializer.ZERO_OR_POSITIVE_LONG)
//                .makeOrGet();
//
//        edgesLabels = db.createTreeSet("edgesLabels")
//                .serializer(BTreeKeySerializer.TUPLE2)
//                .makeOrGet();
//
//
//        edges4vertice = db.createTreeSet("edges4vertice")
//                .serializer(BTreeKeySerializer.TUPLE4)
//                .makeOrGet();
//
//        edgesProps = db.createTreeMap("edgesProps")
//                .keySerializer(BTreeKeySerializer.TUPLE2)
//                .valuesOutsideNodesEnable()
//                .makeOrGet();
//
//        verticesProps = db.createTreeMap("verticesProps")
//                .keySerializer(BTreeKeySerializer.TUPLE2)
//                .valuesOutsideNodesEnable()
//                .makeOrGet();
//
//
//        verticesIndex = db.createTreeSet("verticesIndex")
//                .serializer(BTreeKeySerializer.TUPLE3)
//                .makeOrGet();
//
//        edgesIndex = db.createTreeSet("edgesIndex")
//                .serializer(BTreeKeySerializer.TUPLE3)
//                .makeOrGet();
//
//        verticesKeys = db.createTreeSet("verticesKeys")
//                .serializer(BTreeKeySerializer.STRING)
//                .makeOrGet();
//
//        edgesKeys = db.createTreeSet("edgesKeys")
//                .serializer(BTreeKeySerializer.STRING)
//                .makeOrGet();
//
//
//        verticesIndex2 = db.createTreeSet("verticesIndex2")
//                .serializer(BTreeKeySerializer.TUPLE4)
//                .makeOrGet();
//
//        edgesIndex2 = db.createTreeSet("edgesIndex2")
//                .serializer(BTreeKeySerializer.TUPLE4)
//                .makeOrGet();
//
//        verticesKeys2 = db.createTreeSet("verticesKeys2")
//                .serializer(BTreeKeySerializer.STRING)
//                .makeOrGet();
//
//        edgesKeys2 = db.createTreeSet("edgesKeys2")
//                .serializer(BTreeKeySerializer.STRING)
//                .makeOrGet();
//
//    }
//
//
//    protected Long vertexRecid(Object id){
//        if(useUserIds){
//            Long ret =  vertices2recid.get(id);
//            if(ret == null && id instanceof String){
//                try{
//                    ret = vertices2recid.get(Long.valueOf((String)id));
//                    if(ret==null) ret = vertices2recid.get(Integer.valueOf((String)id));
//                    if(ret==null) ret = vertices2recid.get(Short.valueOf((String)id));
//                    if(ret==null) ret = vertices2recid.get(Double.valueOf((String)id));
//                    if(ret==null) ret = vertices2recid.get(Float.valueOf((String)id));
//                }catch(NumberFormatException e){
//                    return null;
//                }
//            }
//            return ret;
//        }
//        if(id instanceof String)try{
//            return Long.valueOf((String)id);
//        }catch(NumberFormatException e){
//            return null;
//        }
//
//        if(!(id instanceof Long)) return  null;
//        return (Long)id;
//    }
//
//    protected Long edgeRecid(Object id){
//        if(useUserIds){
//            Long ret =  edges2recid.get(id);
//            if(ret == null && id instanceof String){
//                try{
//                    ret = edges2recid.get(Long.valueOf((String)id));
//                    if(ret==null) ret = edges2recid.get(Integer.valueOf((String)id));
//                    if(ret==null) ret = edges2recid.get(Short.valueOf((String)id));
//                    if(ret==null) ret = edges2recid.get(Double.valueOf((String)id));
//                    if(ret==null) ret = edges2recid.get(Float.valueOf((String)id));
//                }catch(NumberFormatException e){
//                    return null;
//                }
//            }
//            return ret;
//        }
//
//        if(id instanceof String)try{
//            return Long.valueOf((String)id);
//        }catch(NumberFormatException e){
//            return null;
//        }
//
//        if(!(id instanceof Long)) return  null;
//        return (Long)id;
//    }
//
//    protected final Vertex VERTEX_EMPTY = new Vertex(null);
//
//
//    public Vertex addVertex(Object id) {
//        //preallocate recid
//        Long recid = engine.put(VERTEX_EMPTY,VERTEX_SERIALIZER);
//
//        if(id==null) id=recid;
//        if(useUserIds){
//            vertices2recid.put(id,recid);
//        }else{
//            id = recid;
//        }
//
//        //and insert real value
//        Vertex v = new Vertex(id);
//        engine.update(recid, v, VERTEX_SERIALIZER);
//        vertices.add(recid);
//        return v;
//    }
//
//
//    public Vertex getVertex(Object id) {
//        if(id==null) throw new IllegalArgumentException();
//        Long recid = vertexRecid(id);
//
//        if(recid==null || !vertices.contains(recid))return null;
//        return engine.get(recid, VERTEX_SERIALIZER);
//    }
//
//    public Vertex getVertex(Object id, boolean addIfNonExisting) {
//        Vertex v = getVertex(id);
//        if (v == null) {
//             v = addVertex(id);
//        }
//        return v;
//    }
//
//    public void removeVertex(Vertex vertex) {
//        vertex.remove();
//    }
//
//
//    public List<Vertex> getVertexList() {
//        return Lists.newArrayList(getVertices());
//    }
//    public Set<Vertex> getVertexSet() {
//        return Sets.newHashSet(getVertices());
//    }
//    public List<Edge> getEdgeList() {
//        return Lists.newArrayList(getEdges());
//    }
//    public Set<Edge> getEdgeSet() {
//        return Sets.newHashSet(getEdges());
//    }
//
//    public Iterable<Vertex> getVertices() {
//        return new Iterable<Vertex>() {
//
//
//            public Iterator<Vertex> iterator() {
//                final Iterator<Long> vertIter = vertices.iterator();
//                return new MVertexRecidIterator(vertIter);
//            }
//        };
//    }
//
//
//    public Iterable<Vertex> getVertices(final String key, final Object value) {
//
//        return new Iterable<Vertex>() {
//
//            public Iterator<Vertex> iterator() {
//
//                boolean indexed = verticesKeys.contains(key);
//
//                Set<Long> longs=null;
//
//                if(!indexed){
//                    //traverse all
//                    longs = new HashSet<Long>();
//                    for(Map.Entry<Fun.Tuple2<Long,String>,Object> e:verticesProps.entrySet()){
//                        if(e.getKey().b.equals(key)&& e.getValue().equals(value))
//                            longs.add(e.getKey().a);
//                    }
//                }
//
//                final Iterator<Long> i = indexed?
//                        Fun.filter(verticesIndex,key,value).iterator():longs.iterator();
//
//
//                return new MVertexRecidIterator(i);
//            }
//        };
//
//    }
//
//    public Edge addEdge(Vertex outVertex, Vertex inVertex, String label) {
//        return addEdge(null, outVertex, inVertex, label);
//    }
//
//    public Edge addEdge(Object id, Vertex outVertex, Vertex inVertex, String label) {
//        Long recid = engine.put(EDGE_EMPTY, EDGE_SERIALIZER);
//        if(id==null) id = recid;
//        if(useUserIds){
//            edges2recid.put(id,recid);
//        }else{
//            id = recid;
//        }
//
//        Edge edge = new Edge(id,vertexRecid(outVertex.getId()), vertexRecid(inVertex.getId()),label);
//        edges.add(recid);
//        engine.update(recid,edge,EDGE_SERIALIZER);
//        edges4vertice.add(Fun.t4(edge.out,true,label,recid));
//        edges4vertice.add(Fun.t4(edge.in,false,label,recid));
//        edgesLabels.add(Fun.t2(label, recid));
//        return edge;
//    }
//
//
//    public Edge getEdge(Object id) {
//        if(id==null) throw new IllegalArgumentException();
//
//        Long recid = edgeRecid(id);
//
//        if(recid==null || !edges.contains(recid))return null;
//        return engine.get(recid,EDGE_SERIALIZER);
//    }
//
//
//    public void removeEdge(Edge edge) {
//        edge.remove();
//    }
//
//
//    public class MVertexRecidIterator implements Iterator<Vertex>{
//        protected final Iterator<Long> i;
//
//        public MVertexRecidIterator(Iterator<Long> i) {
//            this.i = i;
//        }
//
//
//        public boolean hasNext() {
//            return i.hasNext();
//        }
//
//
//        public Vertex next() {
//            return engine.get(i.next(), VERTEX_SERIALIZER);
//        }
//
//
//        public void remove() {
//            i.remove();
//        }
//    }
//
//    public class MEdgeRecidIterator implements Iterator<Edge>{
//        protected final Iterator<Long> i;
//
//        public MEdgeRecidIterator(Iterator<Long> i) {
//            this.i = i;
//        }
//
//
//        public boolean hasNext() {
//            return i.hasNext();
//        }
//
//
//        public Edge next() {
//            return engine.get(i.next(),EDGE_SERIALIZER);
//        }
//
//
//        public void remove() {
//            i.remove();
//        }
//    }
//
//
//    public Iterable<Edge> getEdges() {
//        return new Iterable<Edge>() {
//
//
//            public Iterator<Edge> iterator() {
//                return new MEdgeRecidIterator(edges.iterator());
//            }
//        };
//    }
//
//
//    public Iterable<Edge> getEdges(final String key, final Object value) {
//
//        if ("label".equals(key)) return new Iterable<Edge>() {
//
//            public Iterator<Edge> iterator() {
//                return new MEdgeRecidIterator(Fun.filter(edgesLabels, (String) value).iterator());
//            }
//        };
//
//        else return new Iterable<Edge>() {
//
//            public Iterator<Edge> iterator() {
//
//                boolean indexed = edgesKeys.contains(key);
//
//                Set<Long> longs=null;
//
//                if(!indexed){
//                    //traverse all
//                    longs = new HashSet<Long>();
//                    for(Map.Entry<Fun.Tuple2<Long,String>,Object> e:edgesProps.entrySet()){
//                        if(e.getKey().b.equals(key)&& e.getValue().equals(value))
//                            longs.add(e.getKey().a);
//                    }
//                }
//
//                Iterator<Long> i= indexed?
//                        Fun.filter(edgesIndex,key,value).iterator():longs.iterator();
//
//                return new MEdgeRecidIterator(i);
//            }
//        };
//    }
//
//
//
//
//    public void shutdown() {
//        if(db.isClosed()) return;
//        db.commit();
//        db.close();
//    }
//
//
//
////
////    public String toString() {
////        boolean up = !db.isClosed();
////        return StringFactory.graphString(this, "vertices:" + (up?this.vertices.size():"CLOSED") + " edges:" +
////                (up?this.edges.size():"CLOSED") + " db:" + this.db);
////    }
//}