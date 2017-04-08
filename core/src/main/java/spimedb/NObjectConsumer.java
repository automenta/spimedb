package spimedb;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.undertow.websockets.core.WebSocketChannel;
import spimedb.server.Session;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Created by me on 4/1/17.
 */
public interface NObjectConsumer extends Consumer<NObject> {

    public static NObjectConsumer Tagged(Consumer<NObject> each, String... tags) {
        return new OnTag.LambdaOnTag(tags, each);
    }
    public static NObjectConsumer HashPredicate(BiConsumer<String,String> each, String... tags) {
        return new OnHashPredicate.LambdaHashPredicate(each, tags);
    }



    abstract class OnTag implements NObjectConsumer {

        public final String[] any;

        public OnTag(String... any) {
            this.any = any;
        }

        public static class LambdaOnTag extends OnTag {

            final Consumer<NObject> target;

            LambdaOnTag(String[] tags, Consumer<NObject> target) {
                super(tags);
                this.target = target;
            }

            @Override
            public void accept(NObject x) {
                target.accept(x);
            }
        }
    }


    abstract class OnHashPredicate extends OnTag {

        public OnHashPredicate(String... any) {
            super(any);
        }

        @Override
        public void accept(NObject n) {
            String name = n.name();
            if (name.charAt(0) == '#') {
                //TODO parse better
                int hashTagEnds = name.indexOf('(');
                if (hashTagEnds == -1) return;
                String hash = name.substring(1, hashTagEnds);
                String predicate = name.substring(hashTagEnds+1, name.length()-1);
                onHashPredicate(hash, predicate);
            }
        }

        abstract protected void onHashPredicate(String hash, String predicate);

        static final class LambdaHashPredicate extends OnHashPredicate {
            private final BiConsumer<String, String> each;

            public LambdaHashPredicate(BiConsumer<String, String> each, String... tags) {
                super(tags);
                this.each = each;
            }

            @Override
            protected void onHashPredicate(String hash, String predicate) {
                each.accept(hash, predicate);
            }
        }
    }


}
