package automenta.netention.web.shell;

import automenta.netention.Core;
import automenta.netention.web.SpacetimeWebServer;
import com.syncleus.spangraph.InfiniPeer;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.StreamSourceFrameChannel;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import nars.NAR;
import nars.io.out.Output;
import nars.io.out.TextOutput;
import nars.model.impl.Default;
import nars.nal.nal8.operator.TermFunction;
import nars.nal.term.Term;

import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Logger;

import static io.undertow.Handlers.websocket;
import static io.undertow.websockets.core.WebSockets.sendText;


public class NARServer extends SpacetimeWebServer {

    final public static Logger broadcast = Logger.getLogger(NARServer.class + ".broadcast");

    private static final int DEFAULT_WEBSOCKET_PORT = 10000;
    static final boolean WEBSOCKET_DEBUG = false;
    


    public NARServer(InfiniPeer p, int httpPort) throws Exception {
        super(p, httpPort);


        addPrefixPath("/nars.io", websocket(new WebSocketConnectionCallback() {

            protected void broadcast(String msg, WebSocketChannel channel) {
                broadcast.info(msg);
                for (WebSocketChannel session : channel.getPeerConnections()) {
                    sendText(msg, session, null);
                }
            }







            @Override
            public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel channel) {

                String uid = Core.uuid();

                broadcast("online(" + uid + "). :|:", channel);

                NAR nar = new NAR(new Default());
                nar.input("schizo(" + uid  + ")!");

                new Output(nar) {

                    @Override
                    public void event(Class aClass, Object... objects) {

                        try {

                            String t = TextOutput.getOutputString(aClass, objects, false, nar, new StringBuilder()).toString();
                            //String t = JSON.stringFrom(objects);

                            //String t = Json.array().add(aClass.getSimpleName()).add(objects).toString();
                            sendText(t, channel, null);

                        }
                        catch (Exception e) {
                            sendText(e.toString(), channel, null);
                        }
                    }
                };

                //http://jmvidal.cse.sc.edu/talks/agentcommunication/performatives.html
                nar.on(new TermFunction("propagate") {

                    @Override
                    public Object function(Term... terms) {
                        broadcast(Arrays.toString(terms), channel);
                        return null;
                    }
                });
                nar.on(new TermFunction("propagate") {

                    @Override
                    public Object function(Term... terms) {
                        broadcast(Arrays.toString(terms), channel);
                        return null;
                    }
                });
                nar.on(new TermFunction("stop") {
                    @Override public Object function(Term... terms) {
                        nar.param.conceptActivationFactor.set(0.15f);
                        return null;
                    }
                });
                nar.on(new TermFunction("go") {
                    @Override public Object function(Term... terms) {
                        nar.param.conceptActivationFactor.set(1f);
                        return null;
                    }
                });

                final Thread runner = new Thread(new Runnable() {

                    public long delayMS = 750;

                    @Override public void run() {

                        while (true) {
                            nar.frame(1);

                            try {
                                Thread.sleep(delayMS);
                            } catch (InterruptedException e) {
                                break;
                            }
                        }

                    }
                });

                runner.start();

                channel.getReceiveSetter().set(new AbstractReceiveListener() {


                    @Override
                    protected void onClose(WebSocketChannel webSocketChannel, StreamSourceFrameChannel channel) throws IOException {
                        broadcast("(--,online(" + uid + ")). :|:", webSocketChannel);

                        if (runner!=null)  {
                            runner.interrupt();

                        }
                        nar.delete();


                        super.onClose(webSocketChannel, channel);
                    }

                    @Override
                    protected void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage message) {
                        final String messageData = message.getData();

                        nar.input(messageData);
                        //nar.run(1);
                    }

                });

                channel.resumeReceives();
            }


        }));



        
    }



    public static void main(String[] args) throws Exception {
                
        int httpPort;
        
        String nlpHost = null;
        int nlpPort = 0;
        
        if (args.length < 1) {
            System.out.println("Usage: NARServer <httpPort> [nlpHost nlpPort] [cycleIntervalMS]");
            
            return;
        }
        else {
            httpPort = Integer.parseInt(args[0]);
            
            if (args.length >= 3) {
                nlpHost = args[1];
                if (!"null".equals(args[2])) {
                    nlpPort = Integer.parseInt(args[2]);
                    //nlp = new NLPInputParser(nlpHost, nlpPort);
                }
            }
//            if (args.length >= 4) {
//                cycleIntervalMS = Integer.parseInt(args[3]);
//            }
        }
                
        NARServer s = new NARServer(InfiniPeer.local("nars"), httpPort);
        
        System.out.println("NARS Web Server ready. port: " + httpPort);
        //System.out.println("  Cycle interval (ms): " + cycleIntervalMS);
        /*if (nlp!=null) {
            System.out.println("  NLP enabled, using: " + nlpHost + ":" + nlpPort);            
        }*/

    }


    
    /**
     * Sends <var>text</var> to all currently connected WebSocket clients.
     *
     * @param text The String to send across the network.
     * @throws InterruptedException When socket related I/O errors occur.
     */
    /*public void sendToAll(String text) {
        Collection<WebSocket> con = connections();
        synchronized (con) {
            for (WebSocket c : con) {
                c.send(text);
            }
        }
    }*/



}
