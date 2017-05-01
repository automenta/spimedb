package spimedb.util.bag;

import com.google.common.base.Joiner;
import jcog.data.sorted.SortedArray;
import jcog.table.SortedListTable;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * priority queue
 */

public class PriBag<V> extends SortedListTable<V, Budget<V>> implements BiFunction<Budget<V>, Budget<V>, Budget<V>> {

    public final BudgetMerge mergeFunction;


    /**
     * inbound pressure sum since last commit
     */
    public volatile float pressure = 0;

    /**
     * if you dont manually call commit at periodic times (ex: forgetting updates),
     * then use autocommit to update the bag after each insertion
     */
    private static final boolean autocommit = true;


    public PriBag(int cap, BudgetMerge mergeFunction, /*@NotNull*/ Map<V, Budget<V>> map) {
        super(new SortedArray<>(), map);

        this.mergeFunction = mergeFunction;
        this.capacity = cap;
    }

    @Override
    public float floatValueOf(Budget<V> x) {
        return -pCmp(x);
    }

    public boolean containsKey(Object o) {
        return map.containsKey(o);
    }

    @Nullable
    @Override
    public Budget<V> remove(/*@NotNull*/ V x) {
        Budget<V> b = super.remove(x);
        if (b != null) {
            onRemoved(b);
        }
        return b;
    }

    /**
     * returns whether the capacity has changed
     */
    //@Override
    public final boolean setCapacity(int newCapacity) {
        if (newCapacity != this.capacity) {
            synchronized (_items()) {
                this.capacity = newCapacity;
                if (this.size() > newCapacity)
                    commit(null);
            }
            return true;
        }
        return false;
    }

    //@Override
    public final boolean isEmpty() {
        return size() == 0;
    }


    /**
     * returns true unless failed to add during 'add' operation
     */
    //@Override
    protected boolean updateItems(@Nullable Budget<V> toAdd) {


        SortedArray<Budget<V>> items = this.items;

        //List<BLink<V>> pendingRemoval;
        List<Budget> pendingRemoval;
        boolean result;
        synchronized (items) {
            int additional = (toAdd != null) ? 1 : 0;
            int c = capacity();

            int s = size();

            int nextSize = s + additional;
            if (nextSize > c) {
                pendingRemoval = new ArrayList(nextSize - c);
                s = clean(toAdd, s, nextSize - c, pendingRemoval);
                if (s + additional > c) {
                    clean2(pendingRemoval);
                    return false; //throw new RuntimeException("overflow");
                }
            } else {
                pendingRemoval = null;
            }


            if (toAdd != null) {

                //append somewhere in the items; will get sorted to appropriate location during next commit
                //TODO update range

//                Object[] a = items.array();

//                //scan for an empty slot at or after index 's'
//                for (int k = s; k < a.length; k++) {
//                    if ((a[k] == null) /*|| (((BLink)a[k]).isDeleted())*/) {
//                        a[k] = toAdd;
//                        items._setSize(s+1);
//                        return;
//                    }
//                }

                int ss = size();
                if (ss < c) {
                    items.add(toAdd, this);
                    result = true;
                    //items.addInternal(toAdd); //grows the list if necessary
                } else {
                    //throw new RuntimeException("list became full during insert");
                    map.remove(toAdd.id);
                    result = false;
                }

//                float p = toAdd.pri;
//                if (minPri < p && capacity()<=size()) {
//                    this.minPri = p;
//                }


            } else {
                result = size() > 0;
            }

        }

        if (pendingRemoval != null)
            clean2(pendingRemoval);

        return result;

//        if (toAdd != null) {
//            synchronized (items) {
//                //the item key,value should already be in the map before reaching here
//                items.add(toAdd, this);
//            }
//            modified = true;
//        }
//
//        if (modified)
//            updateRange(); //regardless, this also handles case when policy changed and allowed more capacity which should cause minPri to go to -1

    }

    private int clean(@Nullable Budget<V> toAdd, int s, int minRemoved, List<Budget> trash) {

        final int s0 = s;

        if (cleanDeletedEntries()) {
            //first step: remove any nulls and deleted values
            s -= removeDeleted(trash, minRemoved);

            if (s0 - s >= minRemoved)
                return s;
        }

        //second step: if still not enough, do a hardcore removal of the lowest ranked items until quota is met
        s = removeWeakestUntilUnderCapacity(s, trash, toAdd != null);

        return s;
    }


