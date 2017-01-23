package spimedb.io;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import opennlp.tools.cmdline.parser.ParserTool;
import opennlp.tools.parser.Parse;
import opennlp.tools.parser.Parser;
import opennlp.tools.parser.ParserFactory;
import opennlp.tools.parser.ParserModel;
import org.jetbrains.annotations.Nullable;
import spimedb.util.HTTP;

import java.io.InputStream;

/**
 * http://opennlp.sourceforge.net/models-1.5/
 */
public class NLP {

    static final HTTP http = new HTTP();


    public static final LoadingCache<Class,Object> model = Caffeine.newBuilder().build(new CacheLoader<Class,Object>(){
        @Override
        public Object load(Class k) throws Exception {

            //http://opennlp.sourceforge.net/models-1.5/en-ner-date.bin
            //http://opennlp.sourceforge.net/models-1.5/en-ner-time.bin
            //http://opennlp.sourceforge.net/models-1.5/en-ner-location.bin
            //http://opennlp.sourceforge.net/models-1.5/en-ner-money.bin
            //http://opennlp.sourceforge.net/models-1.5/en-ner-organization.bin
            //http://opennlp.sourceforge.net/models-1.5/en-ner-person.bin
            //http://opennlp.sourceforge.net/models-1.5/en-ner-percentage.bin

            if (k == ParserModel.class) {
                final InputStream[] stream = new InputStream[1];
                http.asStream("http://opennlp.sourceforge.net/models-1.5/en-parser-chunking.bin", (modelIn) -> stream[0] = modelIn);
                ParserModel model = new ParserModel(stream[0]);
                stream[0].close();
                return model;
            }

            throw new RuntimeException("unsupported key: " + k);
        }
    });

    public static ParserModel parser() {
        return (ParserModel) model.get(ParserModel.class);
    }

    public static Parse parse(String input) {

        ParserModel model = parser();
        Parser parser = ParserFactory.create(model);
        Parse topParses[] = ParserTool.parseLine(input, parser, 1);
        return topParses[0];

    }

    public static void main(String[] args) {
        Parse parse = NLP.parse("what the fuck?");
        String res = toString(parse);
        System.out.println(res);


    }

    @Nullable
    public static String toString(@Nullable Parse parse) {
        if (parse == null)
            return null;

        StringBuffer sb = new StringBuffer();
        parse.show(sb);
        return sb.toString();
    }
}
