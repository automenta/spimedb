package org.infinispan.server.websocket;

import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import org.infinispan.Cache;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryCreated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryRemoved;
import org.infinispan.notifications.cachelistener.event.*;
import org.infinispan.server.websocket.json.JsonObject;
import org.xnio.ChannelListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Cache listener.
 * <p>
 * Used to notify websocket clients of cache entry updates.
 *
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
@Listener
public class CacheListener {

    //private static final Log logger = LogFactory.getLog(MethodHandles.lookup().lookupClass(), Log.class);

    private List<ChannelNotifyParams> channels = new CopyOnWriteArrayList();

    @CacheEntryCreated
    public void cacheEntryCreated(CacheEntryCreatedEvent<Object, Object> event) {
        notifyChannels(event, event.getType());
    }

    @CacheEntryModified
    public void cacheEntryModified(CacheEntryModifiedEvent<Object, Object> event) {
        notifyChannels(event, event.getType());
    }

    @CacheEntryRemoved
    public void cacheEntryRemoved(CacheEntryRemovedEvent<Object, Object> event) {
        notifyChannels(event, event.getType());
    }

    private void notifyChannels(CacheEntryEvent<Object, Object> event, Event.Type eventType) {
        if (event.isPre()) {
            return;
        }

        JsonObject jsonObject;

        Cache<Object, Object> cache = event.getCache();
        Object key = event.getKey();
        Object value;

        switch (eventType) {
            case CACHE_ENTRY_CREATED:
                // TODO: Add optimization ... don't get from cache if non of the channels are interested in creates...
                value = cache.get(key);
                jsonObject = ChannelUtils.toJSON(key.toString(), value, cache.getName());
                break;
            case CACHE_ENTRY_MODIFIED:
                value = event.getValue();
                jsonObject = ChannelUtils.toJSON(key.toString(), value, cache.getName());
                break;
            case CACHE_ENTRY_REMOVED:
                jsonObject = ChannelUtils.toJSON(key.toString(), null, cache.getName());
                break;
            default:
                return;
        }

        jsonObject.put("eventType", eventType.toString());

        String jsonString = jsonObject.toString();
        for (ChannelNotifyParams channel : channels) {
            if (channel.channel.isOpen() && channel.onEvents.contains(eventType)) {
                if (channel.key != null) {
                    if (event.getKey().equals(channel.key) || channel.key.equals("*")) {
                        WebSockets.sendText(jsonString, channel.channel, null);
                    }
                } else {
                    WebSockets.sendText(jsonString, channel.channel, null);
                }
            }
        }
    }

    public void addChannel(ChannelNotifyParams channel) {
        if (!channels.contains(channel)) {
            channels.add(channel);

            channel.channel.addCloseTask(new ChannelListener<WebSocketChannel>() {
                @Override
                public void handleEvent(WebSocketChannel webSocketChannel) {
                    removeChannel(channel);
                }
            });
            try {
                channel.channel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }


    }

    public void removeChannel(ChannelNotifyParams channel) {
        channels.remove(channel);
    }

    public static class ChannelNotifyParams {

        private static final String[] DEFAULT_EVENTS = {Event.Type.CACHE_ENTRY_MODIFIED.toString(), Event.Type.CACHE_ENTRY_REMOVED.toString()};

        public final WebSocketChannel channel;
        public final String key;
        public final List<Event.Type> onEvents = new ArrayList<Event.Type>();

        public ChannelNotifyParams(WebSocketChannel channel, String key, String[] onEvents) {
            if (channel == null) {
                //logger.invalidNullArgument("channel");
            }
            String[] onEventsSpec = onEvents;

            this.channel = channel;
            this.key = key;

            if (onEventsSpec == null) {
                onEventsSpec = DEFAULT_EVENTS;
            }
            for (String eventType : onEventsSpec) {
                try {
                    this.onEvents.add(Event.Type.valueOf(eventType));
                } catch (RuntimeException e) {
                    // Ignore for now
                    //logger.debug("Runtime exception on adding events", e);
                }
            }

            if (onEvents == null && key.equals("*")) {
                this.onEvents.add(Event.Type.CACHE_ENTRY_CREATED);
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof ChannelNotifyParams) {
                ChannelNotifyParams channelNotifyParams = (ChannelNotifyParams) obj;
                if (channelNotifyParams.channel == channel) {
                    if (key == null) {
                        return (channelNotifyParams.key == null);
                    } else {
                        return key.equals(channelNotifyParams.key);
                    }
                }
            }

            return false;
        }

        @Override
        public int hashCode() {
            if (key != null) {
                return super.hashCode() + channel.hashCode() + key.hashCode();
            } else {
                return super.hashCode() + channel.hashCode();
            }
        }
    }

//    class ChannelCloseFutureListener implements ChannelListener {
//
//
//        @Override
//        public void handleEvent(Channel channel) {
//            /*for (ChannelNotifyParams channel : channel) {
//                if (channelCloseFuture.channel() == channel.channel) {
//                    removeChannel(channel);
//                }
//            }*/
//        }
//    }
}