    /**
     * return whether to clean deleted entries prior to removing any lowest ranked items
     */
    protected static boolean cleanDeletedEntries() {
        return false;
    }

    private void clean2(List<Budget> trash) {
        int toRemoveSize = trash.size();
        if (toRemoveSize > 0) {

            for (int i = 0; i < toRemoveSize; i++) {
                Budget w = trash.get(i);
                map.remove(w.id);

//                    if (k2 != w && k2 != null) {
//                        //throw new RuntimeException(
//                        logger.error("bag inconsistency: " + w + " removed but " + k2 + " may still be in the items list");
//                        //reinsert it because it must have been added in the mean-time:
//                        map.putIfAbsent(k, k2);
//                    }

                //pressure -= w.priIfFiniteElseZero(); //release pressure

                onRemoved(w);
                w.delete();

            }

        }


    }

    /**
     * called on eviction
     */
    protected void onRemoved(Budget<V> w) {

    }

    private int removeWeakestUntilUnderCapacity(int s, /*@NotNull*/ List<Budget> toRemove, boolean pendingAddition) {
        SortedArray<Budget<V>> items = this.items;
        final int c = capacity;
        while (!isEmpty() && ((s - c) + (pendingAddition ? 1 : 0)) > 0) {
            Budget<V> w = items.remove(s - 1);
            if (w != null) //skip over nulls
                toRemove.add(w);
            s--;
        }
        return s;
    }

    @Nullable
    //@Override
    public V activate(Object key, float toAdd) {
        Budget<V> c = map.get(key);
        if (c != null && !c.isDeleted()) {
            //float dur = c.dur();
            float pBefore = c.pri;
            c.priAdd(toAdd);
            float delta = c.pri - pBefore;
            pressure += delta;// * dur;
            return c.id;
        }
        return null;
    }

    public void mul(float factor) {
        forEach(b->b.priMult(factor));
    }
    public void add(float inc) {
        forEach(b->b.priAdd(inc));
    }

    //@Override
    public V mul(Object key, float factor) {
        Budget<V> c = map.get(key);
        if (c != null) {
            float pBefore = c.pri;
            if (pBefore != pBefore)
                return null; //already deleted

            c.priMult(factor);
            float delta = c.pri - pBefore;
            pressure += delta;// * dur;
            return c.id;
        }
        return null;

    }



    //    //@Override
//    public final int compare(@Nullable BLink o1, @Nullable BLink o2) {
//        float f1 = cmp(o1);
//        float f2 = cmp(o2);
//
//        if (f1 < f2)
//            return 1;           // Neither val is NaN, thisVal is smaller
//        if (f1 > f2)
//            return -1;            // Neither val is NaN, thisVal is larger
//        return 0;
//    }


    /**
     * true iff o1 > o2
     */
    static final boolean cmpGT(@Nullable Budget o1, @Nullable Budget o2) {
        return cmpGT(o1, pCmp(o2));
    }

    static final boolean cmpGT(@Nullable Budget o1, float o2) {
        return (pCmp(o1) < o2);
    }

    /**
     * true iff o1 > o2
     */
    static final boolean cmpGT(float o1, @Nullable Budget o2) {
        return (o1 < pCmp(o2));
    }


    /**
     * true iff o1 < o2
     */
    static final boolean cmpLT(@Nullable Budget o1, @Nullable Budget o2) {
        return cmpLT(o1, pCmp(o2));
    }

    static final boolean cmpLT(@Nullable Budget o1, float o2) {
        return (pCmp(o1) > o2);
    }

    /**
     * gets the scalar float value used in a comparison of BLink's
     * essentially the same as b.priIfFiniteElseNeg1 except it also includes a null test. otherwise they are interchangeable
     */
    static float pCmp(@Nullable Budget b) {
        return (b == null) ? -2f : b.pri; //sort nulls beneath

//        float p = b.pri;
//        return p == p ? p : -1f;
        //return (b!=null) ? b.priIfFiniteElseNeg1() : -1f;
        //return b.priIfFiniteElseNeg1();
    }


    //@Override
    public final V key(/*@NotNull*/ Budget<V> l) {
        return l.id;
    }



