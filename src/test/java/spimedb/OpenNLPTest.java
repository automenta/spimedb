package spimedb;

import opennlp.tools.cmdline.parser.ParserTool;
import opennlp.tools.parser.Parse;
import opennlp.tools.parser.Parser;
import opennlp.tools.parser.ParserFactory;
import opennlp.tools.parser.ParserModel;
import org.junit.Ignore;
import org.junit.Test;
import spimedb.util.HTTP;

import java.io.IOException;

/**
 * Created by me on 1/20/17.
 */
@Ignore //because downloading the files could consume time during a test run
public class OpenNLPTest {

    @Test
    public void testParse() throws IOException {

        HTTP http = new HTTP();

        /** http://opennlp.sourceforge.net/models-1.5/ */
        //http://opennlp.sourceforge.net/models-1.5/en-ner-date.bin
        //http://opennlp.sourceforge.net/models-1.5/en-ner-time.bin
        //http://opennlp.sourceforge.net/models-1.5/en-ner-location.bin
        //http://opennlp.sourceforge.net/models-1.5/en-ner-money.bin
        //http://opennlp.sourceforge.net/models-1.5/en-ner-organization.bin
        //http://opennlp.sourceforge.net/models-1.5/en-ner-person.bin
        //http://opennlp.sourceforge.net/models-1.5/en-ner-percentage.bin
        http.asStream("http://opennlp.sourceforge.net/models-1.5/en-parser-chunking.bin", (modelIn) -> {

            try {
                ParserModel model = new ParserModel(modelIn);
                Parser parser = ParserFactory.create(model);
                String sentence =
                        "The UFO landed in another dimension.";
                        //"The quick brown fox jumps over the lazy dog .";
                Parse topParses[] = ParserTool.parseLine(sentence, parser, 1);
                for (Parse p : topParses) {
                    p.show();
                    //p.showCodeTree();
                    //System.out.println(p);
                    //System.out.println("\t" + p.getDerivation());
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            finally {
                if (modelIn != null) {
                    try {
                        modelIn.close();
                    }
                    catch (IOException e) {
                    }
                }
            }

        });



    }
}
