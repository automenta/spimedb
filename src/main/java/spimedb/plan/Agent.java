package spimedb.plan;

import com.google.common.collect.Lists;
import org.infinispan.commons.util.concurrent.ConcurrentHashSet;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spimedb.graph.MapGraph;

import java.io.PrintStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;


public class Agent {

    final static Logger logger = LoggerFactory.getLogger(Agent.class);

    public final MapGraph<Goal,String> plan = new MapGraph(new ConcurrentHashMap(), ConcurrentHashSet::new);
    public final Map<String, GoalState> state = new ConcurrentHashMap();

    final ExecutorService exe;

    public Agent(ExecutorService exe) {
        this.exe = exe;
    }


    public <A extends Agent> GoalState goal(Goal<A> t) {
        return subgoal(new InvokedGoal<>(t));
    }

    <A extends Agent> GoalState subgoal(Goal<A> t) {

        String tid = t.id();
        return state.compute(tid, (tiid, prevState) -> {
            if (prevState == null) {
                plan.addVertex(t);
                GoalState s =  new GoalState(t);
                exe.execute(()->{
                    s.setState(State.Running);
                    long start = System.currentTimeMillis();
                    try {
                        t.DO((A) Agent.this, (next) -> {
                            for (Goal n : next) {

                                if (n == null) continue;

                                plan.addEdge(t, n, "=>");
                                subgoal(n);
                            }
                        });
                        s.setState(State.OK);
                    } catch (RuntimeException e) {
                        s.setState(State.Error);
                    }
                    long end = System.currentTimeMillis();
                    s.addTime( (end - start) );
                });
                return s;
            } else {
                switch (prevState.getState()) {
                    default:
                        break;
                }
                return prevState;
            }
        });

    }

    public void sync(long timeoutMS) {
        try {
            exe.awaitTermination(timeoutMS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            logger.error("awaitTermination: {}", e);
        }
    }

    public void printState(PrintStream out) {
        state.forEach( (t,s) -> {
            out.println(s + "\t" + s.time() + "ms");
            out.println("\t" + plan.vertex(s.goal, false));
        });
    }

    enum State {

        /** conditions not ready yet */
        Wait,

        /** can run at any time */
        Ready,

        /** executing */
        Running,

        /** ran successfully */
        OK,

        /** ran and errored, stalled */
        Error

    }

    public static class GoalState {

        public final Logger logger;
        public final Goal goal;

        long wallTime = 0;

        public GoalState(Goal goal) {
            this.goal = goal;
            this.logger = LoggerFactory.getLogger(goal.id());
        }

        private State state = State.Ready;

        @NotNull
        public State getState() {
            return state;
        }

        public void setState(@NotNull State state) {
            this.state = state;
            logger.info(state.toString());
        }

        @Override
        public String toString() {
            return goal.toString() + ": " + state.toString();
        }

        void addTime(long t) {
            wallTime += t;
        }

        /** the sum of wall time during which the goal executed, in milliseconds */
        public long time() {
            return wallTime;
        }
    }

    private static class InvokedGoal<A extends Agent> extends SynchronousGoal<A> {
        private final Goal<A> root;

        public InvokedGoal(Goal<A> root) {
            super(root.id(), System.currentTimeMillis());
            this.root = root;
        }

        @Override
        protected Iterable<Goal<? super A>> run(A context) throws RuntimeException {
            return Lists.newArrayList(root);
        }
    }
}
