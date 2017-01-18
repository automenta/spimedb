package spimedb.client.websocket;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.browser.Location;
import org.teavm.jso.browser.Window;
import spimedb.client.util.JSRunnable;
import spimedb.client.util.JsConsumer;

/**
 * Created by me on 1/13/17.
 */
public interface WebSocket extends JSObject {


    /** creates a socket back to the server that provided the location of the current page */
    static WebSocket newSocket(String path) {
        Location l = Window.current().getLocation();
        return newSocket(l.getHostName(), Integer.parseInt(l.getPort()), path);
    }

    static WebSocket newSocket(String host, int port, String path) {
        return Util.newSocket("ws://" + host + ":" + port + "/" + path);
    }

    void close();

    void send(JSObject obj);

    void send(String text);

    default void onText(JsConsumer each) {
        Util.setMessageConsumerText(this, each);
    }

    default void onJSONText(JsConsumer each) {
        Util.setMessageConsumerJSONText(this, each);
    }

    default void onJSONBinary(JsConsumer each) {
        Util.setMessageConsumerJSONBinary(this, each);
    }

    @JSProperty("onopen")
    void setOnOpen(JSRunnable r);

    @JSProperty("onclose")
    void setOnClose(JSRunnable r);

//    default void whereLonLat(float[][] bounds) {
//        send("whereLonLat(" + Arrays.toString(bounds) + ")");
//    }

    class Util {
        @JSBody(params = {"url"},
                script = "const ws = new WebSocket(url);" +
                        "ws.binaryType = 'arraybuffer';" +
                        "return ws;")
        native static WebSocket newSocket(String url);

        @JSBody(params = {"socket", "each"},
                script = "socket.onmessage = function(m) { each(JSON.parse(m.data) };")
        native static void setMessageConsumerJSONText(WebSocket socket, JsConsumer each);

        @JSBody(params = {"socket", "each"},
                script = "socket.onmessage = function(m) { each(msgpack.decode(new Uint8Array(m.data))); };")
        native static void setMessageConsumerJSONBinary(WebSocket socket, JsConsumer each);

        @JSBody(params = {"socket", "each"},
                script = "socket.onmessage = function(m) { each(m.data); };")
        native static void setMessageConsumerText(WebSocket socket, JsConsumer each);

    }
}
