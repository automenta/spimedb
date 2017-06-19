//package spimedb.query;
//
//import jcog.bloom.StableBloomFilter;
//import org.apache.lucene.index.LeafReaderContext;
//import org.apache.lucene.search.*;
//
//import java.io.IOException;
//
//public class BloomFilterCollector extends Filter {
//
//    private final StableBloomFilter<String> bloom;
//
//    public BloomFilterCollector(Collector in, StableBloomFilter<String> bloom) {
//        super(in);
//        this.bloom = bloom;
//    }
//
//    @Override
//    public LeafCollector getLeafCollector(LeafReaderContext context) throws IOException {
//        return new FilterLeafCollector(super.getLeafCollector(context)) {
//
//
//            @Override public void collect(int doc) throws IOException {
//
//         }
//        };
//    }
//}
