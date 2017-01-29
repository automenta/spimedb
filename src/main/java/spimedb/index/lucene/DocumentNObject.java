package spimedb.index.lucene;

import com.google.common.base.Joiner;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.apache.commons.lang.ArrayUtils;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.IndexableFieldType;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spimedb.NObject;
import spimedb.index.rtree.PointND;
import spimedb.util.JSON;

import java.util.function.BiConsumer;

import static spimedb.SpimeDB.string;
import static spimedb.SpimeDB.text;
import static spimedb.index.rtree.RectND.unbounded;

/**
 * Created by me on 1/25/17.
 */
public class DocumentNObject implements NObject {

    public final static Logger logger = LoggerFactory.getLogger(DocumentNObject.class);

    final String id;
    public final Document document;
    private final PointND min;
    private final PointND max;

    public static DocumentNObject get(Document d) {
        return new DocumentNObject(d);
    }
    public static DocumentNObject get(NObject n) {
        if (n instanceof DocumentNObject)
            return ((DocumentNObject)n);
        return new DocumentNObject(toDocument(n));
    }

    public static Document toDocument(NObject n) {

        if (n instanceof DocumentNObject)
            return ((DocumentNObject)n).document;

        String nid = n.id();

        Document d = new Document();

        d.add(string(NObject.ID, nid));

        String name = n.name();
        if (name != null && !name.equals(nid))
            d.add(text(NObject.NAME, name));

        String[] t = n.tags();
        if (t.length > 0)
            d.add(string(NObject.TAG, Joiner.on(' ').join(t)));

        if (n.bounded()) {
            PointND minP = n.min();
            if (minP != unbounded) {
                float[] min = minP.coord;

                float[] max = n.max().coord;
                try {
                    //d.add(new FloatRangeField(NObject.BOUND, min, max));
                    //float[] aa = ArrayUtils.addAll(min, max);
                    //d.add(new FloatPoint(NObject.BOUND, aa));
                    d.add(string(NObject.BOUND, JSON.toJSONString(new float[][] { min, max } )));
                } catch (IllegalArgumentException e) {
                    logger.warn("{}", e);
                }

            }
        }

        n.forEach((k,v)-> {

            if (v == null)
                throw new NullPointerException();

            //special handling
            switch (k) {
                case NObject.NAME:
                case NObject.TAG:
                case NObject.CONTENT:
                    return;
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
                d.add(new IntPoint(k, ((Integer) v).intValue()));
            } else if (c == Boolean.class) {
                //HACK
                d.add(new BinaryPoint(k, new byte[] { (byte)(((Boolean)v).booleanValue() ? 0 : 1) } ));
            } else if (c == Double.class) {
                d.add(new DoublePoint(k, ((Double) v).doubleValue()));
            } else if (c == Long.class) {
                throw new UnsupportedOperationException();
                //d.add(new LongPoint(k, ((Long) v).longValue()));
            } else if (v instanceof ScriptObjectMirror) {
                //HACK ignore
            } else if (c == double[][].class) {
                //d.add(new StoredField(k, new BytesRef(JSON.toMsgPackBytes(v))));
                d.add(new StoredField(k, JSON.toJSONBytes(v)));
            } else if (c == byte[].class) {
                d.add(new StoredField(k, (byte[])v));
            } else {
                logger.warn("field un-documentable: {} {} {}", k, c, v);
                d.add(string(k, v.toString()));
            }

        });

        return d;
    }


    DocumentNObject(Document d) {

        this.document = d;
        this.id = d.get(NObject.ID);

        IndexableField b = d.getField(NObject.BOUND);
        if (b!=null) {
            //FloatRangeField f = (FloatRangeField)b;

            //HACK make faster
            Field f = (Field)b;
            float[][] dd = JSON.fromJSON(f.stringValue(), float[][].class);
            min = new PointND(dd[0]);
            max = new PointND(dd[1]);
        } else {
            min = max = unbounded;
        }
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || id.equals(((NObject)obj).id());
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
            String k = f.name();
            switch (k) {
                case NObject.ID: break; //filtered
                case NObject.BOUND: break; //filtered
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
    private Object value(IndexableField f) {
        switch (f.name()) {
            case NObject.LINESTRING:
            case NObject.POLYGON:
                return JSON.fromJSON(f.binaryValue().bytes, double[][].class);
                //return JSON.fromMsgPackBytes(f.binaryValue().bytes);
        }

        if (f instanceof BinaryPoint) {
            //HACK convert to boolean
            return ((BinaryPoint)f).binaryValue().bytes[0] == 0 ? false : true;
        } else if (f instanceof DoublePoint) {
            DoublePoint dp = (DoublePoint) f;
            byte[] b = dp.binaryValue().bytes;

            double[] dd = new double[b.length / Double.BYTES];
            for (int i = 0;i < dd.length; i++)
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
        } else if (f instanceof FloatRangeField) {
            throw new UnsupportedOperationException();
        } else if (f instanceof IntPoint) {
            IntPoint ff = (IntPoint)f;
            return ff.numericValue().intValue(); //HACK assumes dim=1
        }

        IndexableFieldType type = f.fieldType();

        if (type == StoredField.TYPE) {
            byte[] b = f.binaryValue().bytes;
            return b;
        } else {
            Object v = f.stringValue(); //TODO adapt based on field type
            if (v == null) {
                throw new NullPointerException();
            }
            return v;
        }
    }

    @Override
    public PointND min() {
        return min;
    }

    @Override
    public PointND max() {
        return max;
    }


    @Override
    public String toString() {
        return toJSONString();
    }



}

