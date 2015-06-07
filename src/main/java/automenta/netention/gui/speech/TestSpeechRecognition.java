//package jnetention.speech;
//
//import edu.cmu.sphinx.api.Configuration;
//import edu.cmu.sphinx.api.LiveSpeechRecognizer;
//import edu.cmu.sphinx.api.SpeechResult;
//import edu.cmu.sphinx.result.WordResult;
//
//import java.io.IOException;
//
///**
// * http://cmusphinx.sourceforge.net/wiki/tutorialsphinx4
// */
//public class TestSpeechRecognition {
//
//    public static void main(String[] args) throws IOException {
//        Configuration configuration = new Configuration();
//
//// Set path to acoustic model.
//        configuration.setAcousticModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us");
//// Set path to dictionary.
//        configuration.setDictionaryPath("resource:/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict");
//// Set language model.
//        configuration.setLanguageModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us.lm.dmp");
//
//        LiveSpeechRecognizer recognizer = new LiveSpeechRecognizer(configuration);
//// Start recognition process pruning previously cached data.
//        recognizer.startRecognition(true);
//        //SpeechResult result = recognizer.getResult();
//// Pause recognition process. It can be resumed then with startRecognition(false).
//        //recognizer.stopRecognition();
//
//        SpeechResult result;
//        while ((result = recognizer.getResult()) != null) {
//
//            // Print utterance string without filler words.
//            System.out.println(result.getHypothesis());
//
//// Get individual words and their times.
//            for (WordResult r : result.getWords()) {
//                System.out.print("  " + r);
//            }
//            System.out.println();
//
//            //result.getLattice().dumpAllPaths();
//
//            // Save lattice in a graphviz format.
//            //result.getLattice().ddumpDot("lattice.dot", "lattice");
//        }
//    }
//
//}
