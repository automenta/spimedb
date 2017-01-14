package spimedb.client.leaflet;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSProperty;

/**
 *
 * @author Alexey Andreev
 */
public abstract class PolylineOptions extends PathOptions<PolylineOptions> {
    @JSBody(params = {}, script = "return {};")
    public static native PolylineOptions create();

    public final PolylineOptions smoothFactor(double smoothFactor) {
        setSmoothFactor(smoothFactor);
        return this;
    }

    public final PolylineOptions noClip(boolean noClip) {
        setNoClip(noClip);
        return this;
    }

    @JSProperty
    abstract void setSmoothFactor(double smoothFactor);

    @JSProperty
    abstract void setNoClip(boolean noClip);
}
