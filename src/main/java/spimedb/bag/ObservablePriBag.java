package spimedb.bag;

import jcog.event.ListTopic;
import jcog.event.Topic;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Created by me on 1/16/17.
 */
public class ObservablePriBag<X> extends PriBag<X> {

    public final Topic<X> ADD = new ListTopic<>();
    public final Topic<X> REMOVE = new ListTopic<>();
    //TODO: public final Topic<ObjectFloatPair<X>> CHANGE = new ArrayTopic<>(); //object and its new value

    public ObservablePriBag(int cap, BudgetMerge mergeFunction, @NotNull Map map) {
        super(cap, mergeFunction, map);
    }

//    @Override
//    public void clear() {
//
//        super.clear();
//    }


    @Override
    protected void onAdded(Budget<X> w) {
        ADD.emit(w.id);
    }

    @Override
    protected void onRemoved(Budget<X> w) {
        REMOVE.emit(w.id);
    }

}
