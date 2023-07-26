package spimedb.server;

import com.google.common.collect.Iterables;
import jcog.bloom.StableBloomFilter;
import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.search.ScoreDoc;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.factory.Sets;
import org.jetbrains.annotations.Nullable;
import spimedb.FilteredNObject;
import spimedb.NObject;
import spimedb.index.Search;
import spimedb.util.JSON;

import java.io.IOException;
import java.io.OutputStream;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import static java.lang.Double.parseDouble;

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


    public static void send(Search r, OutputStream o, int timeoutMS, ImmutableSet<String> keys) {
        send(r, o, timeoutMS, keys, null);
    }

    static final byte[] openingBracketBytes = "[[".getBytes();
    static final byte[] intermediateClosingBracketBytes = "{}],".getBytes();
    static final byte[] endingClosingBracketBytes = "[]]".getBytes();

    public static void send(Search r, OutputStream o, int timeoutMS, ImmutableSet<String> keys, @Nullable StableBloomFilter<String> sentSTM) {
        if (r != null) {

            try {
                o.write(openingBracketBytes);
            } catch (IOException ignored) {
                return;
            }

            r.forEach((y, x) -> {

                if (sentSTM!=null) {
                    if (!sentSTM.addIfMissing(y.id())) {
                        return true;
                    } else {
                        //sentSTM.forget(0.0005f);
                    }
                }

                JSON.toJSON(searchResult(y, keys, x), o, ',');

                return true;
            }, timeoutMS, () -> {
                try {


                    o.write(intermediateClosingBracketBytes); //<-- TODO search result metadata, query time etc

                    if (r.facets != null) {
                        stream(r.facets, o);
                        o.write(']');
                    } else {
                        o.write(endingClosingBracketBytes);
                    }

                } catch (IOException ignored) {

                }
            });

        }


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
                    case NObject.ICON -> {
                        //rewrite the thumbnail blob byte[] as ID reference
                        return d.id();
                    }
                    case NObject.DATA -> {
                        //rewrite the thumbnail blob byte[] as ID reference (if not already a string representing a URL)
                        if (v instanceof byte[]) {
                            return d.id();
                        } else if (v instanceof String s) {
                            if (s.startsWith("file:")) {
                                return d.id();  //same as if it's a byte
                            } else {
                                return s;
                            }
                        } else {
                            //??
                        }
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

//    public static Response send(SpimeDB db, @NotNull String id, String field) {
//
//        DObject x = db.get(id);
//
//        if (x != null) {
//
//            Object f = x.get(field);
//
//            if (f instanceof String s) {
//                //interpret the string stored at this as a URL or a redirect to another field
//                if (s.equals(NObject.DATA)) {
//                    if (!field.equals(NObject.DATA))
//                        return send(db, id, NObject.DATA);
//                    else {
//                        //infinite loop
//                        throw new UnsupportedOperationException("document field redirect cycle");
//                    }
//                } else {
//                    if (s.startsWith("file:")) {
//                        File ff = new File(s.substring(5));
//                        if (ff.exists()) {
//                            return Response.ok((StreamingOutput) o -> IOUtils.copyLarge(new FileInputStream(ff), o, new byte[BUFFER_SIZE])).type(typeOf(x.get("url"))).build();
//                        }
//                    }
//                }
//            } else if (f instanceof byte[]) {
//                return Response.ok((StreamingOutput) o -> o.write((byte[]) f)).type(typeOfField(field)).build();
//            }
//        }
//
//        return Response.status(404).build();
//    }

    private static String typeOfField(String field) {
        return switch (field) { //deprecated
            case "thumbnail", "icon" -> "image/*";
            default -> "application/*";
        };
    }

    private static String typeOf(String s) {
        int i = s.lastIndexOf('.');
        if ((i != -1) && (i < s.length() - 1)) {
            //HACK todo use a nice trie or something
            switch (s.substring(i + 1)) {
                case "jpg" -> {
                    return "image/jpg";
                }
                case "png" -> {
                    return "image/png";
                }
                case "gif" -> {
                    return "image/gif";
                }
                case "pdf" -> {
                    return "application/pdf";
                }
                case "html" -> {
                    return "text/html";
                }
                case "xml" -> {
                    return "text/xml";
                }
                case "json" -> {
                    return "application/json";
                }
            }
        }
        return "application/*";
    }

    /**
     * parse Lon/Lat Rectangle from string:
     * lonMin;lonMax;latMin;latMax
     * longitude first (x), then latitude (y)
     * see: https://www.w3.org/DesignIssues/MatrixURIs.html
     * TODO use more efficient number extraction method
     */
    @Nullable
    public static double[] LonLatRect(String b) {

        String[] bb = b.split(";");
        if (bb.length != 4)
            return null;

        double[] x = new double[4];
        try {
            x[0] = parseDouble(bb[0]);
            x[1] = parseDouble(bb[1]);
            x[2] = parseDouble(bb[2]);
            x[3] = parseDouble(bb[3]);
        } catch (NumberFormatException e) {
            return null;
        }

        return x;
    }
}
