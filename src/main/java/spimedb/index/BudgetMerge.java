package spimedb.index;

/**
 * Created by me on 1/15/17.
 */
public interface BudgetMerge {
    /**
     * returns overflow
     */
    float merge(Budget existing, float incoming);

    BudgetMerge add = (e, i) -> {
        e.priAdd(i);
        return 0;
    };
}
