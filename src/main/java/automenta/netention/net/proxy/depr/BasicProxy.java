package automenta.netention.net.proxy.depr;

import automenta.netention.web.SpacetimeWebServer;
import io.undertow.Undertow;
import io.undertow.client.ClientCallback;
import io.undertow.client.ClientConnection;
import io.undertow.client.UndertowClient;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.ServerConnection;
import io.undertow.server.handlers.ResponseCodeHandler;
import io.undertow.server.handlers.proxy.ProxyCallback;
import io.undertow.server.handlers.proxy.ProxyClient;
import io.undertow.server.handlers.proxy.ProxyConnection;
import io.undertow.server.handlers.proxy.ProxyHandler;
import io.undertow.util.AttachmentKey;
import io.undertow.util.Headers;
import org.xnio.ChannelListener;
import org.xnio.IoUtils;
import org.xnio.OptionMap;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.Channel;
import java.util.concurrent.TimeUnit;

/**
 * Created by me on 6/12/15.
 */
public class BasicProxy {

    private static final ProxyClient.ProxyTarget TARGET = new ProxyClient.ProxyTarget() {
    };

    public static class ParameterizedProxyClient implements ProxyClient {

        private final AttachmentKey<ClientConnection> clientAttachmentKey = AttachmentKey.create(ClientConnection.class);
        private final UndertowClient client;

        public ParameterizedProxyClient() {

            this.client = UndertowClient.getInstance();
        }

        public ProxyClient.ProxyTarget findTarget(HttpServerExchange exchange) {
            return TARGET;
        }

        public void getConnection(ProxyClient.ProxyTarget target, HttpServerExchange exchange, ProxyCallback<ProxyConnection> callback, long timeout, TimeUnit timeUnit) {

            String uriTarget = getURI(exchange.getQueryString());


            URI uri = null;
            if (uriTarget!=null) {
                try {
                    uri = new URI(uriTarget);
                } catch (URISyntaxException e) {                }
            }

            exchange.getRequestHeaders().add(Headers.CONTENT_LOCATION, uri.toString());
            exchange.getResponseHeaders().add(Headers.HOST, uri.getHost());
            exchange.getResponseHeaders().add(Headers.CONTENT_LOCATION, uri.toString());
            exchange.getResponseHeaders().add(Headers.LOCATION, uri.toString());
            exchange.getResponseHeaders().add(Headers.URI, uri.toString());

            if (uri == null) {
                //exchange.setResponseCode(404);
                SpacetimeWebServer.send("invalid uri", exchange);
                return;
            }


            //ClientConnection existing = (ClientConnection) exchange.getConnection().getAttachment(this.clientAttachmentKey);
//            if (existing != null) {
//                System.out.println("existing connection: " + existing);
//                if (existing.isOpen()) {
//                    System.out.println("  existing connection is open: " + existing);
//                    callback.completed(exchange, new ProxyConnection(existing, uri.getPath()));
//                    return;
//                }
//                exchange.getConnection().removeAttachment(this.clientAttachmentKey);
//            }

            this.client.connect(new ConnectNotifier(callback, exchange, uri),
                    uri, exchange.getIoThread(), exchange.getConnection().getBufferPool(), OptionMap.EMPTY);
        }

        private String getURI(String queryString) {
            System.out.println("load: " + queryString);

            String uriTarget = queryString;
            if (!uriTarget.startsWith("http://"))
                uriTarget = uriTarget + "http://";

            return uriTarget;
        }

        private final class ConnectNotifier
                implements ClientCallback<ClientConnection> {
            private final ProxyCallback<ProxyConnection> callback;
            private final HttpServerExchange exchange;
            private final URI uri;

            private ConnectNotifier(ProxyCallback<ProxyConnection> callback, HttpServerExchange exchange, URI uri) {
                this.callback = callback;
                this.exchange = exchange;
                this.uri = uri;
            }

            public void completed(final ClientConnection connection) {

                final ServerConnection serverConnection = this.exchange.getConnection();

                //serverConnection.putAttachment(clientAttachmentKey, connection);


                serverConnection.addCloseListener(new ServerConnection.CloseListener() {
                    public void closed(ServerConnection serverConnection) {
                        IoUtils.safeClose(connection);
                    }
                });
                connection.getCloseSetter().set(new ChannelListener() {
                    public void handleEvent(Channel channel) {
                        serverConnection.removeAttachment(clientAttachmentKey);
                    }
                });

                this.callback.completed(this.exchange, new ProxyConnection(connection,
                        uri.toString()));
                        //uri.getPath() == null ? "/" : uri.getPath()));
            }

            public void failed(IOException e) {
                this.callback.failed(this.exchange);
            }
        }

    }

    public static void main(String[] args) {
        Undertow reverseProxy = Undertow.builder()
                .addHttpListener(8080, "localhost")
                .setIoThreads(4)
                .setHandler(new ProxyHandler(
                        new ParameterizedProxyClient(), 30000, ResponseCodeHandler.HANDLE_404, true, true))
                .build();
        reverseProxy.start();
    }

}