    public final Budget<V> put(/*@NotNull*/ V key, float pri) {
        return put(key, pri, null);
    }

    //@Override
    public final Budget<V> put(/*@NotNull*/ V key, float pri, @Nullable MutableFloat overflow) {


        if (pri < 0) { //already deleted
            onRemoved(new Budget<>(key, pri)); //HACK maybe use a separate handler, for onRejected
            return null;
        }

        pressure += pri;


        Budget<V> existing = map.get(key);

        if (existing != null) {
            //result=0
            //Budget vv = existing.clone();
            if (existing.isDeleted()) {
                //it has been deleted.. TODO reinsert?
                map.remove(key);
                pressure -= pri;

                onRemoved(existing);
                return null;
            }


            //re-rank
            float o = mergeFunction.merge(existing, pri);

            if (autocommit)
                sort();


            if (o > 0) {
                if (overflow != null)
                    overflow.add(o);
                pressure -= o;
            }

            return existing;

        } else {
            if (size() >= capacity && pri < priMin() /* < here rather than <= allows flat FIFO replacement */) {

                //reject due to insufficient budget
                if (overflow != null) {
                    overflow.add(pri);
                }
                pressure -= pri;

                onRemoved(new Budget<>(key, pri));
                return null;

            } else {

                //accepted for fresh insert
                Budget next = new Budget<>(key, pri);

                map.put(key, next);

                synchronized (items) {
                    if (updateItems(next)) {
                        onAdded(next); //success
                        return next;
                    } else {
                        onRemoved(next);
                        return null;
                    }
                }

            }
        }

    }

    protected void onAdded(Budget<V> w) {

    }

//    /**
//     * the applied budget will not become effective until commit()
//     */
//    /*@NotNull*/
//    protected final void putExists(/*@NotNull*/ Budgeted b, float scale, /*@NotNull*/ BLink<V> existing, @Nullable MutableFloat overflow) {
//
//
//
//    }

//    /*@NotNull*/
//    protected final BLink<V> newLink(/*@NotNull*/ V i, /*@NotNull*/ Budgeted b) {
//        return newLink(i, b, 1f);
//    }

//    /*@NotNull*/
//    protected final BLink<V> newLink(/*@NotNull*/ V i, /*@NotNull*/ Budgeted b, float scale) {
//        return newLink(i, scale * b.pri, b.dur(), b.qua());
//    }


    @Nullable
    //@Override
    protected final Budget<V> addItem(Budget<V> i) {
        throw new UnsupportedOperationException();
    }


    /*@NotNull*/
    public final PriBag<V> commit(@Nullable Function<PriBag, Consumer<Budget>> update) {

        synchronized (items) {

            update(update != null ? update.apply(this) : null);

        }

        return this;
    }

    public float mass() {
        float mass = 0;
        synchronized (items) {
            int iii = size();
            for (int i = 0; i < iii; i++) {
                Budget x = get(i);
                if (x != null)
                    mass += x.priSafe(0);
            }
        }
        return mass;
    }


    /**
     * applies the 'each' consumer and commit simultaneously, noting the range of items that will need sorted
     */
    /*@NotNull*/
    protected PriBag<V> update(@Nullable Consumer<Budget> each) {


        synchronized (items) {

            if (each != null)
                this.pressure = 0; //reset pressure accumulator

            if (size() > 0) {
                if (updateItems(null)) {

                    updateBudget(each);

                }

                sort();

            }

        }


        return this;
    }

    public void sort() {
        int size = size();
        if (size > 1)
            qsort(new short[16 /* estimate */], items.array(), (short) 0 /*dirtyStart - 1*/, (short) (size - 1));
    }

    /**
     * returns the index of the lowest unsorted item
     */
    private void updateBudget(@Nullable Consumer<Budget> each) {
//        int dirtyStart = -1;


        int s = size();
        Budget<V>[] l = items.array();
        int i = s - 1;
        for (; i >= 0; ) {
            Budget<V> b = l[i];

            float bCmp;
            bCmp = b != null ? b.priSafe(-1) : -2; //sort nulls to the end of the end

            if (bCmp > 0) {
                if (each != null)
                    each.accept(b);
            }

            i--;
        }

    }


