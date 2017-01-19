package spimedb.bag;

import org.jetbrains.annotations.Nullable;
import org.teavm.jso.core.JSFunction;
import spimedb.client.lodash.Lodash;
import spimedb.client.util.Console;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

/**
 * batches and notifies listeners about bag changes
 * @param X the input key
 * @param Y the output value, lazily constructed
 */
abstract public class ChangeBatcher<X, Y> {

    private final JSFunction onChange;
    final Map<X, Boolean> changed = new HashMap();
    final Map<X, Y> built = new HashMap();

    static final Object[] empty = new Object[0];

    /** to obey an existing remove command when trying to add */
    private static final boolean RemoveOverrides = true;


    public ChangeBatcher(int updateMS, IntFunction<Y[]> arrayBuilder) {

        onChange = Lodash.throttle(() -> {


            List<Y> ADD = new LinkedList();
            List<Y> REM = new LinkedList();
            int addC = 0, remC = 0; //since linkedlist size() is slow

            for (Map.Entry<X, Boolean> e : changed.entrySet()) {

                X x = e.getKey();
                Boolean s = e.getValue();

                if (s == TRUE) {
                    Y y = build(x);
                    if (y == null)
                        continue;

                    Y existed = built.put(x, y);
                    assert(existed==null); if (existed!=null) Console.log("previous exist");
                    ADD.add(y);
                    addC++;
                } else if (s == FALSE){
                    Y removed = built.remove(x);
                    assert(removed!=null); if (removed==null) Console.log("didnt exist");
                    REM.add(removed);
                    remC++;
                } else {
                    Console.log("unknown value");
                    assert(false);
                }
            }

            if (addC > 0 || remC > 0) {
                update(ADD.toArray(addC > 0 ? arrayBuilder.apply(addC) : (Y[]) empty),
                        REM.toArray(remC > 0 ? arrayBuilder.apply(remC) : (Y[]) empty));
            }

            changed.clear();


        }, updateMS);
    }

    @Nullable
    abstract public Y build(X x);

    abstract public void update(Y[] added, Y[] removed);

    protected boolean setNext(X x, boolean addOrRem) {
        boolean exist = built.get(x) != null;
        if (addOrRem) {
            //add
            if (exist) {
                changed.remove(x); //in case remove was pending
                return false; //already exist, ignore
            }

        } else {
            //remove
            if (!exist) {
                changed.remove(x); //in case add was pending
                return false; //doesnt exist, ignore
            }
        }

        changed.put(x, addOrRem);
        onChange.call(null);
        return true;
    }

    public void add(X x) {
        setNext(x, TRUE);
    }

    public void remove(X x) {
        setNext(x, FALSE);
    }
}
