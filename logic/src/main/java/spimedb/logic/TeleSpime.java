package spimedb.logic;

import jcog.Util;
import org.gridkit.nanocloud.Cloud;
import org.gridkit.nanocloud.CloudFactory;
import org.gridkit.nanocloud.RemoteNode;
import org.gridkit.vicluster.ViNode;
import org.gridkit.vicluster.ViProps;

import java.lang.management.ManagementFactory;
import java.util.concurrent.Callable;

/**
 * https://github.com/gridkit/nanocloud-getting-started/blob/master/src/test/java/org/gridkit/lab/examples/nanocloud/BasicViNodeUsage.java
 */
public class TeleSpime {

    public static void main(String[] args) {
        Cloud cloud = CloudFactory.createCloud();
        RemoteNode rn = RemoteNode.at(cloud.node("**")).useSimpleRemoting();
        rn.setRemoteAccount("seh");
        rn.setRemoteJavaExec("/home/seh/jdk9/bin/java");


        ViNode x = cloud.node("ana");
                //x.touch();

        cloud.node("**").exec(new Callable<Void>() {

            @Override
            public Void call() throws Exception {
                //String jvmName = ManagementFactory.getRuntimeMXBean().getName();
                System.out.println("System properties: " + System.getProperties());
                //Thread.sleep(10000);
                return null;
            }
        });
        Util.sleep(100000);

//        // exec() will invoke task synchronously (but in parallel across nodes)
//        allNodes.exec(new Runnable() {
//            @Override
//            public void run() {
//                System.out.println("Hello");
//            }
//        });
    }
}
