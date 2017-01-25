package spimedb.index.lucene;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import spimedb.NObject;
import spimedb.index.rtree.PointND;

import java.util.function.BiConsumer;

import static spimedb.index.rtree.RectND.unbounded;

/**
 * Created by me on 1/25/17.
 */
public class DocumentNObject implements NObject {

    public static NObject get(Document d) {
        IndexableField blob = d.getField(NObject.BLOB);
        if (blob!=null) {
            return NObject.fromJSON(blob.stringValue());
        } else {
            return new DocumentNObject(d);
        }
    }

    Document document;

    DocumentNObject(Document d) {
        setDocument(d);
    }

    public void setDocument(Document document) {
        this.document = document;
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
        return document.getValues(NObject.TAG);
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

