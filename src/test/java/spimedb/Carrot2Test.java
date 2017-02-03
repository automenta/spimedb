//package spimedb;
//
//import org.apache.commons.lang3.StringUtils;
//import org.apache.lucene.analysis.Analyzer;
//import org.apache.lucene.analysis.standard.StandardAnalyzer;
//import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
//import org.apache.lucene.queryparser.classic.QueryParser;
//import org.apache.lucene.search.IndexSearcher;
//import org.apache.lucene.search.Query;
//import org.apache.lucene.search.ScoreDoc;
//import org.apache.lucene.search.TopDocs;
//import org.apache.lucene.store.Directory;
//import org.apache.lucene.store.FSDirectory;
//import org.carrot2.clustering.lingo.LingoClusteringAlgorithm;
//import org.carrot2.core.*;
//import org.carrot2.core.attribute.*;
//import org.carrot2.shaded.guava.common.collect.Maps;
//import org.carrot2.source.SearchEngineResponse;
//import org.carrot2.source.lucene.*;
//import org.carrot2.util.ExceptionUtils;
//import org.carrot2.util.attribute.*;
//import org.carrot2.util.attribute.constraint.ImplementingClasses;
//import org.carrot2.util.attribute.constraint.IntRange;
//import org.carrot2.util.attribute.constraint.NotBlank;
//import org.carrot2.util.simplexml.SimpleXmlWrappers;
//import org.slf4j.Logger;
//
//import java.io.IOException;
//import java.text.NumberFormat;
//import java.util.*;
//
///**
// * Created by me on 2/3/17.
// */
//public class Carrot2Test {
//
//    final SpimeDB spime;
//
//    public Carrot2Test() throws IOException {
//
//        spime = new SpimeDB("/home/me/kml/index");
//
//        /*
//         * We will use the CachingController for this example. Running
//         * LuceneDocumentSource within the CachingController will let us open the index
//         * once per component initialization and not once per query, which would be the
//         * case with SimpleController. We will also use this opportunity to show how
//         * component-specific attribute values can be passed during CachingComponent
//         * initialization.
//         */
//
//        /*
//         * Create a caching controller that will reuse processing component instances, but
//         * will not perform any caching of results produced by components. We will leave
//         * caching of documents from Lucene index to Lucene and the operating system
//         * caches.
//         */
//        final Controller controller = ControllerFactory.createPooling();
//
//        /*
//         * Prepare a map with component-specific attributes. Here, this map will contain
//         * the index location and names of fields to be used to fetch document title and
//         * summary.
//         */
//        final Map<String, Object> luceneGlobalAttributes = new HashMap<String, Object>();
//        luceneGlobalAttributes.put("LuceneDocumentSource.spime", spime);
//
//
//        LuceneDocumentSourceDescriptor
//                .attributeBuilder(luceneGlobalAttributes);
//
//        /*
//         * Specify fields providing data inside your Lucene index.
//         */
//        SimpleFieldMapperDescriptor
//                .attributeBuilder(luceneGlobalAttributes)
//                .titleField("title")
//                .contentField("snippet")
//                .searchFields(Arrays.asList(new String[]{NObject.NAME, NObject.DESC}));
//
//        /*
//         * Initialize the controller passing the above attributes as component-specific
//         * for Lucene. The global attributes map will be empty. Note that we've provided
//         * an identifier for our specially-configured Lucene component, we'll need to use
//         * this identifier when performing processing.
//         */
//        controller.init(new HashMap<String, Object>(),
//                new ProcessingComponentConfiguration(LuceneDocumentSource.class, "lucene",
//                        luceneGlobalAttributes));
//
//
//        /*
//         * Perform processing.
//         */
//        String query = "mining";
//        final Map<String, Object> processingAttributes = Maps.newHashMap();
//        CommonAttributesDescriptor.attributeBuilder(processingAttributes)
//                .query(query);
//
//        /*
//         * We need to refer to the Lucene component by its identifier we set during
//         * initialization. As we've not assigned any identifier to the
//         * LingoClusteringAlgorithm we want to use, we can its fully qualified class name.
//         */
//        ProcessingResult process = controller.process(processingAttributes, "lucene",
//                LingoClusteringAlgorithm.class.getName());
//
//        ConsoleFormatter.displayResults(process);
//
//    }
//
//    public static void main(String[] args) throws IOException {
//        new Carrot2Test();
//    }
//
//    public static class ConsoleFormatter {
//        public static void displayResults(ProcessingResult processingResult) {
//            final Collection<Document> documents = processingResult.getDocuments();
//            final Collection<Cluster> clusters = processingResult.getClusters();
//            final Map<String, Object> attributes = processingResult.getAttributes();
//
//            // Show documents
//            if (documents != null) {
//                displayDocuments(documents);
//            }
//
//            // Show clusters
//            if (clusters != null) {
//                displayClusters(clusters);
//            }
//
//            // Show attributes other attributes
//            displayAttributes(attributes);
//        }
//
//        public static void displayDocuments(final Collection<Document> documents) {
//            System.out.println("Collected " + documents.size() + " documents\n");
//            for (final Document document : documents) {
//                displayDocument(0, document);
//            }
//        }
//
//        public static void displayAttributes(final Map<String, Object> attributes) {
//            System.out.println("Attributes:");
//
//            String DOCUMENTS_ATTRIBUTE = CommonAttributesDescriptor.Keys.DOCUMENTS;
//            String CLUSTERS_ATTRIBUTE = CommonAttributesDescriptor.Keys.CLUSTERS;
//            for (final Map.Entry<String, Object> attribute : attributes.entrySet()) {
//                if (!DOCUMENTS_ATTRIBUTE.equals(attribute.getKey())
//                        && !CLUSTERS_ATTRIBUTE.equals(attribute.getKey())) {
//                    System.out.println(attribute.getKey() + ":   " + attribute.getValue());
//                }
//            }
//        }
//
//        public static void displayClusters(final Collection<Cluster> clusters) {
//            displayClusters(clusters, Integer.MAX_VALUE);
//        }
//
//        public static void displayClusters(final Collection<Cluster> clusters,
//                                           int maxNumberOfDocumentsToShow) {
//            displayClusters(clusters, maxNumberOfDocumentsToShow,
//                    ClusterDetailsFormatter.INSTANCE);
//        }
//
//        public static void displayClusters(final Collection<Cluster> clusters,
//                                           int maxNumberOfDocumentsToShow, ClusterDetailsFormatter clusterDetailsFormatter) {
//            System.out.println("\n\nCreated " + clusters.size() + " clusters\n");
//            int clusterNumber = 1;
//            for (final Cluster cluster : clusters) {
//                displayCluster(0, "" + clusterNumber++, cluster, maxNumberOfDocumentsToShow,
//                        clusterDetailsFormatter);
//            }
//        }
//
//        private static void displayDocument(final int level, Document document) {
//            final String indent = getIndent(level);
//
//            System.out.printf(indent + "[%2s] ", document.getStringId());
//            System.out.println((Object) document.getField(Document.TITLE));
//            final String url = document.getField(Document.CONTENT_URL);
//            if (StringUtils.isNotBlank(url)) {
//                System.out.println(indent + "     " + url);
//            }
//            System.out.println();
//        }
//
//        private static void displayCluster(final int level, String tag, Cluster cluster,
//                                           int maxNumberOfDocumentsToShow, ClusterDetailsFormatter clusterDetailsFormatter) {
//            final String label = cluster.getLabel();
//
//            // indent up to level and display this cluster's description phrase
//            for (int i = 0; i < level; i++) {
//                System.out.print("  ");
//            }
//            System.out.println(label + "  "
//                    + clusterDetailsFormatter.formatClusterDetails(cluster));
//
//            // if this cluster has documents, display three topmost documents.
//            int documentsShown = 0;
//            for (final Document document : cluster.getDocuments()) {
//                if (documentsShown >= maxNumberOfDocumentsToShow) {
//                    break;
//                }
//                displayDocument(level + 1, document);
//                documentsShown++;
//            }
//            if (maxNumberOfDocumentsToShow > 0
//                    && (cluster.getDocuments().size() > documentsShown)) {
//                System.out.println(getIndent(level + 1) + "... and "
//                        + (cluster.getDocuments().size() - documentsShown) + " more\n");
//            }
//
//            // finally, if this cluster has subclusters, descend into recursion.
//            final int num = 1;
//            for (final Cluster subcluster : cluster.getSubclusters()) {
//                displayCluster(level + 1, tag + "." + num, subcluster,
//                        maxNumberOfDocumentsToShow, clusterDetailsFormatter);
//            }
//        }
//
//        private static String getIndent(final int level) {
//            final StringBuilder indent = new StringBuilder();
//            for (int i = 0; i < level; i++) {
//                indent.append("  ");
//            }
//
//            return indent.toString();
//        }
//
//        public static class ClusterDetailsFormatter {
//            public final static ClusterDetailsFormatter INSTANCE = new ClusterDetailsFormatter();
//
//            protected NumberFormat numberFormat;
//
//            public ClusterDetailsFormatter() {
//                numberFormat = NumberFormat.getInstance();
//                numberFormat.setMaximumFractionDigits(2);
//            }
//
//            public String formatClusterDetails(Cluster cluster) {
//                final Double score = cluster.getScore();
//                return "(" + cluster.getAllDocuments().size() + " docs"
//                        + (score != null ? ", score: " + numberFormat.format(score) : "") + ")";
//            }
//        }
//    }
//
//    private final static Logger logger = org.slf4j.LoggerFactory
//            .getLogger(org.carrot2.source.lucene.LuceneDocumentSource.class);
//
//    static {
//        SimpleXmlWrappers.addWrapper(
//                FSDirectory.class,
//                FSDirectoryWrapper.class,
//                false);
//    }
//
//    /**
//     * A {@link IDocumentSource} fetching {@link Document}s from a local Apache Lucene index.
//     * The index should be binary-compatible with the Lucene version actually imported by this
//     * plugin.
//     */
//    @Bindable(prefix = "LuceneDocumentSource", inherit = CommonAttributes.class)
//    public final static class LuceneDocumentSource extends ProcessingComponentBase implements
//            IDocumentSource {
//        protected final static String INDEX_PROPERTIES = "Index properties";
//
//
//
//        /**
//         * Logger for this class.
//         */
//
//        /*
//         * Register selected SimpleXML wrappers for Lucene data types.
//         */
//
//        @Processing
//        @Input
//        @Attribute(key = AttributeNames.RESULTS, inherit = true)
//        @IntRange(min = 1)
//        public int results = 100;
//
//        @Processing
//        @Output
//        @Attribute(key = AttributeNames.RESULTS_TOTAL, inherit = true)
//        public long resultsTotal;
//
//        @Processing
//        @Output
//        @Attribute(key = AttributeNames.DOCUMENTS, inherit = true)
//        @Internal
//        public Collection<Document> documents;
//
//
//        @Input
//        @Attribute
//        @Init
//        @Processing
//        @Required
//        @Internal(configuration = true)
//        @ImplementingClasses(classes =
//                {
//                        SpimeDB.class
//                }, strict = false)
//        @Label("spime")
//        @Level(AttributeLevel.BASIC)
//        @Group(INDEX_PROPERTIES)
//        public SpimeDB spime;
//
//        public Directory directory;
//
//        /**
//         * {@link org.apache.lucene.analysis.Analyzer} used at indexing time. The same
//         * analyzer should be used for querying.
//         */
//        @Input
//        @Init
//        @Processing
//        @Required
//        @Attribute
//        @Internal(configuration = false)
//        @ImplementingClasses(classes =
//                { /* No suggestions for default implementations. */}, strict = false)
//        @Label("Analyzer")
//        @Level(AttributeLevel.MEDIUM)
//        @Group(INDEX_PROPERTIES)
//        public Analyzer analyzer = new StandardAnalyzer();
//
//        /**
//         * {@link IFieldMapper} provides the link between Carrot2
//         * {@link org.carrot2.core.Document} fields and Lucene index fields.
//         */
//        @Input
//        @Init
//        @Processing
//        @Required
//        @Attribute
//        @Internal
//        @ImplementingClasses(classes =
//                {
//                        SimpleFieldMapper.class
//                }, strict = false)
//        @Label("Field mapper")
//        @Level(AttributeLevel.ADVANCED)
//        @Group("Index field mapping")
//        public IFieldMapper fieldMapper = new SimpleFieldMapper();
//
//        /**
//         * A pre-parsed {@link org.apache.lucene.search.Query} object or a {@link String}
//         * parsed using the built-in classic QueryParser over a
//         * set of search fields returned from the {@link #fieldMapper}.
//         */
//        @Input
//        @Processing
//        @Attribute(key = AttributeNames.QUERY, inherit = false) // false intentional!
//        @Required
//        @ImplementingClasses(classes =
//                {
//                        Query.class, String.class
//                }, strict = false)
//        @NotBlank
//        @Label("Query")
//        @Level(AttributeLevel.BASIC)
//        @Group(DefaultGroups.QUERY)
//        public Object query;
//
//        /**
//         * Keeps references to Lucene document instances in Carrot2 documents. Please bear in
//         * mind two limitations:
//         * <ul>
//         * <li><strong>Lucene documents will not be serialized to XML/JSON.</strong>
//         * Therefore, they can only be accessed when invoking clustering through Carrot2 Java
//         * API. To pass some of the fields of Lucene documents to Carrot2 XML/JSON output,
//         * implement a custom {@link IFieldMapper} that will store those fields as regular
//         * Carrot2 fields.</li>
//         * <li><strong>Increased memory usage</strong> when using a {@link org.carrot2.core.Controller}
//         * {@link org.carrot2.core.ControllerFactory#createCachingPooling(Class...) configured to cache} the
//         * output from {@link org.carrot2.source.lucene.LuceneDocumentSource}.</li>
//         * </ul>
//         */
//        @Input
//        @Processing
//        @Attribute
//        @Internal
//        @Label("Keep Lucene documents")
//        @Level(AttributeLevel.ADVANCED)
//        @Group(DefaultGroups.RESULT_INFO)
//        public boolean keepLuceneDocuments = false;
//
//        /**
//         * Carrot2 {@link Document} field that stores the original Lucene document instance.
//         * Keeping of Lucene document instances is disabled by default. Enable it using the
//         * {@link #keepLuceneDocuments} attribute.
//         */
//        public final static String LUCENE_DOCUMENT_FIELD = "luceneDocument";
//
//        /**
//         * A context-shared map between {@link org.apache.lucene.store.Directory} objects and
//         * any opened {@link org.apache.lucene.search.IndexSearcher}s.
//         */
//        private IdentityHashMap<Directory, IndexSearcher> openIndexes;
//
//        /**
//         * Controller context serving as the synchronization monitor when opening indices.
//         */
//        private IControllerContext context;
//
//        /**
//         * A serialization listener that prevents Lucene documents from appearing in the
//         * Carrot2 documents serialized to XML/JSON.
//         */
//        private final Document.IDocumentSerializationListener removeLuceneDocument = new Document.IDocumentSerializationListener() {
//            @Override
//            public void beforeSerialization(Document document,
//                                            Map<String, ?> otherFieldsForSerialization) {
//                otherFieldsForSerialization.remove(LUCENE_DOCUMENT_FIELD);
//            }
//        };
//
//        /*
//         *
//         */
//        @SuppressWarnings("unchecked")
//        @Override
//        public void init(IControllerContext context) {
//            super.init(context);
//            this.context = context;
//
//            synchronized (context) {
//                final String key = AttributeUtils.getKey(getClass(), "openIndexes");
//                if (context.getAttribute(key) == null) {
//                    context.setAttribute(key, Maps.newIdentityHashMap());
//                    context.addListener(new IControllerContextListener() {
//                        public void beforeDisposal(IControllerContext context) {
//                            closeAllIndexes();
//                        }
//                    });
//                }
//
//                this.openIndexes = (IdentityHashMap<Directory, IndexSearcher>) context
//                        .getAttribute(key);
//            }
//        }
//
//        /*
//         *
//         */
//        public void process() throws ProcessingException {
//            try {
//                final SearchEngineResponse response = fetchSearchResponse();
//                documents = response.results;
//                resultsTotal = response.getResultsTotal();
//            } catch (Exception e) {
//                throw ExceptionUtils.wrapAs(ProcessingException.class, e);
//            }
//        }
//
//        /**
//         * Fetch search engine response.
//         */
//        protected SearchEngineResponse fetchSearchResponse() throws Exception {
//            directory = spime.dir;
//
//            if (this.query instanceof String) {
//                final String[] searchFields = fieldMapper.getSearchFields();
//                if (searchFields == null || searchFields.length == 0) {
//                    throw new ProcessingException(
//                            "At least one search field must be given for a plain text query. "
//                                    + "Alternatively, use a Lucene Query object.");
//                }
//
//                final String textQuery = (String) query;
//                if (org.apache.commons.lang.StringUtils.isEmpty(textQuery)) {
//                    throw new ProcessingException(
//                            "An instantiated Lucene Query object or a non-empty "
//                                    + "plain text query is required.");
//                }
//
//                if (searchFields.length == 1) {
//                    query = new QueryParser(searchFields[0], analyzer)
//                            .parse(textQuery);
//                } else {
//                    query = new MultiFieldQueryParser(searchFields, analyzer).parse(textQuery);
//                }
//            }
//
//            final SearchEngineResponse response = new SearchEngineResponse();
//            final IndexSearcher searcher = indexOpen();
//            final TopDocs docs = searcher.search((Query) query, results);
//
//            response.metadata.put(SearchEngineResponse.RESULTS_TOTAL_KEY, docs.totalHits);
//
//            for (ScoreDoc scoreDoc : docs.scoreDocs) {
//                final Document doc = new Document();
//                final org.apache.lucene.document.Document luceneDoc = searcher
//                        .doc(scoreDoc.doc);
//
//                // Set score before mapping to give the mapper a chance to override it
//                doc.setScore((double) scoreDoc.score);
//
//                if (keepLuceneDocuments) {
//                    doc.setField(LUCENE_DOCUMENT_FIELD, luceneDoc);
//                    doc.addSerializationListener(removeLuceneDocument);
//                }
//
//                this.fieldMapper.map((Query) query, analyzer, luceneDoc, doc);
//                response.results.add(doc);
//            }
//
//            return response;
//        }
//
//        /**
//         * Close all opened indexes in the shared context.
//         */
//        private void closeAllIndexes() {
//            synchronized (context) {
//                for (IndexSearcher searcher : openIndexes.values()) {
//                    try {
//                        searcher.getIndexReader().close();
//                    } catch (IOException e) {
//                        logger.warn("Could not close search index: " + searcher, e);
//                    }
//                }
//            }
//        }
//
//        /**
//         * Open or retrieve an open handle to an {@link IndexSearcher}.
//         */
//        private IndexSearcher indexOpen() throws ProcessingException {
//            synchronized (context) {
//                directory = spime.dir;
//                IndexSearcher searcher = openIndexes.get(directory);
//                if (searcher == null) {
//                    searcher = spime.searcher();//new IndexSearcher(DirectoryReader.open(directory));
//                    openIndexes.put(directory, searcher);
//                }
//                return searcher;
//            }
//        }
//    }
//
//}
