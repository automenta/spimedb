package spimedb.client;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

/**
 * Created by me on 1/13/17.
 */
public interface WebSocket extends JSObject {


    static class Builder {
        @JSBody(params = { "url" },
                script = "const ws = new WebSocket(url);" +
                        "ws.binaryType = 'arraybuffer';" +
                        "return ws;")
        native static WebSocket newSocket(String url);
    }

    static WebSocket newSocket(String host, int port, String path) {
        return Builder.newSocket("ws://" + host + ":" + port + "/" + path);
    }

    void send(JSObject obj);

    void send(String text);

    @JSBody(params = {"each"},
            script = "this.onmessage = ((m) => each(m.data));")
    void onData(JSConsumer<JSObject> each);

    @JSProperty("onopen")
    void setOnOpen(JSRunnable r);

    @JSProperty("onclose")
    void setOnClose(JSRunnable r);
}
