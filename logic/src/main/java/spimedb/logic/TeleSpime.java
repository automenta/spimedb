package spimedb.logic;

import jcog.Util;
import org.gridkit.nanocloud.Cloud;
import org.gridkit.nanocloud.CloudFactory;
import org.gridkit.nanocloud.RemoteNode;
import org.jetbrains.annotations.NotNull;
import spimedb.SpimeDB;
import spimedb.SpimePeer;
import spimedb.media.Multimedia;
import spimedb.server.WebServer;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * https://github.com/gridkit/nanocloud-getting-started/blob/master/src/test/java/org/gridkit/lab/examples/nanocloud/BasicViNodeUsage.java
 */
public class TeleSpime {


    public static void main(String[] args) {

        System.setProperty("debug", "true");

        //System.out.println(InetAddress.getLocalHost().getCanonicalHostName());


        Cloud cloud = CloudFactory.createCloud();
        RemoteNode rn = RemoteNode.at(cloud.node("**")).useSimpleRemoting();
        rn.setRemoteAccount("seh");
        //rn.setRemoteJavaExec("/home/seh/jdk9/bin/java");
        rn.setRemoteJavaExec("/jdk9/bin/java");
        rn.setProp("debug", "true");


        //Thread local = local();
        //local.start();

        cloud.node(/*"**"*/ "ea").exec(new Runnable() {
            @Override
            public void run() {

                //String jvmName = ManagementFactory.getRuntimeMXBean().getName();
                System.out.println("System properties: " + System.getProperties());
                try {
                    System.out.println(InetAddress.getLocalHost().getCanonicalHostName());
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }


                try {
                    int port = 8090;

                    File home =
                            //null;
                            new File("/home/seh/doc");

                    SpimeDB db = new SpimeDB(home);
                    SpimePeer peer = new SpimePeer(port, db);
                    new Multimedia(db);

                    WebServer ws = new WebServer(db);
                    ws.setPort(port);

                    peer.runFPS(0.5f);
                    System.out.println("ready: " + peer);

//                    for (int i = 0; i < 500; i++) {
//                        if (!peer.them.isEmpty()) System.out.println(peer.summary());
//                        Util.sleep(1000);
//                    }
//
//                    peer.stop();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });


//        // exec() will invoke task synchronously (but in parallel across nodes)
//        allNodes.exec(new Runnable() {
//            @Override
//            public void run() {
//                System.out.println("Hello");
//            }
//        });
    }

    @NotNull
    private static Thread local() {
        return new Thread(() -> {


                try {
                    SpimePeer me = new SpimePeer(10000,
                        new SpimeDB(new File("/home/me/doc/"))
                        //new SpimeDB()
                    );
                    me.runFPS(4f);

                    System.out.println("connecting");
                    for (int i = 0; i < 100000; i++) {
                        if (!me.them.isEmpty()) System.out.println(me.summary());
                        else me.ping("ana", 10000);
                        Util.sleep(1000);
                    }
                    me.stop();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            });
    }
}
