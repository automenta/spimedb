package spimedb.index.lucene;

import com.google.common.base.Joiner;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.apache.commons.lang.ArrayUtils;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.IndexableFieldType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spimedb.NObject;
import spimedb.index.rtree.PointND;
import spimedb.util.JSON;

import java.util.function.BiConsumer;

import static spimedb.index.Search.string;
import static spimedb.index.Search.text;
import static spimedb.index.rtree.RectND.unbounded;

/**
 * Created by me on 1/25/17.
 */
public class DocumentNObject implements NObject {

    public final static Logger logger = LoggerFactory.getLogger(DocumentNObject.class);

    final String id;
    final Document document;
    private final PointND min;
    private final PointND max;

    public static NObject get(Document d) {
        return new DocumentNObject(d);
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

        PointND minP = n.min();
        if (minP != unbounded) {
            float[] min = minP.coord;
            float[] max = n.max().coord;
            try {
                d.add(new FloatRangeField(NObject.BOUND, min, max));
            } catch (IllegalArgumentException e) {
                logger.warn("{}", e);
            }
        }

        n.forEach((k,v)-> {

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
                //HACK ignore
//                d.add(new BinaryPoint(k, ((Boolean)v).booleanValue() ));
            } else if (c == Double.class) {
                d.add(new DoublePoint(k, ((Double) v).doubleValue()));
            }  else if (v instanceof ScriptObjectMirror) {
                //HACK ignore
            } else if (c == double[][].class) {
                //d.add(new StoredField(k, new BytesRef(JSON.toMsgPackBytes(v))));
                d.add(new StoredField(k, JSON.toJSONBytes(v)));
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

        Object b = d.get(NObject.BOUND);
        if (b!=null) {
            FloatRangeField f = (FloatRangeField)b;

            //HACK make faster
            min = new PointND(f.getMin(0),f.getMin(1),f.getMin(2),f.getMin(3));
            max = new PointND(f.getMax(0),f.getMax(1),f.getMax(2),f.getMax(3));
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
                default:
                    each.accept(k, value(f));
                    break;
            }
        });
    }

    @Override
    public <X> X get(String tag) {
        return (X) value(document.getField(tag));
    }

    private Object value(IndexableField f) {
        switch (f.name()) {
            case NObject.LINESTRING:
            case NObject.POLYGON:
                return JSON.fromJSON(f.binaryValue().bytes, double[][].class);
                //return JSON.fromMsgPackBytes(f.binaryValue().bytes);
        }

        IndexableFieldType type = f.fieldType();
        if (type == StoredField.TYPE) {
            byte[] b = f.binaryValue().bytes;
            return b;
        } else {
            Object v = f.stringValue(); //TODO adapt based on field type
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

