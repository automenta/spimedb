package spimedb.bag;

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
    BudgetMerge max = (e, i) -> {
        e.pri(Math.max(e.pri, i));
        return 0; //TODO calculate overflow
    };
    BudgetMerge and = (e, i) -> {
        e.pri( e.pri * i );
        return 0; //TODO calculate overflow
    };
    BudgetMerge or = (e, i) -> {
        e.pri( 1f - ((1f - e.pri) * (1f - i)) );
        return 0; //TODO calculate overflow
    };
}
