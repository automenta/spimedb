package spimedb.query;

import org.apache.lucene.facet.Facets;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.taxonomy.FastTaxonomyFacetCounts;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyReader;
import spimedb.NObject;
import spimedb.SpimeDB;
import spimedb.index.Search;

import java.io.IOException;

public class CollectFacets extends FacetsCollector {

    private final int limit;

    public CollectFacets(int limit) {

        this.limit = limit;
    }

    public void commit(Search s, SpimeDB db) {

        try {
            DirectoryTaxonomyReader taxoReader = db.readTaxonomy();
            Facets facets = new FastTaxonomyFacetCounts(taxoReader, db.facetConfig(), this);
            s.setFacets(facets.getTopChildren(limit, NObject.TAG));
            taxoReader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } //finally {

    }
}
