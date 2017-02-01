package spimedb;

import java.util.function.Supplier;

/**
 * Created by me on 2/1/17.
 */
public class LazyValue<X> {

    public final String key;
    public final X pendingValue;
    public final Supplier<X> value;

    public LazyValue(String key, X pendingValue, Supplier<X> value) {
        this.key = key;
        this.pendingValue = pendingValue;
        this.value = value;
    }
}
