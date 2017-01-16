package spimedb.index;

import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by me on 1/15/17.
 */
public class PriBagTest {

    @Test
    public void testPriBag1() {
//        HashMap<Object,Float> pri = new HashMap();
//        PriBag b = new PriBag(pri::get, 4);
//        b.put("a", 0);

        PriBag<String> s = new PriBag(3, BudgetMerge.add, new HashMap());
        s.put("a", 0.1f);
        s.put("b", 0.3f);
        s.put("c", 0.2f);
        s.put("d", 0.4f);



        System.out.println(s);

        assertEquals(3, s.size());

        assertEquals(0.4, s.priMax(), 0.01f);
        assertEquals(0.2, s.priMin(), 0.01f);

        assertEquals("d", s.top().id);
        assertEquals("c", s.bottom().id);


    }
    @Test
    public void testPriBag2() {
//        HashMap<Object,Float> pri = new HashMap();
//        PriBag b = new PriBag(pri::get, 4);
//        b.put("a", 0);

        int cap = 8;
        int vary = 64;
        PriBag<String> s = new PriBag(cap, BudgetMerge.add, new HashMap());

        for (int i = 0; i < 32 * 64; i++) {
            s.put( "x" + (i % vary), 0.05f + (float)Math.random() );
        }

        System.out.println(s);

        assertEquals(cap, s.size());

        assertTrue(s.top().pri > s.bottom().pri);
        //TODO fully test monotonically decreasing-ness

    }

}