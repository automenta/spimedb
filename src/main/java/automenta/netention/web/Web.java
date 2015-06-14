package automenta.netention.web;

import automenta.netention.Core;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import nars.util.utf8.Utf8;

import java.io.IOException;
import java.util.Deque;
import java.util.Map;

/**
 * Utility functions for web server processes
 */
public interface Web {
    static void send(String s, HttpServerExchange ex) {
        ex.startBlocking();

        ex.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");

        try {
            ex.getOutputStream().write(Utf8.toUtf8(s));
        } catch (IOException e) {
            SpacetimeWebServer.logger.severe(e.toString());
        }

        ex.getResponseSender().close();
    }

    static void send(JsonNode d, HttpServerExchange ex) {
        ex.startBlocking();

        ex.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");

        try {
            Core.json.writeValue(ex.getOutputStream(), d);
        } catch (IOException ex1) {
            SpacetimeWebServer.logger.severe(ex1.toString());
        }

        ex.getResponseSender().close();
    }

    static String[] getStringArrayParameter(HttpServerExchange ex, String param) throws IOException {
        Map<String, Deque<String>> reqParams = ex.getQueryParameters();

        Deque<String> idArray = reqParams.get(param);

        ArrayNode a = Core.json.readValue(idArray.getFirst(), ArrayNode.class);

        String[] ids = new String[a.size()];
        int j = 0;
        for (JsonNode x : a) {
            ids[j++] = x.textValue();
        }

        return ids;
    }
}
