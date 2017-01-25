package spimedb.index;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.NRTCachingDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spimedb.NObject;
import spimedb.SpimeDB;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

/**
 * Lucene free-text search plugin
 */
public class Search implements BiConsumer<NObject, SpimeDB>, Function<String, Document> {

    public final static Logger logger = LoggerFactory.getLogger(Search.class);

    private final Directory dir;

    private final Set<String> out = Collections.newSetFromMap(new ConcurrentHashMap<>(1024));

    private final AtomicBoolean writing = new AtomicBoolean(false);
    private final SpimeDB db;
    private final StandardAnalyzer analyzer;
    private final Directory dirBase;

    public Search(SpimeDB db)  {
        this(db, new RAMDirectory());
    }

    public Search(SpimeDB db, String path) throws IOException {
        this(db, FSDirectory.open(new File(path).toPath()));
    }

    public Search(SpimeDB db, Directory directory) {
        this.db = db;

        //this.dir = new RAMDirectory();
        dirBase = directory;

        //this.dir = directory;
        this.dir = new NRTCachingDirectory(dirBase, 5.0, 60.0);

        analyzer = new StandardAnalyzer();

        db.on(this);
    }

    private void commit(String id) {
        if (out.add(id)) {
            commit();
        }
    }

    static <X> Iterable<X> drain(Iterable<X> x) {
        return () -> new Iterator<X>() {

            final Iterator<X> ii = x.iterator();

            @Override
            public boolean hasNext() {
                return ii.hasNext();
            }

            @Override
            public X next() {
                X n = ii.next();
                ii.remove();
                return n;
            }
        };
    }



    private void commit() {
        if (writing.compareAndSet(false, true)) {
            db.exe.execute( () -> {
                try {

                    IndexWriterConfig writerConf = new IndexWriterConfig(analyzer);
                    writerConf.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
                    //writerConf.setRAMBufferSizeMB(s * 1024 /* estimate */);

                    IndexWriter writer = new IndexWriter(dir, writerConf);

                    int written = 0;
                    int s;


                    while ((s = out.size()) > 0) {

                        long seq = writer.addDocuments(Iterables.transform(drain(out), this));

//                        Iterator<String> ii = out.iterator();
//                        while (ii.hasNext()) {
//                            String nid = ii.next();
//                            ii.remove();
//                            Document d = apply(nid);
//                            writer.updateDocument(new Term(NObject.ID, nid), d);
//                        }
                        writer.commit();
                        written += s;
                    }
                    writer.close();
                    logger.info("{} indexed", written);

                } catch (IOException e) {
                    logger.error("indexing error: {}", e);
                }
                writing.set(false);
            });
        }

    }

    @Override
    public void accept(NObject x, SpimeDB db) {
        commit(x.id());
    }

    @Override
    public Document apply(String nid) {
        NObject n = db.get(nid);

        Document d = new Document();

        d.add(string(NObject.ID, nid));

        String name = n.name();
        if (name!=null && !name.equals(nid))
            d.add(text(NObject.NAME, name));

        for (String t : n.tags())
            d.add(string(NObject.TAG, t));

        d.add(text(NObject.BLOB, n.toJSONString())); //HACK store the JSON as a text blob

//        n.forEach((k,v)->{
//
//            //special handling
//            switch (k) {
//                case NObject.NAME:
//
//                    return;
//
//                case NObject.TAG:
//                case NObject.CONTENT:
//                    return;
//            }
//
//            Class c = v.getClass();
//            switch (c.getSimpleName()) {
//                case "String":
//                    d.add(new TextField(k, v.toString(), Field.Store.YES));
//                    break;
//                case "String[]":
//                    d.add(new StringField(k, v.toString(), Field.Store.YES));
//                    break;
//            }
//
//        });
        return d;
    }


    public static StringField string(String key, String value) {
        return new StringField(key, value, Field.Store.YES);
    }
    public static TextField text(String key, String value) {
        return new TextField(key, value, Field.Store.YES);
    }

    public final class SearchResult {

        private final TopDocs docs;
        private final IndexSearcher searcher;

        public SearchResult(IndexSearcher searcher, TopDocs docs) {
            this.searcher = searcher;
            this.docs = docs;
            logger.info("query: {}", docs.scoreDocs);
        }

        public Iterator<Document> docs() {

            final DocumentStoredFieldVisitor visitor = new DocumentStoredFieldVisitor();

            IndexReader reader = searcher.getIndexReader();

            return Iterators.transform(Iterators.forArray(docs.scoreDocs), sd->{
                try {
                    visitor.getDocument().clear();
                    reader.document(sd.doc, visitor);
                    return visitor.getDocument();
                } catch (IOException e) {
                    logger.error("{} {}", sd, e);
                    return null;
                }
            });
        }

    }