    private int removeDeleted(/*@NotNull*/ List<Budget> removed, int minRemoved) {

        SortedArray<Budget<V>> items = this.items;
        final Object[] l = items.array();
        int removedFromMap = 0;

        //iterate in reverse since null entries should be more likely to gather at the end
        for (int s = size() - 1; removedFromMap < minRemoved && s >= 0; s--) {
            Budget x = (Budget) l[s];
            if (x == null || x.isDeleted()) {
                items.removeFast(s);
                if (x != null)
                    removed.add(x);
                removedFromMap++;
            }
        }

        return removedFromMap;
    }

    //@Override
    public void clear() {
        synchronized (items) {
            //map is possibly shared with another bag. only remove the items from it which are present in items
            items.forEach(x -> {
                map.remove(x.id);
                onRemoved(x);
            });
            items.clear();

        }
    }


    @Nullable
    @Override
    public Budget apply(@Nullable Budget bExisting, Budget bNext) {
        if (bExisting != null) {
            mergeFunction.merge(bExisting, bNext.pri);
            return bExisting;
        } else {
            return bNext;
        }
    }

    //@Override
    public void forEach(Consumer<? super Budget<V>> action) {
        Object[] x = items.array();
        if (x.length > 0) {
            for (Budget a : ((Budget[]) x)) {
                if (a != null) {
                    Budget<V> b = a;
                    if (!b.isDeleted())
                        action.accept(b);
                }
            }
        }
    }


    /**
     * http://kosbie.net/cmu/summer-08/15-100/handouts/IterativeQuickSort.java
     */

    static void qsort(short[] stack, Budget[] c, short left, short right) {
        int stack_pointer = -1;
        int cLenMin1 = c.length - 1;
        while (true) {
            short i, j;
            if (right - left <= 7) {
                Budget swap;
                //bubble sort on a region of right less than 8?
                for (j = (short) (left + 1); j <= right; j++) {
                    swap = c[j];
                    i = (short) (j - 1);
                    float swapV = pCmp(swap);
                    while (i >= left && cmpGT(c[i], swapV)) {
                        swap(c, (short) (i + 1), i);
                        i--;
                    }
                    c[i + 1] = swap;
                }
                if (stack_pointer != -1) {
                    right = stack[stack_pointer--];
                    left = stack[stack_pointer--];
                } else {
                    break;
                }
            } else {
                Budget swap;

                short median = (short) ((left + right) / 2);
                i = (short) (left + 1);
                j = right;

                swap(c, i, median);

                if (cmpGT(c[left], c[right])) {
                    swap(c, right, left);
                }
                if (cmpGT(c[i], c[right])) {
                    swap(c, right, i);
                }
                if (cmpGT(c[left], c[i])) {
                    swap(c, i, left);
                }

                {
                    Budget temp = c[i];
                    float tempV = pCmp(temp);

                    while (true) {
                        while (i < cLenMin1 && cmpLT(c[++i], tempV)) ;
                        while (cmpGT(c[--j], tempV)) ;
                        if (j < i) {
                            break;
                        }
                        swap(c, j, i);
                    }

                    c[left + 1] = c[j];
                    c[j] = temp;
                }

                short a, b;
                if ((right - i + 1) >= (j - left)) {
                    a = i;
                    b = right;
                    right = (short) (j - 1);
                } else {
                    a = left;
                    b = (short) (j - 1);
                    left = i;
                }

                stack[++stack_pointer] = a;
                stack[++stack_pointer] = b;
            }
        }
    }

    static void swap(Budget[] c, short x, short y) {
        Budget swap;
        swap = c[y];
        c[y] = c[x];
        c[x] = swap;
    }


    /*@NotNull*/
    @Override
    public String toString() {
        return Joiner.on(", ").join(items);// + '{' + items.getClass().getSimpleName() + '}';
    }


    static float itemOrZeroIfNull(Budget x) {
        return x != null ? x.pri : 0f;
    }

    //@Override
    public float priMax() {
        return itemOrZeroIfNull(items.first());
    }

    //@Override
    public float priMin() {
        return itemOrZeroIfNull(items.last());
    }

    public float pri(V id, float valueIfAbsent) {
        Budget<V> b = map.get(id);
        if (b == null) {
            return valueIfAbsent;
        }
        return b.pri;
    }
}