package spimedb.client.leaflet;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

/**
 *
 * @author Alexey Andreev
 */
public abstract class TileLayerOptions implements JSObject {
    @JSBody(params = {}, script = "return {};")
    public static native TileLayerOptions create();

    public final TileLayerOptions minZoom(int minZoom) {
        setMinZoom(minZoom);
        return this;
    }

    public final TileLayerOptions maxZoom(int maxZoom) {
        setMaxZoom(maxZoom);
        return this;
    }

    public final TileLayerOptions tileSize(int tileSize) {
        setTileSize(tileSize);
        return this;
    }

    public final TileLayerOptions attribution(String attribution) {
        setAttribution(attribution);
        return this;
    }

    @JSProperty
    abstract void setMinZoom(int minZoom);

    @JSProperty
    abstract void setMaxZoom(int maxZoom);

    @JSProperty
    abstract void setTileSize(int tileSize);

    @JSProperty
    abstract void setAttribution(String attribution);
}
