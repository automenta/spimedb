package spimedb.logic;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Charsets;
import com.google.common.escape.Escaper;
import com.google.common.escape.Escapers;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import jcog.bag.impl.hijack.DefaultHijackBag;
import jcog.net.UDPeer;
import jcog.pri.PriMerge;
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
import nars.premise.PreferSimpleAndPolarized;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.time.RealTime;
import nars.time.Tense;
import nars.util.JsonCompound;
import nars.util.exe.Executioner;
import nars.util.exe.MultiThreadExecutor;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;
import spimedb.NObject;
import spimedb.SpimeDB;
import spimedb.SpimeDBPeer;

import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;

/**
 * Created by me on 4/4/17.
 */
public class BaseAgent extends NAR {

    final static Escaper taskEscaper = Escapers.builder().addEscape('"', "\\\"").build();

    private final UDPeer UDPeer;


    public BaseAgent(SpimeDB db) throws SocketException, UnknownHostException {
        this(db, new MultiThreadExecutor(2, 64, true));
    }

    static final Compound recv = (Compound) $.seti($.the("RECV"));

    public BaseAgent(SpimeDB db, Executioner exe) throws SocketException, UnknownHostException {
        super(new RealTime.DSHalf(),
                new CaffeineIndex(new DefaultConceptBuilder(), 200000, false, exe),
                new XorShift128PlusRandom(1), exe);


        ConceptBagFocus focus = new ConceptBagFocus(this,
                new DefaultHijackBag<>(PriMerge.plus, 512, 2));

        setFocus(focus);

        FireConcepts fire = new FireConcepts.FireConceptsDirect(
                focus,
                DefaultDeriver.the,
                new PreferSimpleAndPolarized(),
                this);


        //load initial data
        try {
            IOUtils.readLines(BaseAgent.class.getClassLoader().getResource("sumo_merged.kif.nal").openStream(), Charsets.UTF_8).forEach(i -> {
                try {
                    List<Task> input = input(
                            "$0.01;0.9$ " +
                            i);
                    //input.forEach(System.out::println);
                    input.forEach(tt -> {
                        tt.term().recurseTerms(x -> {
                            if (x instanceof Atom) {
                                index(x.toString(), null);
                            }
                        });
                    });
                } catch (Throwable ignore) {
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        UDPeer = new SpimeDBPeer(7979, db) {

            @Override
            protected void receive(@Nullable UDProfile connected, @NotNull Msg m) {
                JsonElement j = new Gson().fromJson(new String(m.data()), JsonElement.class);

                //if (j.isJsonObject() && j.getAsJsonObject().get("I").getAsInt())

                input(j);
            }
        };


        new LeakOut(this, 16, 0.1f) {
            @Override
            protected float send(Task task) {
                UDPeer.believe("{ \">\": \"public\", N: \"" + taskEscaper.escape(task.toString()) + "\" }", 2);
                return 1f;
            }
        };

        IRC irc = new IRC("experiment2", "irc.freenode.net", "#netention") {


            @Override
            public void onGenericMessage(GenericMessageEvent event) throws Exception {
                super.onGenericMessage(event);

                JsonObject m = new JsonObject();
                m.addProperty(NObject.NAME, event.getMessage());
                input(m);

//                //MutableNObject m = new MutableNObject();
//                m.name(event.getMessage());
//                m.when(event.getTimestamp());

                if (event instanceof MessageEvent) {
                    //String channel = ((MessageEvent) event).getChannel().getName().substring(1) /* remove '#' */;
                    //m.withTags(channel, "public");

                }

                //m.withTags("public");

                //a.peer.say(m.toString(), 4);

                //System.out.println(new Gson().toJson(event));

                //a.peer.say
            }
        };

        new Thread(() -> {
            try {
                irc.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

    }

    protected void input(JsonElement j) {
        Term x = $.the(SpimeDB.uuidString());
        believe($.sim(x, JsonCompound.the(j)));
        believe($.inh(x, recv), Tense.Present);
    }

    final Cache<String, List<Term>> indexCache = Caffeine
            .newBuilder().maximumSize(16 * 1024).build();

    protected void index(String text, Term ref) {
        //k = DObject.parseKeywords(new LowerCaseTokenizer(), text);
        //indexCache.get()
    }

//    public static void main(String[] args) throws SocketException, UnknownHostException {
//        BaseAgent a = new BaseAgent();
//
//        a.log();
//
//        a.UDPeer.ping("a.narchy.xyz", 8080);
//        a.UDPeer.ping("localhost", 8080);
//
//        a.loop(1f).join();
//
//    }

}
