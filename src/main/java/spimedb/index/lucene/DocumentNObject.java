package spimedb.index.lucene;

import com.google.common.base.Joiner;
import org.apache.commons.lang.ArrayUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spimedb.NObject;
import spimedb.index.rtree.PointND;

import java.util.function.BiConsumer;

import static spimedb.index.Search.string;
import static spimedb.index.Search.text;
import static spimedb.index.rtree.RectND.unbounded;

/**
 * Created by me on 1/25/17.
 */
public class DocumentNObject implements NObject {

    public final static Logger logger = LoggerFactory.getLogger(DocumentNObject.class);

    final Document document;

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

        n.forEach((k,v)->{

            //special handling
            switch (k) {
                case NObject.NAME:
                case NObject.TAG:
                case NObject.CONTENT:
                    return;
            }

            Class c = v.getClass();
            if (c == String.class) {
                d.add(text(k, ((String)v)));
            } else if (c == String[].class) {
                String[] ss = (String[])v;
                for (String s : ss) {
                    d.add(text(k, s));
                }
            } else {
                logger.warn("field un-documentable: {} {} {}", k, c, v);
            }

        });

        return d;


    }


    DocumentNObject(Document d) {
        this.document = d;
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
        Object v = f.stringValue(); //TODO adapt based on field type
        return v;
    }

    @Override
    public PointND min() {
        return unbounded; //TODO
    }

    @Override
    public PointND max() {
        return unbounded; //TODO
    }


    @Override
    public String toString() {
        return toJSONString();
    }


}

