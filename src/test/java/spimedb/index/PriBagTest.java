package spimedb.index;

import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;

/**
 * Created by me on 1/15/17.
 */
public class PriBagTest {

    @Test
    public void testPriBag() {
//        HashMap<Object,Float> pri = new HashMap();
//        PriBag b = new PriBag(pri::get, 4);
//        b.put("a", 0);

        PriBag<String> s = new PriBag(3, BudgetMerge.add, new HashMap());
        s.put("a", 0.1f);
        s.put("b", 0.3f);
        s.put("c", 0.2f);
        s.put("d", 0.4f);

        //s.commit();

        System.out.println(s);

        assertEquals(3, s.size());

        assertEquals(0.4, s.priMax(), 0.01f);
        assertEquals(0.2, s.priMin(), 0.01f);

        assertEquals("d", s.top().id);
        assertEquals("c", s.bottom().id);


    }
}