    public SearchResult find(String query) {
        int hitsPerPage = 10;
        try {
            Query q = new QueryParser(NObject.NAME, analyzer).parse(query);
            DirectoryReader reader = DirectoryReader.open(dir);
            IndexSearcher searcher = new IndexSearcher(reader);
            TopDocs docs = searcher.search(q, hitsPerPage);

            if (docs.totalHits == 0) {
                //TODO: return EmptySearchResult;
            }

            return new SearchResult(searcher, docs);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

///*
// * Licensed to the Apache Software Foundation (ASF) under one or more
// * contributor license agreements.  See the NOTICE file distributed with
// * this work for additional information regarding copyright ownership.
// * The ASF licenses this file to You under the Apache License, Version 2.0
// * (the "License"); you may not use this file except in compliance with
// * the License.  You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//package org.apache.lucene.demo.facet;
//
//
//        import java.io.IOException;
//        import java.util.ArrayList;
//        import java.util.List;
//
//        import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
//        import org.apache.lucene.document.Document;
//        import org.apache.lucene.facet.DrillDownQuery;
//        import org.apache.lucene.facet.DrillSideways.DrillSidewaysResult;
//        import org.apache.lucene.facet.DrillSideways;
//        import org.apache.lucene.facet.FacetField;
//        import org.apache.lucene.facet.FacetResult;
//        import org.apache.lucene.facet.Facets;
//        import org.apache.lucene.facet.FacetsCollector;
//        import org.apache.lucene.facet.FacetsConfig;
//        import org.apache.lucene.facet.taxonomy.FastTaxonomyFacetCounts;
//        import org.apache.lucene.facet.taxonomy.TaxonomyReader;
//        import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyReader;
//        import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyWriter;
//        import org.apache.lucene.index.DirectoryReader;
//        import org.apache.lucene.index.IndexWriter;
//        import org.apache.lucene.index.IndexWriterConfig;
//        import org.apache.lucene.index.IndexWriterConfig.OpenMode;
//        import org.apache.lucene.search.IndexSearcher;
//        import org.apache.lucene.search.MatchAllDocsQuery;
//        import org.apache.lucene.store.Directory;
//        import org.apache.lucene.store.RAMDirectory;
//
///** Shows simple usage of faceted indexing and search. */
//public class SimpleFacetsExample {
//
//    private final Directory indexDir = new RAMDirectory();
//    private final Directory taxoDir = new RAMDirectory();
//    private final FacetsConfig config = new FacetsConfig();
//
//    /** Empty constructor */
//    public SimpleFacetsExample() {
//        config.setHierarchical("Publish Date", true);
//    }
//
//    /** Build the example index. */
//    private void index() throws IOException {
//        IndexWriter indexWriter = new IndexWriter(indexDir, new IndexWriterConfig(
//                new WhitespaceAnalyzer()).setOpenMode(OpenMode.CREATE));
//
//        // Writes facet ords to a separate directory from the main index
//        DirectoryTaxonomyWriter taxoWriter = new DirectoryTaxonomyWriter(taxoDir);
//
//        Document doc = new Document();
//        doc.add(new FacetField("Author", "Bob"));
//        doc.add(new FacetField("Publish Date", "2010", "10", "15"));
//        indexWriter.addDocument(config.build(taxoWriter, doc));
//
//        doc = new Document();
//        doc.add(new FacetField("Author", "Lisa"));
//        doc.add(new FacetField("Publish Date", "2010", "10", "20"));
//        indexWriter.addDocument(config.build(taxoWriter, doc));
//
//        doc = new Document();
//        doc.add(new FacetField("Author", "Lisa"));
//        doc.add(new FacetField("Publish Date", "2012", "1", "1"));
//        indexWriter.addDocument(config.build(taxoWriter, doc));
//
//        doc = new Document();
//        doc.add(new FacetField("Author", "Susan"));
//        doc.add(new FacetField("Publish Date", "2012", "1", "7"));
//        indexWriter.addDocument(config.build(taxoWriter, doc));
//
//        doc = new Document();
//        doc.add(new FacetField("Author", "Frank"));
//        doc.add(new FacetField("Publish Date", "1999", "5", "5"));
//        indexWriter.addDocument(config.build(taxoWriter, doc));
//
//        indexWriter.close();
//        taxoWriter.close();
//    }
//
//    /** User runs a query and counts facets. */
//    private List<FacetResult> facetsWithSearch() throws IOException {
//        DirectoryReader indexReader = DirectoryReader.open(indexDir);
//        IndexSearcher searcher = new IndexSearcher(indexReader);
//        TaxonomyReader taxoReader = new DirectoryTaxonomyReader(taxoDir);
//
//        FacetsCollector fc = new FacetsCollector();
//
//        // MatchAllDocsQuery is for "browsing" (counts facets
//        // for all non-deleted docs in the index); normally
//        // you'd use a "normal" query:
//        FacetsCollector.search(searcher, new MatchAllDocsQuery(), 10, fc);
//
//        // Retrieve results
//        List<FacetResult> results = new ArrayList<>();
//
//        // Count both "Publish Date" and "Author" dimensions
//        Facets facets = new FastTaxonomyFacetCounts(taxoReader, config, fc);
//        results.add(facets.getTopChildren(10, "Author"));
//        results.add(facets.getTopChildren(10, "Publish Date"));
//
//        indexReader.close();
//        taxoReader.close();
//
//        return results;
//    }
//
//    /** User runs a query and counts facets only without collecting the matching documents.*/
//    private List<FacetResult> facetsOnly() throws IOException {
//        DirectoryReader indexReader = DirectoryReader.open(indexDir);
//        IndexSearcher searcher = new IndexSearcher(indexReader);
//        TaxonomyReader taxoReader = new DirectoryTaxonomyReader(taxoDir);
//
//        FacetsCollector fc = new FacetsCollector();
//
//        // MatchAllDocsQuery is for "browsing" (counts facets
//        // for all non-deleted docs in the index); normally
//        // you'd use a "normal" query:
//        searcher.search(new MatchAllDocsQuery(), fc);
//
//        // Retrieve results
//        List<FacetResult> results = new ArrayList<>();
//
//        // Count both "Publish Date" and "Author" dimensions
//        Facets facets = new FastTaxonomyFacetCounts(taxoReader, config, fc);
//
//        results.add(facets.getTopChildren(10, "Author"));
//        results.add(facets.getTopChildren(10, "Publish Date"));
//
//        indexReader.close();
//        taxoReader.close();
//
//        return results;
//    }
//
//    /** User drills down on 'Publish Date/2010', and we
//     *  return facets for 'Author' */
//    private FacetResult drillDown() throws IOException {
//        DirectoryReader indexReader = DirectoryReader.open(indexDir);
//        IndexSearcher searcher = new IndexSearcher(indexReader);
//        TaxonomyReader taxoReader = new DirectoryTaxonomyReader(taxoDir);
//
//        // Passing no baseQuery means we drill down on all
//        // documents ("browse only"):
//        DrillDownQuery q = new DrillDownQuery(config);
//
//        // Now user drills down on Publish Date/2010:
//        q.add("Publish Date", "2010");
//        FacetsCollector fc = new FacetsCollector();
//        FacetsCollector.search(searcher, q, 10, fc);
//
//        // Retrieve results
//        Facets facets = new FastTaxonomyFacetCounts(taxoReader, config, fc);
//        FacetResult result = facets.getTopChildren(10, "Author");
//
//        indexReader.close();
//        taxoReader.close();
//
//        return result;
//    }
//
//    /** User drills down on 'Publish Date/2010', and we
//     *  return facets for both 'Publish Date' and 'Author',
//     *  using DrillSideways. */
//    private List<FacetResult> drillSideways() throws IOException {
//        DirectoryReader indexReader = DirectoryReader.open(indexDir);
//        IndexSearcher searcher = new IndexSearcher(indexReader);
//        TaxonomyReader taxoReader = new DirectoryTaxonomyReader(taxoDir);
//
//        // Passing no baseQuery means we drill down on all
//        // documents ("browse only"):
//        DrillDownQuery q = new DrillDownQuery(config);
//
//        // Now user drills down on Publish Date/2010:
//        q.add("Publish Date", "2010");
//
//        DrillSideways ds = new DrillSideways(searcher, config, taxoReader);
//        DrillSidewaysResult result = ds.search(q, 10);
//
//        // Retrieve results
//        List<FacetResult> facets = result.facets.getAllDims(10);
//
//        indexReader.close();
//        taxoReader.close();
//
//        return facets;
//    }
//
//    /** Runs the search example. */
//    public List<FacetResult> runFacetOnly() throws IOException {
//        index();
//        return facetsOnly();
//    }
//
//    /** Runs the search example. */
//    public List<FacetResult> runSearch() throws IOException {
//        index();
//        return facetsWithSearch();
//    }
//
//    /** Runs the drill-down example. */
//    public FacetResult runDrillDown() throws IOException {
//        index();
//        return drillDown();
//    }
//
//    /** Runs the drill-sideways example. */
//    public List<FacetResult> runDrillSideways() throws IOException {
//        index();
//        return drillSideways();
//    }
//
//    /** Runs the search and drill-down examples and prints the results. */
//    public static void main(String[] args) throws Exception {
//        System.out.println("Facet counting example:");
//        System.out.println("-----------------------");
//        SimpleFacetsExample example = new SimpleFacetsExample();
//        List<FacetResult> results1 = example.runFacetOnly();
//        System.out.println("Author: " + results1.get(0));
//        System.out.println("Publish Date: " + results1.get(1));
//
//        System.out.println("Facet counting example (combined facets and search):");
//        System.out.println("-----------------------");
//        List<FacetResult> results = example.runSearch();
//        System.out.println("Author: " + results.get(0));
//        System.out.println("Publish Date: " + results.get(1));
//
//        System.out.println("Facet drill-down example (Publish Date/2010):");
//        System.out.println("---------------------------------------------");
//        System.out.println("Author: " + example.runDrillDown());
//
//        System.out.println("Facet drill-sideways example (Publish Date/2010):");
//        System.out.println("---------------------------------------------");
//        for(FacetResult result : example.runDrillSideways()) {
//            System.out.println(result);
//        }
//    }
//
//}