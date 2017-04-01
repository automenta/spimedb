package spimedb.util.bloom;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by me on 12/25/16.
 */
public class UnBloomFilterTest {

    @Test
    public void testTheBasics() {
        UnBloomFilter<String> filter = new UnBloomFilter<>(32,
                (String x) -> x.getBytes());

        String twentyNineId = "abc";
        String thirtyId = "def";
        String thirtyThreeId = "ghi";
        assertFalse("nothing should be contained at all", filter.containsOrAdd(twentyNineId));
        assertTrue("now it should", filter.containsOrAdd(twentyNineId));
        assertFalse("false unless the hash collides", filter.containsOrAdd(thirtyId));
        assertTrue("original should still return true", filter.containsOrAdd(twentyNineId));
        assertTrue("new array should still return true", filter.containsOrAdd(thirtyId));

        // Handling collisions. {27, 28, 33} and {27, 28, 30} hash to the same index using the current
        // hash function inside OoaBFilter.
        assertFalse("colliding array returns false", filter.containsOrAdd(thirtyThreeId));
        assertTrue(
                "colliding array returns true in second call", filter.containsOrAdd(thirtyThreeId));
        //assertFalse("original colliding array returns false", filter.containsAndAdd(thirtyId));
        assertTrue("original colliding array returns true", filter.containsOrAdd(thirtyId));
        //assertFalse("colliding array returns false", filter.containsAndAdd(thirtyThreeId));
    }
}