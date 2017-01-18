package spimedb.client.leaflet;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

/**
 *
 * @author Alexey Andreev
 */
public abstract class LeafletMapOptions implements JSObject {
    @JSBody(params = {}, script = "return {};")
    public static native LeafletMapOptions create();

    public final LeafletMapOptions center(LatLng latlng) {
        setCenter(latlng);
        return this;
    }

    public final LeafletMapOptions center(double lat, double lng) {
        return center(LatLng.create(lat, lng));
    }

    public final LeafletMapOptions zoom(double zoom) {
        setZoom(zoom);
        return this;
    }

    public final LeafletMapOptions layers(Layer[] layers) {
        setLayers(layers);
        return this;
    }

    public final LeafletMapOptions minZoom(double zoom) {
        setMinZoom(zoom);
        return this;
    }

    public final LeafletMapOptions maxZoom(double zoom) {
        setMaxZoom(zoom);
        return this;
    }

    public final LeafletMapOptions maxBounds(LatLngBounds bounds) {
        setMaxBounds(bounds);
        return this;
    }

    public final LeafletMapOptions maxBounds(LatLng southWest, LatLng northEast) {
        return maxBounds(LatLngBounds.create(southWest, northEast));
    }

    public final LeafletMapOptions dragging(boolean dragging) {
        setDragging(dragging);
        return this;
    }

    public final LeafletMapOptions touchZoom(boolean touchZoom) {
        setTouchZoom(touchZoom);
        return this;
    }

    public final LeafletMapOptions doubleClickZoom(boolean doubleClickZoom) {
        setDoubleClickZoom(doubleClickZoom);
        return this;
    }

    public final LeafletMapOptions worldCopyJump(boolean b) {
        setWorldCopyJump(b);
        return this;
    }
    public final LeafletMapOptions continuousWorld(boolean b) {
        setContinuousWorld(b);
        return this;
    }

    @JSProperty
    abstract void setCenter(LatLng latlng);

    @JSProperty
    abstract void setZoom(double zoom);

    @JSProperty
    abstract void setLayers(Layer[] layer);

    @JSProperty
    abstract void setMinZoom(double minZoom);

    @JSProperty
    abstract void setMaxZoom(double maxZoom);

    @JSProperty
    abstract void setMaxBounds(LatLngBounds bounds);

    @JSProperty
    abstract void setDragging(boolean dragging);

    @JSProperty
    abstract void setTouchZoom(boolean touchZoom);

    @JSProperty
    abstract void setScrollWheelZoom(boolean scrollWheelZoom);

    @JSProperty
    abstract void setDoubleClickZoom(boolean doubleClickZoom);

    @JSProperty
    abstract void setContinuousWorld(boolean b);

    @JSProperty
    abstract void setWorldCopyJump(boolean b);

}
