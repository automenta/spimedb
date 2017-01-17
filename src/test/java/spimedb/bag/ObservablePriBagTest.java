package spimedb.bag;

import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;

/**
 * Created by me on 1/16/17.
 */
public class ObservablePriBagTest {

    @Test
    public void testObserveAddRemove() {


        ObservablePriBag<String> b = new ObservablePriBag<>(3, BudgetMerge.add, new HashMap<>());
        StringBuilder seq = new StringBuilder(1024);
        b.ADD.on(v -> seq.append('+').append(v).append(' '));
        b.REMOVE.on(v -> seq.append('-').append(v).append(' '));

        b.put("a", 0.1f);
        b.put("b", 0.1f);

        b.put("a", 0.1f); //should not retrigger add event
        b.remove("b");

        b.clear(); //should register as a removal

        assertEquals("+a +b -b -a ", seq.toString());
        seq.setLength(0);

        b.put("a", 0.15f);
        b.put("b", 0.20f);
        b.put("c", 0.25f);
        b.put("d", 0.25f);
        b.put("b", 0.25f); //should not re-trigger since it's already present

        assertEquals("+a +b +c -a +d ", seq.toString());
        seq.setLength(0);

    }
}