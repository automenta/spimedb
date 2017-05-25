package spimedb.logic;

import jcog.Util;
import org.gridkit.nanocloud.Cloud;
import org.gridkit.nanocloud.CloudFactory;
import org.gridkit.nanocloud.RemoteNode;
import spimedb.SpimeDB;
import spimedb.SpimePeer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * https://github.com/gridkit/nanocloud-getting-started/blob/master/src/test/java/org/gridkit/lab/examples/nanocloud/BasicViNodeUsage.java
 */
public class TeleSpime {


    public static void main(String[] args) throws UnknownHostException {

        System.setProperty("debug", "true");

        System.out.println(InetAddress.getLocalHost().getCanonicalHostName());

        Thread local = new Thread(() -> {


            try {
                SpimePeer me = new SpimePeer(10000, new SpimeDB());
                me.setFPS(4f);

                System.out.println("connecting");
                for (int i = 0; i < 100; i++) {
                    if (!me.them.isEmpty()) System.out.println(me.summary());
                    else me.ping("ana", 10000);
                    Util.sleep(1000);
                }
                me.stop();
            } catch (IOException e) {
                e.printStackTrace();
            }

        });

        Cloud cloud = CloudFactory.createCloud();
        RemoteNode rn = RemoteNode.at(cloud.node("**")).useSimpleRemoting();
        rn.setRemoteAccount("seh");
        rn.setRemoteJavaExec("/home/seh/jdk9/bin/java");
        rn.setProp("debug", "true");


        local.start();

        cloud.node(/*"**"*/ "ana").exec(new Runnable() {
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
                    SpimePeer peer = new SpimePeer( 10000, new SpimeDB());
                    peer.setFPS(2f);
                    System.out.println("ready: " + peer);

                    for (int i = 0; i < 500; i++) {
                        if (!peer.them.isEmpty()) System.out.println(peer.summary());
                        Util.sleep(1000);
                    }

                    peer.stop();
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
}
