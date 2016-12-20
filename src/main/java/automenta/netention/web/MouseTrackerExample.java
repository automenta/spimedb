package automenta.netention.web;

import io.baratine.web.*;

import java.io.IOException;

public class MouseTrackerExample extends AbstractWeb {

    @WebSocketPath("/mouse")
    public void doUpgradePath(RequestWeb request) {
        request.upgrade(new ServiceWebSocket<String, MousePointer>() {

            public void open(WebSocket<MousePointer> ws) throws Exception {
                ws.next(new MousePointer(0,0));
            }

            @Override
            public void next(String m, WebSocket<MousePointer> ws) throws IOException {
                MousePointer p = object(m, MousePointer.class);
                ws.next(new MousePointer(p._y, p._x));
            }

            public void close(WebSocket<MousePointer> webSocket) {
                //System.out.println("closed websocket connection");
            }
        });
    }

    @Get("/hello")
    public void doHello(RequestWeb request) {
        request.ok(new MousePointer(2,2));
    }

    public static void main(String[] args) {

        Web.include(MouseTrackerExample.class);
        Web.go(args);
    }

}