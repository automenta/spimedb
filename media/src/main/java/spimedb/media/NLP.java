package spimedb.media;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import opennlp.tools.cmdline.parser.ParserTool;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.parser.Parse;
import opennlp.tools.parser.Parser;
import opennlp.tools.parser.ParserFactory;
import opennlp.tools.parser.ParserModel;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.Span;
import org.jetbrains.annotations.Nullable;
import spimedb.util.HTTP;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * http://opennlp.sourceforge.net/models-1.5/
 */
public class NLP {

    static final HTTP http = new HTTP();


    public static final LoadingCache<Class,Object> models = Caffeine.newBuilder().build(new CacheLoader<Class,Object>(){
        @Override
        public Object load(Class k) throws Exception {

            //http://opennlp.sourceforge.net/models-1.5/en-ner-date.bin
            //http://opennlp.sourceforge.net/models-1.5/en-ner-time.bin
            //http://opennlp.sourceforge.net/models-1.5/en-ner-location.bin
            //http://opennlp.sourceforge.net/models-1.5/en-ner-money.bin
            //http://opennlp.sourceforge.net/models-1.5/en-ner-organization.bin
            //http://opennlp.sourceforge.net/models-1.5/en-ner-person.bin
            //http://opennlp.sourceforge.net/models-1.5/en-ner-percentage.bin

            String base = "http://opennlp.sourceforge.net/models-1.5/";

            if (k == TokenizerModel.class) {
                InputStream s = stream(base + "en-token.bin");
                TokenizerModel model = new TokenizerModel(s);
                s.close();
                return (model);
            }
            if (k == ParserModel.class) {
                InputStream s = stream(base + "en-parser-chunking.bin");
                ParserModel model = new ParserModel(s);
                s.close();
                return model;
            } else if (k == TokenNameFinderModel.class) {
                TokenNameFinderModel person, location;
                {
                    InputStream s = stream(base + "en-ner-person.bin");
                    person = new TokenNameFinderModel(s);
                    s.close();
                }
                {
                    InputStream s = stream(base + "en-ner-location.bin");
                    location = new TokenNameFinderModel(s);
                    s.close();
                }
                return new NameFinderME[] { new NameFinderME(person), new NameFinderME(location) };
            }

            throw new RuntimeException("unsupported key: " + k);
        }
    });


    public static TokenizerME tokenizer() {
        return new TokenizerME((TokenizerModel)models.get(TokenizerModel.class));
    }

    private static InputStream stream(String url) throws IOException {
        final InputStream[] stream = new InputStream[1];
        http.asStream(url, (modelIn) -> stream[0] = modelIn);
        return stream[0];
    }


    public static Parse parse(String input) {
        ParserModel model = (ParserModel) models.get(ParserModel.class);
        Parser parser = ParserFactory.create(model);
        Parse[] topParses = ParserTool.parseLine(input, parser, tokenizer(), 1);
        return topParses[0];

    }

    @Nullable public static Map<String,String[]> names(String input) {

        NameFinderME[] finders = (NameFinderME[]) models.get(TokenNameFinderModel.class);
        String[] tokens = tokenizer().tokenize(input);

        Map<String,String[]> x = new HashMap(finders.length);
        for (NameFinderME m : finders) {
            Span[] ss = m.find(tokens);
            if (ss.length > 0)
                x.put(ss[0].getType(), Span.spansToStrings(ss, tokens));
        }

        if (!x.isEmpty()) {
            return x;
        } else {
            return null;
        }
    }

    public static void main(String[] args) {
        String t = "John Smith said that to Jane Smith in New York.";
        Parse parse = NLP.parse(t);
        String res = toString(parse);
        System.out.println(res);

        System.out.println(names(t));


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
