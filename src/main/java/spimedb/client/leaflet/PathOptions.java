package spimedb.client.leaflet;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

/**
 *
 * @author Alexey Andreev
 */
public abstract class PathOptions<T extends PathOptions<T>> implements JSObject {
    @SuppressWarnings("unchecked")
    public final T color(String color) {
        setColor(color);
        return (T)this;
    }

    public final T color(int r, int g, int b) {
        return color("rgb(" + r + ',' + g + ',' + b + ')');
    }

    @SuppressWarnings("unchecked")
    public final T weight(double weight) {
        setWeight(weight);
        return (T)this;
    }

    @SuppressWarnings("unchecked")
    public final T opacity(double opacity) {
        setOpacity(opacity);
        return (T)this;
    }

    @JSProperty
    abstract void setColor(String color);

    @JSProperty
    abstract void setWeight(double weight);

    @JSProperty
    abstract void setOpacity(double opacity);
}
