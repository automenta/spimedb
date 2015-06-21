package org.infinispan.server.websocket;

import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import org.infinispan.Cache;
import org.infinispan.server.websocket.json.JsonConversionException;
import org.infinispan.server.websocket.json.JsonObject;

/**
 * Channel Utilities.
 *
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class ChannelUtils {

    //private static final Log logger = LogFactory.getLog(MethodHandles.lookup().lookupClass(), Log.class);

    /**
     * Push a cache entry value out onto the websocket channel (to the browser).
     *
     * @param key    The cache entry key whose value is to be pushed to the browser.
     * @param cache  The cache containing the key.
     * @param socket The channel context associated with the browser websocket channel..
     */
    public static void pushCacheValue(String key, Cache<Object, Object> cache, WebSocketChannel socket) {
        Object value = cache.get(key);

        JsonObject responseObject = toJSON(key, value, cache.getName());

        // Write the JSON response out onto the channel...
        WebSockets.sendText(responseObject.toString(), socket, null);

        //socket. channel().writeAndFlush(new TextWebSocketFrame(responseObject.toString()));
    }

    /**
     * Cache key, value and cache-name to JSON string.
     * <p>
     * Note that value objects (like String, Numbers, Characters) are not being converted.
     * </p>
     *
     * @param key       The cache key.
     * @param value     The cache value.
     * @param cacheName The cache name.
     * @return JSON Object representing a cache entry payload for transmission to the browser channel.
     * @throws IllegalStateException In case of complex object which can not be converted to JSON.
     */
    public static JsonObject toJSON(String key, Object value, String cacheName) {
        JsonObject jsonObject = JsonObject.createNew();

        jsonObject.put(OpHandler.CACHE_NAME, cacheName);
        jsonObject.put(OpHandler.KEY, key);

        if (value != null) {
            if (needsJsonConversion(value)) {
                JsonObject valueObject = getJsonObject(value);
                jsonObject.put(OpHandler.VALUE, valueObject.toString());
                jsonObject.put(OpHandler.MIME, "application/json");
            } else {
                jsonObject.put(OpHandler.VALUE, value);
                jsonObject.put(OpHandler.MIME, "text/plain");
            }
        } else {
            jsonObject.put(OpHandler.VALUE, null);
        }

        return jsonObject;
    }

    private static JsonObject getJsonObject(Object value) {
        JsonObject valueObject = null;
        try {
            valueObject = JsonObject.fromObject(value);
        } catch (JsonConversionException e) {
            e.printStackTrace();
            //throw logger.unableToGetFieldsFromObject(e);
        }
        return valueObject;
    }

    private static boolean needsJsonConversion(Object value) {
        if (value instanceof String)
            return false;
        if (value instanceof Number)
            return false;
        if (value instanceof Character)
            return false;
        return true;
    }
}
