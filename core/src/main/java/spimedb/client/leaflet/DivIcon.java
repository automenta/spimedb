package spimedb.client.leaflet;

import org.teavm.jso.JSBody;

/**
 * http://leafletjs.com/reference-1.0.2.html#divicon
 */
public abstract class DivIcon extends Icon {

    @JSBody(params = {"cssClass","html"}, script = "return L.divIcon({className: cssClass,html:html,iconSize:[16,16]});")
    public static native DivIcon create(String cssClass, String html);

//     "return new L.DivIcon(options)"
//    @JSBody(params = {"cssClass","ele"}, script = "return L.divIcon({cssClass: cssClass,html:ele});")
//    public static native DivIcon create(String cssClass, HTMLElement ele);

}
