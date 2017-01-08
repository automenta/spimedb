package spimedb;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.graph.ElementOrder;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import net.bytebuddy.ByteBuddy;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by me on 1/8/17.
 */
public class Schema {


    @JsonIgnore
    public final MutableGraph<String> inh = GraphBuilder.directed().allowsSelfLoops(false).expectedNodeCount(512).nodeOrder(ElementOrder.unordered()).build();

//    @JsonIgnore
//    public final Map<String,Tag> tag = new ConcurrentHashMap<>();

    @JsonIgnore
    public final Map<String, Class> tagClasses = new ConcurrentHashMap<String, Class>();
    @JsonIgnore
    public final ClassLoader cl = ClassLoader.getSystemClassLoader();
    @JsonIgnore
    public final ByteBuddy tagProxyBuilder = new ByteBuddy();

    public Schema() {
    }

//    public Tag get(String id, boolean createIfUnknown) {
//        if (createIfUnknown) {
//            return tag.computeIfAbsent(id, Tag::new);
//        } else {
//            return tag.get(id);
//        }
//    }

    public void tag(String id, String[] parents) {
        for (String parentTag : parents) {
            inh.putEdge(parentTag, id);
        }
    }

    /**
     * computes the set of subtree (children) tags held by the extension of the input (parent) tags
     *
     * @param parentTags if empty, searches all tags; otherwise searches the specified tags and all their subtags
     */
    public Set<String> tagsAndSubtags(@Nullable String... parentTags) {

        if (parentTags == null || parentTags.length == 0) {
            return inh.nodes();
        }

        Set<String> s = new HashSet<>();
        for (String x : parentTags) {
            if (s.add(x)) {
                s.addAll(inh.successors(x));
            }
        }
        return s;
    }

    public Class[] resolve(String... tags) {
        Class[] c = new Class[tags.length];
        int i = 0;
        for (String s : tags) {
            Class x = tagClasses.get(s);
            if (x == null)
                throw new NullPointerException("missing class: " + s);
            c[i++] = x;
        }
        return c;
    }

    public Class the(String tagID, String... supertags) {

        synchronized (tagClasses) {
            if (tagClasses.containsKey(tagID))
                throw new RuntimeException(tagID + " class already defined");

            Class[] s = resolve(supertags);
            Class proxy = tagProxyBuilder.makeInterface(s).name("_" + tagID).make().load(cl).getLoaded();
            tagClasses.put(tagID, proxy);
            return proxy;
        }

        //.subclass(NObject.class).implement(s)
                /*.method(any())
                .intercept(MethodDelegation.to(MyInterceptor.class)
                        .andThen(SuperMethodCall.INSTANCE)
                        .defineField("myCustomField", Object.class, Visibility.PUBLIC)*/
                /*.make()
                .load(cl)
                .getLoaded();*/
    }

}
