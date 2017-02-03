package spimedb.util;

import java.util.Comparator;
import java.util.concurrent.Executor;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by me on 2/3/17.
 */
public class PrioritizedExecutor  {

    public final PriorityBlockingQueue<Runnable> pq = new PriorityBlockingQueue<Runnable>(128, new ComparePriority());

    final Executor exe;

    public PrioritizedExecutor(int threads) {
         this.exe = new ThreadPoolExecutor(threads, threads, 10, TimeUnit.SECONDS, pq);
    }

    public void run(float pri, Runnable r) {
        run(new MyRunWithPriority(pri, r));
    }

    public void run(RunWithPriority r) {
        exe.execute(r);
    }

    public interface RunWithPriority extends Runnable {
        public float pri();
    }

    private static class ComparePriority<T extends RunWithPriority> implements Comparator<T> {

        @Override
        public int compare(T x, T y) {
            if (x == y) return 0;

            int c = Float.compare(y.pri(), x.pri());
            if (c == 0) {
                return Integer.compare(x.hashCode(), y.hashCode());
            }
            return c;
        }
    }

    private static class MyRunWithPriority implements RunWithPriority {
        private final float pri;
        private final Runnable r;

        public MyRunWithPriority(float pri, Runnable r) {
            this.pri = pri;
            this.r = r;
        }

        @Override
        public float pri() {
            return pri;
        }

        @Override
        public void run() {
            r.run();
        }
    }
}
