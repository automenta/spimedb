//package spimedb.client.util;
//
//import org.teavm.javascript.spi.Sync;
//
//import java.util.ArrayDeque;
//import java.util.Queue;
//
///**
// *
// * @author Alexey Andreev
// */
//public class EventLoop {
//    private static Thread thread;
//    private static final Queue<Runnable> queue = new ArrayDeque<>();
//
//    @Sync
//    public static void submit(Runnable runnable) {
//        if (thread == null) {
//            thread = new Thread(EventLoop::runEventLoop, "EventLoop");
//            thread.start();
//        }
//        queue.add(runnable);
//        new Thread(() -> {
//            synchronized (queue) {
//                queue.notifyAll();
//            }
//        }).start();
//    }
//
//    private static void runEventLoop() {
//        while (true) {
//            Runnable runnable;
//            synchronized (queue) {
//                runnable = queue.poll();
//                if (runnable == null) {
//                    try {
//                        queue.wait();
//                    } catch (InterruptedException e) {
//                        break;
//                    }
//                }
//            }
//            if (runnable != null) {
//                runnable.run();
//            }
//        }
//    }
//
//    public static boolean isInEventLoop() {
//        return Thread.currentThread() == thread;
//    }
//
//    public static void requireEventLoop() {
//        if (!isInEventLoop()) {
//            throw new IllegalStateException("This method should be called from event loop");
//        }
//    }
//}
