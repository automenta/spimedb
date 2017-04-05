package spimedb.logic;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.escape.Escaper;
import com.google.common.escape.Escapers;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import jcog.bag.impl.PLinkHijackBag;
import jcog.net.UDPeer;
import jcog.random.XorShift128PlusRandom;
import nars.$;
import nars.NAR;
import nars.Task;
import nars.bag.impl.ArrayBag;
import nars.bag.impl.TaskHijackBag;
import nars.bag.leak.Leak;
import nars.bag.leak.LeakOut;
import nars.budget.BudgetMerge;
import nars.conceptualize.DefaultConceptBuilder;
import nars.control.ConceptBagFocus;
import nars.control.FireConcepts;
import nars.derive.DefaultDeriver;
import nars.derive.TrieDeriver;
import nars.index.term.TermIndex;
import nars.index.term.map.CaffeineIndex;
import nars.premise.MatrixPremiseBuilder;
import nars.premise.PreferSimpleAndConfident;
import nars.term.Compound;
import nars.term.atom.Atomic;
import nars.time.RealTime;
import nars.time.Tense;
import nars.time.Time;
import nars.util.JsonCompound;
import nars.util.exe.Executioner;
import nars.util.exe.MultiThreadExecutor;
import nars.util.exe.SynchronousExecutor;
import org.jetbrains.annotations.NotNull;
import spimedb.Peer;


import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.UUID;

/**
 * Created by me on 4/4/17.
 */
public class BaseAgent extends NAR {

    final static Escaper taskEscaper = Escapers.builder().addEscape('"', "\\\"").build();

    private final Peer peer;


    public BaseAgent() throws SocketException, UnknownHostException {
        this(new MultiThreadExecutor(2, 64, true));
    }

    static final Compound recv = (Compound)$.seti($.the("RECV"));

    public BaseAgent(Executioner exe) throws SocketException, UnknownHostException {
        super(new RealTime.DSHalf(),
                new CaffeineIndex(new DefaultConceptBuilder(), 200000, false, exe),
                new XorShift128PlusRandom(1), exe);


        ConceptBagFocus focus = new ConceptBagFocus(this,
                new PLinkHijackBag(512, 2, random));

        setFocus(focus);

        FireConcepts.FireConceptsDirect fire = new FireConcepts.FireConceptsDirect(
                focus,
                new MatrixPremiseBuilder(DefaultDeriver.the, new PreferSimpleAndConfident()),
                this);
        fire.activationRate.setValue(1f);
        fire.conceptsFiredPerCycle.set(8);


        peer = new Peer(7979) {

            @Override
            protected void receive(Msg m) {
                JsonElement j = new Gson().fromJson(new String(m.data()), JsonElement.class);

                //if (j.isJsonObject() && j.getAsJsonObject().get("I").getAsInt())

                Atomic x = $.the(UUID.randomUUID().toString());
                believe($.sim(x, JsonCompound.the(j)));
                believe($.inh(x, recv), Tense.Present);
            }
        };


        new LeakOut(this, 16, 0.1f) {
            @Override protected float send(Task task) {
                peer.say("{ \">\": \"\", N: \"" + taskEscaper.escape(task.toString()) + "\" }", 2);
                return 1f;
            }
        };
    }

    public static void main(String[] args) throws SocketException, UnknownHostException {
        BaseAgent a = new BaseAgent();

        a.log();

        a.peer.ping("a.narchy.xyz", 8080);

        a.loop(10f).join();
    }

}
