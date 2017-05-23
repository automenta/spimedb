package spimedb.server;

import com.google.common.collect.Iterables;
import org.apache.commons.io.IOUtils;
import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.search.ScoreDoc;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.factory.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import spimedb.FilteredNObject;
import spimedb.NObject;
import spimedb.SpimeDB;
import spimedb.index.DObject;
import spimedb.index.Search;
import spimedb.util.JSON;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

public enum WebIO {
    ;

    public static final int BUFFER_SIZE = 32 * 1024;

    public static final ImmutableSet<String> searchResultSummary =
            Sets.immutable.of(
                    NObject.ID, NObject.NAME, NObject.INH, NObject.TAG, NObject.BOUND,
                    NObject.ICON, NObject.SCORE, NObject.LINESTRING, NObject.POLYGON,
                    NObject.TYPE, "page"
            );
    public static final ImmutableSet<String> searchResultFull =
            Sets.immutable.withAll(Iterables.concat(Sets.mutable.ofAll(searchResultSummary), Sets.immutable.of(
                    NObject.DESC, NObject.DATA
            )));

    public static void send(Search r, OutputStream o, ImmutableSet<String> keys) {
        if (r != null) {

            try {
                o.write("[[".getBytes());
                r.forEachDocument((y, x) -> {
                    JSON.toJSON(searchResult(
                            DObject.get(y), keys, x
                    ), o, ',');
                    return true;
                });
                o.write("{}],".getBytes()); //<-- TODO search result metadata, query time etc

                if (r.facets != null) {
                    stream(r.facets, o);
                    o.write(']');
                } else
                    o.write("[]]".getBytes());

                r.close();

            } catch (IOException e) {

            }
        }

        //ex.setStatusCode(StatusCodes.INTERNAL_SERVER_ERROR);

    }

    static public void stream(FacetResult x, OutputStream o) {
        JSON.toJSON(
                Stream.of(x.labelValues).map(y -> new Object[]{y.label, y.value}).toArray(Object[]::new)
                /*Stream.of(x.labelValues).collect(
                Collectors.toMap(y->y.label, y->y.value ))*/, o);
    }

    public static FilteredNObject searchResult(NObject d, ImmutableSet<String> include) {
        return searchResult(d, include, null);
    }

    public static FilteredNObject searchResult(NObject d, ImmutableSet<String> include, @Nullable ScoreDoc score) {
        return new FilteredNObject(d, include) {
            @Override
            protected Object value(String key, Object v) {
                switch (key) {
                    case NObject.ICON:
                        //rewrite the thumbnail blob byte[] as ID reference
                        return d.id();
                    case NObject.DATA:
                        //rewrite the thumbnail blob byte[] as ID reference (if not already a string representing a URL)
                        if (v instanceof byte[]) {
                            return d.id();
                        } else if (v instanceof String) {
                            String s = (String) v;
                            if (s.startsWith("file:")) {
                                return d.id();  //same as if it's a byte
                            } else {
                                return s;
                            }
                        } else {
                            //??
                        }
                }
                return v;
            }

            @Override
            public void forEach(BiConsumer<String, Object> each) {
                super.forEach(each);
                if (score != null)
                    each.accept(NObject.SCORE, score.score);
            }
        };
    }

    public static Response send(SpimeDB db, @NotNull String id, String field) {

        DObject x = db.get(id);

        if (x != null) {

            Object f = x.get(field);

            if (f instanceof String) {
                //interpret the string stored at this as a URL or a redirect to another field
                String s = (String) f;
                switch (s) {
                    case NObject.DATA:
                        if (!field.equals(NObject.DATA))
                            return send(db, id, NObject.DATA);
                        else {
                            //infinite loop
                            throw new UnsupportedOperationException("document field redirect cycle");
                        }
                    default:
                        if (s.startsWith("file:")) {
                            File ff = new File(s.substring(5));
                            if (ff.exists()) {
                                return Response.ok((StreamingOutput) o -> {
                                    IOUtils.copyLarge(new FileInputStream(ff), o, new byte[BUFFER_SIZE]);
                                }).type(typeOf(x.get("url"))).build();
                            }
                        }
                        break;
                }
            } else if (f instanceof byte[]) {
                return Response.ok((StreamingOutput) o -> {
                    o.write((byte[]) f);
                }).type(typeOfField(field)).build();
            }
        }

        return Response.status(404).build();
    }

    private static String typeOfField(String field) {
        switch (field) {
            case "thumbnail": //deprecated
            case "icon":
                return "image/*";
        }
        return "application/*";
    }

    private static String typeOf(String s) {
        int i = s.lastIndexOf('.');
        if ((i != -1) && (i < s.length()-1)) {
            //HACK todo use a nice trie or something
            switch (s.substring(i+1)) {
                case "jpg": return "image/jpg";
                case "png": return "image/png";
                case "gif": return "image/gif";
                case "pdf": return "application/pdf";
                case "html": return "text/html";
                case "xml": return "text/xml";
                case "json": return "application/json";
            }
        }
        return "application/*";
    }

}
