package spimedb.plan;

import org.infinispan.commons.util.concurrent.ConcurrentHashSet;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spimedb.graph.MapGraph;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;


public class Agent {

    final static Logger logger = LoggerFactory.getLogger(Agent.class);

    final MapGraph<Goal,String> plan = new MapGraph(new ConcurrentHashMap(), ConcurrentHashSet::new);
    final Map<String, GoalState> state = new ConcurrentHashMap();

    final ExecutorService exe;

    public Agent(ExecutorService exe) {
        this.exe = exe;
    }

    public <A extends Agent> GoalState goal(Goal<A> t) {
        String tid = t.id();
        return state.compute(tid, (tiid, prevState) -> {
            if (prevState == null) {
                plan.addVertex(t);
                GoalState s =  new GoalState(t);
                Logger tlog = s.logger;
                tlog.info("plan");
                exe.execute(()->{
                    try {
                        s.setState(State.Running);
                        t.DO((A) Agent.this, (next) -> {
                            s.setState(State.OK);
                            for (Goal n : next) {
                                plan.addEdge(t, n, "=>");
                                goal(n);
                            }
                        });
                    } catch (RuntimeException e) {
                        tlog.error("error {}", new Date(), e);
                        s.setState(State.Error);
                    }
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

    static class GoalState {

        public final Logger logger;

        public GoalState(Goal goal) {
            logger = LoggerFactory.getLogger(goal.id());
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

    }

}
