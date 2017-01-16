package spimedb.client.leaflet;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.dom.html.HTMLElement;
import spimedb.client.JsConsumer;

/**
 *
 * @author Alexey Andreev
 */
public abstract class LeafletMap implements JSObject {
    @JSBody(params = { "element", "options" }, script = "return L.map(element, options);")
    public static native LeafletMap create(HTMLElement element, LeafletMapOptions options);

    @JSBody(params = { "elementId", "options" }, script = "return L.map(elementId, options);")
    public static native LeafletMap create(String elementId, LeafletMapOptions options);

    public abstract void remove();

    @JSProperty
    public abstract HTMLElement getMapPane();

    @JSProperty
    public abstract HTMLElement getTilePane();

    @JSProperty
    public abstract HTMLElement getObjectsPane();

    @JSProperty
    public abstract HTMLElement getShadowPane();

    @JSProperty
    public abstract HTMLElement getOverlayPane();

    @JSProperty
    public abstract HTMLElement getMarkerPane();

    @JSProperty
    public abstract HTMLElement getPopupPane();

    /** http://leafletjs.com/reference-1.0.2.html#map-zoomlevelschange */
    @JSBody(params = "listener", script = "return this.on('click', listener);")
    public native final LeafletMap onClick(LeafletEventListener<LeafletMouseEvent> listener);

    @JSBody(params = "listener", script = "return this.on('load', listener);")
    public native final LeafletMap onLoad(JsConsumer listener);

    @JSBody(params = "listener", script = "return this.on('moveend', listener);")
    public native final LeafletMap onMoveEnd(JsConsumer listener);

    @JSBody(params = "listener", script = "return this.on('zoomend', listener);")
    public native final LeafletMap onZoomEnd(JsConsumer listener);

    /**
     *
     * http://leafletjs.com/reference-1.0.2.html#map-getbounds
     */
    @JSBody(params={}, script = "return this.getBounds();")
    public native final JSObject getBounds();


    public abstract LeafletMap setView(LatLng latlng, int zoom);

    public abstract void addLayer(Layer layer);

    public abstract void removeLayer(Layer layer);

    public abstract void setMaxBounds(LatLngBounds bounds);
}
