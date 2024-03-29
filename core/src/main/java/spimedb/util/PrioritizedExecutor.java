package spimedb.util;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static jcog.Str.n2;

/**
 * Created by me on 2/3/17.
 */
public class PrioritizedExecutor implements Executor {

    final static Logger logger = LoggerFactory.getLogger(PrioritizedExecutor.class);

    private static final float DEFAULT_PRIORITY = 0.5f;

    private static final long DEFAULT_TIMEOUT_ms = 2 * 60 * 1000;

    public final PriorityBlockingQueue pq = new PriorityBlockingQueue<>(
            512 * 1024,
            runCompare);

    public final ExecutorService exe;
    public AtomicInteger running = new AtomicInteger();
    public int concurrency;

    public PrioritizedExecutor(int threads) {
        //similar to Fixed-Size threadpool
        this.concurrency = threads;
        this.exe = new ThreadPoolExecutor(threads, threads, 5, TimeUnit.MINUTES, pq) {

            @Override
            protected void beforeExecute(Thread t, Runnable r) {
                running.incrementAndGet();
                //logger.debug("start {}", r);
            }

            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                //logger.debug("  end {}", r);
                int n = running.decrementAndGet();
                /*if (n == 0) {
                    logger.debug("quiescent");
                }*//* else {
                    logger.debug("queue {}", getQueue());
                }*/
            }
        };

        Runtime.getRuntime().addShutdownHook(new Thread(this::onShutDown));
    }

    protected void onShutDown() {
        pq.forEach(x -> logger.info("not executed: {}", x));
    }

    public void run(float pri, Runnable r) {
        run(new MyRunWithPriority(pri, r, DEFAULT_TIMEOUT_ms));
    }

    public void run(RunWithPriority r) {
        exe.execute(r);
    }

    @Override
    public void execute(@NotNull Runnable command) {
        run(DEFAULT_PRIORITY, command);
    }

    public Map summary() {
        Map x = new TreeMap();
        x.put("pending", pq.size());
        x.put("running", running.get());
        return x;
    }


    public interface RunWithPriority extends Runnable {
        float pri();
    }

    static final Comparator<RunWithPriority> runCompare = (x, y) -> {
        if (x == y) return 0;

        int c = Float.compare(y.pri(), x.pri());
        if (c == 0) {
            int h = Integer.compare(x.hashCode(), y.hashCode());
            if (h == 0) {
                return Integer.compare(System.identityHashCode(x),System.identityHashCode(y));
            }
            return h;
        }
        return c;
    };

    final Timer timer = new Timer(true);

    class MyRunWithPriority implements RunWithPriority {
        private final float pri;
        private final Runnable r;
        private final long timeoutMS;

        public MyRunWithPriority(float pri, Runnable r, long timeoutMS) {
            this.pri = pri;
            this.timeoutMS = timeoutMS;
            this.r = r;
        }

        @Override
        public float pri() {
            return pri;
        }

        @Override
        public void run() {

            TimeOutTask timeout = new TimeOutTask(Thread.currentThread());
            try {
                timer.schedule(timeout, timeoutMS);

                try {
                    r.run();
                } catch (ThreadDeath ie) {
                    logger.error("{} interrupted {}", r, ie.getMessage());
                }
            } finally {
                timeout.cancel();
            }
        }

        @Override
        public String toString() {
            return n2(100f * pri) + "%:" + r.toString();
        }
    }


    static class TimeOutTask extends TimerTask {
        final Thread t;

        TimeOutTask(Thread t) {
            this.t = t;
        }

        public void run() {
            if (t != null && t.isAlive()) {
                //t.stop();
                t.interrupt();
            }
        }
    }


//    public static void main(String args[]) {
//        new PoolTest();
//    }

//        public PoolTest()
//        {
//            try
//            {
//                PooledExecutor pe = new PooledExecutor(3);
//                pe.execute(new MyRunnable());
//                pe.shutdownAfterProcessingCurrentlyQueuedTasks();
//            }
//            catch (Exception e)
//            {
//                e.printStackTrace();
//            }
//        }

}
