package org.infinispan.server.websocket.handlers;

import io.undertow.websockets.core.WebSocketChannel;
import org.infinispan.Cache;
import org.infinispan.server.websocket.OpHandler;
import org.infinispan.server.websocket.json.JsonObject;

/**
 * Cache "remove" operation handler.
 *
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class RemoveHandler implements OpHandler {

   @Override
   public void handleOp(JsonObject opPayload, Cache<Object, Object> cache, WebSocketChannel ctx) {
      String key = (String) opPayload.get(OpHandler.KEY);
      cache.remove(key);
   }
}
