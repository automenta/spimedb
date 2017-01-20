package spimedb.plan;

import com.google.common.collect.Lists;
import org.junit.Test;

import java.util.concurrent.ForkJoinPool;

import static org.junit.Assert.assertEquals;

/**
 * Created by me on 1/20/17.
 */
public class AgentTest {

    static class TestAgent extends Agent {

        public final StringBuilder log = new StringBuilder();

        public TestAgent() {
            super(ForkJoinPool.commonPool());
        }
    }

    static class SuperGoal extends SynchronousGoal<TestAgent> {

        public SuperGoal(String id) {
            super(id);
        }

        @Override
        protected Iterable<Goal<? super TestAgent>> run(TestAgent context) throws RuntimeException {
            context.log.append(id()).append(' ');
            return Lists.newArrayList(  new SubGoal("x"), new SubGoal("y") );
        }
    }

    static class SubGoal extends AtomicGoal<TestAgent> {

        SubGoal(String id) {
            super(id);
        }

        @Override
        protected void run(TestAgent context) {
            context.log.append(id()).append(' ');
        }

    }

    @Test
    public void test1() {
        TestAgent a = new TestAgent();
        a.goal(new SuperGoal("1"));
        a.sync(1000 * 10);
        System.out.println(a.log.toString());
        assertEquals("SuperGoal.1 SubGoal.x SubGoal.y ", a.log.toString());
    }
}