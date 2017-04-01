//package spimedb;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
///**
// * Created by me on 2/23/17.
// */
//public class Daemon {
//
//    public static final Logger logger = LoggerFactory.getLogger(Daemon.class);
//
//    public static void main(String[] args) throws Exception {
//        com.sun.akuma.Daemon d = new com.sun.akuma.Daemon.WithoutChdir();
//        if (!d.isDaemonized()) {
//            // when un-daemonized:
//
//            logger.info("fork");
//            d.daemonize();
//            System.exit(0);
//        } else {
//            // perform initialization as a daemon
//            // this involves in closing file descriptors, recording PIDs, etc.
//            d.init("/tmp/spimedb.pid");
//        }
//
//        logger.info("start");
//        Main.main(args);
//    }
//
//
//
//}
