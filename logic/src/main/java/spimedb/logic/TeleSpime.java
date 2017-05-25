package spimedb.logic;

import jcog.Util;
import org.gridkit.nanocloud.Cloud;
import org.gridkit.nanocloud.CloudFactory;
import org.gridkit.nanocloud.RemoteNode;
import org.gridkit.vicluster.ViNode;
import spimedb.SpimeDB;
import spimedb.SpimePeer;

import java.io.IOException;

/**
 * https://github.com/gridkit/nanocloud-getting-started/blob/master/src/test/java/org/gridkit/lab/examples/nanocloud/BasicViNodeUsage.java
 */
public class TeleSpime {

    public static void main(String[] args) throws IOException {
        Cloud cloud = CloudFactory.createCloud();
        RemoteNode rn = RemoteNode.at(cloud.node("**")).useSimpleRemoting();
        rn.setRemoteAccount("seh");
        rn.setRemoteJavaExec("/home/seh/jdk9/bin/java");


        ViNode x = cloud.node("ana");
        //x.touch();

        new Thread(() -> {

            SpimePeer me = null;
            try {
                me = new SpimePeer(10000, new SpimeDB());
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("connecting...");
            while (!me.isOnline()) {
                me.ping("ana", 10000);
                Util.sleep(1000);
            }
            System.out.println("connected: " + me.summary());
            Util.sleep(5000);
            me.stop();
        }).start();

        cloud.node("**").exec(new Runnable() {
            @Override
            public void run() {

                //String jvmName = ManagementFactory.getRuntimeMXBean().getName();
                System.out.println("System properties: " + System.getProperties());


                try {
                    SpimePeer peer = new SpimePeer(10000, new SpimeDB());
                    System.out.println("ready: " + peer);
                    Util.sleep(5000);
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
