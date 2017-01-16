package spimedb.server;

import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.spi.WebSocketHttpExchange;

import java.io.IOException;

/**
 * Created by me on 1/8/17.
 */
public class SessionSocket extends ServerWebSocket {

    protected void onConnect(WebSocketHttpExchange ex, WebSocketChannel s, Session ss) {

    }

    protected void onMessage(WebSocketChannel socket, BufferedTextMessage message, Session session) {

    }

    @Override
    public void onConnect(WebSocketHttpExchange ex, WebSocketChannel s) {
        super.onConnect(ex, s);
        Session ss = Session.session(ex, s);
        //logger.info("{} connect", ss);
        onConnect(ex, s, ss);
    }


    @Override
    protected void onFullTextMessage(WebSocketChannel socket, BufferedTextMessage message) throws IOException {
        onMessage(socket, message, Session.session(socket));
    }



}
