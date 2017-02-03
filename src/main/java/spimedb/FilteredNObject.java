package spimedb;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.eclipse.collections.api.set.ImmutableSet;

import java.util.function.BiConsumer;

/**
 * Created by me on 2/3/17.
 */
@JsonSerialize(using = NObject.NObjectSerializer.class)
public class FilteredNObject extends ProxyNObject {

    private final ImmutableSet<String> keysInclude;

    public FilteredNObject(NObject n, ImmutableSet<String> keysInclude) {
        super(n);
        this.keysInclude = keysInclude;
    }

    protected boolean includeKey(String key) {
        return keysInclude.contains(key);
    }

    @Override
    public void forEach(BiConsumer<String, Object> each) {
        n.forEach((k, v) -> {
            if (includeKey(k)) { //HACK filter out tag field because the information will be present in the graph
                Object vv = value(k, v);
                if (vv != null)
                    each.accept(k, vv);
            }
        });
    }

    protected Object value(String k, Object v) {
        return v;
    }
}
