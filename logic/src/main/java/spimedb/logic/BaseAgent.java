package spimedb.logic;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Charsets;
import com.google.common.escape.Escaper;
import com.google.common.escape.Escapers;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import jcog.bag.impl.PLinkHijackBag;
import jcog.random.XorShift128PlusRandom;
import nars.$;
import nars.NAR;
import nars.Task;
import nars.bag.leak.LeakOut;
import nars.conceptualize.DefaultConceptBuilder;
import nars.control.ConceptBagFocus;
import nars.control.FireConcepts;
import nars.derive.DefaultDeriver;
import nars.index.term.map.CaffeineIndex;
import nars.premise.MatrixPremiseBuilder;
import nars.premise.PreferSimpleAndConfident;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.time.RealTime;
import nars.time.Tense;
import nars.util.JsonCompound;
import nars.util.exe.Executioner;
import nars.util.exe.MultiThreadExecutor;
import org.apache.commons.io.IOUtils;
import org.apache.lucene.analysis.core.LowerCaseTokenizer;
import org.eclipse.collections.impl.factory.primitive.CharSets;
import spimedb.Peer;
import spimedb.SpimeDB;
import spimedb.index.DObject;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;
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

        //load initial data
        try {
            IOUtils.readLines(BaseAgent.class.getClassLoader().getResource("sumo_merged.kif.nal").openStream(), Charsets.UTF_8).forEach(i -> {
                try {
                    List<Task> input = input(
                            //"$0.0;0.9$ " +
                                     i);
                    input.forEach(System.out::println);
                    input.forEach(tt -> {
                        tt.term().recurseTerms(x -> {
                            if (x instanceof Atom) {
                                index(x.toString(), null);
                            }
                        });
                    });
                } catch (Throwable ignore) { }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        peer = new Peer(7979) {

            @Override
            protected void receive(Msg m) {
                JsonElement j = new Gson().fromJson(new String(m.data()), JsonElement.class);

                //if (j.isJsonObject() && j.getAsJsonObject().get("I").getAsInt())

                Atomic x = $.the(SpimeDB.uuidString());
                believe($.sim(x, JsonCompound.the(j)));
                believe($.inh(x, recv), Tense.Present);
            }
        };


        new LeakOut(this, 16, 0.1f) {
            @Override protected float send(Task task) {
                peer.say("{ \">\": \"public\", N: \"" + taskEscaper.escape(task.toString()) + "\" }", 2);
                return 1f;
            }
        };
    }

    final Cache<String,List<Term>> indexCache = Caffeine
            .newBuilder().maximumSize(16*1024).build();

    protected void index(String text, Term ref) {
        //k = DObject.parseKeywords(new LowerCaseTokenizer(), text);
        //indexCache.get()
    }

    public static void main(String[] args) throws SocketException, UnknownHostException {
        BaseAgent a = new BaseAgent();

        a.log();

        a.peer.ping("a.narchy.xyz", 8080);
        a.peer.ping("localhost", 8080);


        a.loop(10f).join();
    }

}
