package spimedb.server;

import com.google.common.collect.Iterables;
import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.search.ScoreDoc;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.factory.Sets;
import spimedb.FilteredNObject;
import spimedb.NObject;
import spimedb.index.DObject;
import spimedb.index.SearchResult;
import spimedb.util.JSON;

import java.io.IOException;
import java.io.OutputStream;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

public enum WebIO {
    ;

    public static final ImmutableSet<String> searchResultSummary =
            Sets.immutable.of(
                    NObject.ID, NObject.NAME, NObject.INH, NObject.TAG, NObject.BOUND,
                    "thumbnail", "score", NObject.LINESTRING, NObject.POLYGON,
                    NObject.TYPE, "url"
            );
    public static final ImmutableSet<String> searchResultFull =
            Sets.immutable.withAll(Iterables.concat(Sets.mutable.ofAll(searchResultSummary), Sets.immutable.of(
                    NObject.DESC, "data"
            )));

    public static void send(SearchResult r, OutputStream o, ImmutableSet<String> keys) {
        if (r != null) {

            try {
                o.write("[[".getBytes());
                r.forEachDocument((y, x) -> {
                    JSON.toJSON(searchResult(
                            DObject.get(y), x, keys
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

    public static FilteredNObject searchResult(NObject d, ScoreDoc x, ImmutableSet<String> keys) {
        return new FilteredNObject(d, keys) {
            @Override
            protected Object value(String key, Object v) {
                switch (key) {
                    case "thumbnail":
                        //rewrite the thumbnail blob byte[] as a String URL
                        return d.id();
                    case "data":
                        //rewrite the thumbnail blob byte[] as a String URL (if not already a string representing a URL)
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
                each.accept("score", x.score);
            }
        };
    }
}
