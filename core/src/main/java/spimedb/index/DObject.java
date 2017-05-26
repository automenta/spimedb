package spimedb.index;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import jcog.tree.rtree.point.DoubleND;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseTokenizer;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.tokenattributes.TermToBytesRefAttribute;
import org.apache.lucene.document.*;
import org.apache.lucene.facet.FacetField;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesFacetField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.IndexableFieldType;
import org.apache.lucene.util.NumericUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spimedb.LazyValue;
import spimedb.MutableNObject;
import spimedb.NObject;
import spimedb.SpimeDB;
import spimedb.util.JSON;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

import static jcog.tree.rtree.rect.RectDoubleND.unbounded;

/**
 * (Lucene) Document-based NObject
 */
public class DObject implements NObject {


    public final static Logger logger = LoggerFactory.getLogger(DObject.class);
    private static final Pattern ID_PATH_SPLITTER = Pattern.compile("/\\/+/");

    final String id;
    public final Document document;
    private final DoubleND min;
    private final DoubleND max;

    public static DObject get(Document d) {
        return new DObject(d);
    }

    public static DObject get(NObject n, SpimeDB db) {
        if (n instanceof DObject)
            return ((DObject) n);
        return get(toDocument(n, db));
    }

    static Document toDocument(@NotNull NObject n, @NotNull SpimeDB db) {

        if (n instanceof DObject)
            return ((DObject) n).document;

        Document d = new Document();

        String nid = n.id();

        d.add(string(NObject.ID, nid));

        String name = n.name();
        if (name != null && !name.equals(nid))
            d.add(text(NObject.NAME, name));

        String[] t = n.tags();
        if (t!=null) {
            for (String tt : t)
                d.add(string(NObject.TAG, tt));
        }

        if (n.bounded()) {
            DoubleND minP = n.min();
            if (minP != unbounded) {

                d.add(new SpacetimeField((minP.coord), (n.max().coord)));

                //d.add(new FloatRangeField(NObject.BOUND, min, max));
                //float[] aa = ArrayUtils.addAll(min, max);
                //d.add(new FloatPoint(NObject.BOUND, aa));
                //d.add(string(NObject.BOUND, JSON.toJSONString(new float[][] { min, max } )));




            }
        }

        n.forEach((k, v) -> {

            if (v == null)
                throw new NullPointerException();

            if (v instanceof LazyValue) {
                LazyValue l = (LazyValue) v;
                v = l.pendingValue;
                db.runLater(l.priority, () -> {
                    Object lv = l.value.get();
                    if (lv != null) {
                        MutableNObject nv = new MutableNObject(nid);
                        nv.put(l.key, lv);
                        db.merge(nv);
                    }
                });
                if (v == null)
                    return; //dont write null pending value
            }

            //special handling
            switch (k) {
                case NObject.NAME:
                case NObject.TAG:
                case NObject.CONTENT:
                    return;

//                case NObject.TYPE:
//                    d.add(new FacetField(NObject.TYPE, v.toString()));
//                    //FacetField f = new FacetField();
//                    return;
            }

            Class c = v.getClass();
            if (c == String.class) {
                d.add(text(k, ((String) v)));
            } else if (c == String[].class) {
                String[] ss = (String[]) v;
                for (String s : ss) {
                    d.add(text(k, s));
                }
            } else if (c == Integer.class) {
                d.add(new IntPoint(k, (Integer) v));
            } else if (c == Boolean.class) {
                //HACK
                d.add(new BinaryPoint(k, new byte[]{(byte) ((Boolean) v ? 0 : 1)}));
            } else if (c == Double.class) {
                d.add(new DoublePoint(k, (Double) v));
            } else if (c == Long.class) {
                throw new UnsupportedOperationException();
                //d.add(new LongPoint(k, ((Long) v).longValue()));
            } else if (v instanceof ScriptObjectMirror) {
                //HACK ignore
            } else if (c == double[][].class) {
                //d.add(new StoredField(k, new BytesRef(JSON.toMsgPackBytes(v))));
                //d.add(new StoredField(k, JSON.toJSONBytes(v)));
                //throw new UnsupportedOperationException();
                d.add(bytes(k, JSON.toMsgPackBytes(v, double[][].class)));
            } else if (c == byte[].class) {
                d.add(new StoredField(k, (byte[]) v));
            } else if (v instanceof JsonNode) {
                try {
                    JsonNode j = (JsonNode)v;
                    String js = JSON.json.writeValueAsString(j);
                    d.add(text(k, js));
                    StringBuilder sb = new StringBuilder();
                    j.fields().forEachRemaining(e->{
                        sb.append(e.getKey()).append(' ');
                        JsonNode val = e.getValue();
                        if (val.isTextual())
                            sb.append(val.textValue()).append(' ');
                        /*else if (val.isNumber())
                            sb.append(val.numberValue()).append(' ');*/
                        /* TODO else: recurse */
                    });
                    if (sb.length()!=0)
                        d.add(text(TAG,sb.toString()));
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            } else {
                logger.warn("field un-documentable: {} {} {}", k, c, v);
                d.add(string(k, v.toString()));
            }

        });

        return d;
    }


    private static IndexableField bytes(String key, byte[] bytes) {
        return new StoredField(key, bytes);
    }


    protected DObject() {
        id = null;
        document = null;
        min = max = null;
    }

    DObject(Document d) {

        this.document = d;
        this.id = d.get(NObject.ID);

        IndexableField b = d.getField(NObject.BOUND);
        if (b instanceof DoubleRange) {
            DoubleRange f = (DoubleRange) b;
            double[] min = new double[4];
            double[] max = new double[4];
            for (int i = 0; i < 4; i++) {
                min[i] = f.getMin(i);
                max[i] = f.getMax(i);
            }

            this.min = new DoubleND(min);
            this.max = new DoubleND(max);
        } else if (b instanceof StoredField) {
            StoredField sf = (StoredField) b;

            byte[] bbb = sf.binaryValue().bytes;
            int l = bbb.length/2;
            double[] min = new double[4], max = new double[4];
            for (int i = 0; i < 4; i++) {
                min[i] = NumericUtils.sortableLongToDouble(NumericUtils.sortableBytesToLong(bbb, i * 8));
                max[i] = NumericUtils.sortableLongToDouble(NumericUtils.sortableBytesToLong(bbb, l + i * 8));
            }
            this.min = new DoubleND(min);
            this.max = new DoubleND(max);

        } else {
            min = max = unbounded;
        }

        if (id.contains("/")) {
            String[] path = ID_PATH_SPLITTER.split(id);
            if (path.length > 0)
                d.add(new FacetField(NObject.ID, path));
        }

        for (String t : tags()) {
            if (/*t == null || */t.isEmpty())
                continue;

            d.add(new FacetField(NObject.TAG, t));
        }


        String name = name();
        if (name != null && !name.isEmpty()) {


            Set<String> k = parseKeywords(new LowerCaseTokenizer(), name);
            for (String l : k) {
                if (l.length() >= 3 && !StopAnalyzer.ENGLISH_STOP_WORDS_SET.contains(l))
                    d.add(new FacetField(NObject.TAG, l));
            }
        }


////            if (t.contains("/")) {
////                String[] path = t.split("/");
////                d.add(new FacetField(NObject.TAG, path));
////            } else {
////                d.add(new SortedSetDocValuesFacetField(NObject.TAG, t));
////            }
//        }
    }

    public static Set<String> parseKeywords(Tokenizer stream, String text) {

        stream.setReader(new StringReader(text));

        try {
            stream.reset();
        } catch (IOException e) {
            return Collections.emptySet();
        }


        Set<String> result;
        if (stream.hasAttributes()) {
            result = new HashSet<>(1);
            try {
                while (stream.incrementToken()) {
                    result.add(stream.getAttribute(TermToBytesRefAttribute.class).toString());
                }
            } catch (IOException ignored) { }
        } else {
            result = Collections.emptySet();
        }

        try {
            stream.end();
            stream.close();
        } catch (IOException ignored) {        }

        return result;

    }

    static StringField string(String key, String value) {
        return new StringField(key, value, Field.Store.YES);
    }

    public static TextField text(String key, String value) {
        return new TextField(key, value, Field.Store.YES);
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || id.equals(((NObject) obj).id());
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String id() {
        return document.get(NObject.ID);
    }

    @Override
    public String name() {
        return document.get(NObject.NAME);
    }

    @Override
    public String[] tags() {
        String tagString = document.get(NObject.TAG);
        if (tagString == null)
            return ArrayUtils.EMPTY_STRING_ARRAY;
        else
            return tagString.split(" ");
    }

    @Override
    public void forEach(BiConsumer<String, Object> each) {

        document.forEach(f -> {
            if (f instanceof SortedSetDocValuesFacetField || f instanceof FacetField)
                return;

            String k = f.name();
            switch (k) {
                case NObject.ID:
                    break; //filtered
                case NObject.BOUND:
                    break; //filtered
                default:
                    each.accept(k, value(f));
                    break;
            }
        });
    }

    @Override
    public <X> X get(String tag) {
        IndexableField f = document.getField(tag);
        if (f == null)
            return null;
        else
            return (X) value(f);
    }

    @NotNull
    private static Object value(IndexableField f) {
        switch (f.name()) {
            case NObject.LINESTRING:
            case NObject.POLYGON:
                return JSON.fromMsgPackBytes(f.binaryValue().bytes, double[][].class);
            //return JSON.fromMsgPackBytes(f.binaryValue().bytes);
        }

        if (f instanceof BinaryPoint) {
            //HACK convert to boolean
            return f.binaryValue().bytes[0] != 0;
        } else if (f instanceof DoublePoint) {
            //throw new UnsupportedOperationException(); //not sure why this doesnt seem to be working
            DoublePoint dp = (DoublePoint) f;
            byte[] b = dp.binaryValue().bytes;

            double[] dd = new double[b.length / Double.BYTES];
            for (int i = 0; i < dd.length; i++)
                dd[i] = DoublePoint.decodeDimension(b, i);
            if (dd.length == 1)
                return dd[0];
            return dd;

        } else if (f instanceof LongPoint) {
            throw new UnsupportedOperationException(); //not sure why this doesnt seem to be working
//            LongPoint lp = (LongPoint)f;
//            byte[] b = lp.binaryValue().bytes;
//            long[] dd = new long[b.length / Long.BYTES];
//            for (int i = 0;i < dd.length; i++)
//                dd[i] = LongPoint.decodeDimension(b, i);
//            if (dd.length == 1)
//                return dd[0];
//            return dd;
        } else if (f instanceof FloatRange) {
            throw new UnsupportedOperationException();
        } else if (f instanceof IntPoint) {
            IntPoint ff = (IntPoint) f;
            return ff.numericValue().intValue(); //HACK assumes dim=1
        }

        IndexableFieldType type = f.fieldType();

        if (type == StoredField.TYPE) {
            return f.binaryValue().bytes;
        } else {
            String s = f.stringValue(); //TODO adapt based on field type
            if (s.startsWith("{") && s.endsWith("}")) {
                //try to parse as json
                JsonNode j = null;
                try {
                    j = JSON.json.readValue(s, JsonNode.class);
                    return j;
                } catch (IOException e) {
                    //could not parse
                }
            }
            return s;
        }
    }

    @Override
    public DoubleND min() {
        return min;
    }

    @Override
    public DoubleND max() {
        return max;
    }


    @Override
    public String toString() {
        return toJSONString();
    }


}

