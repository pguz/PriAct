import cc.mallet.classify.Classification;
import cc.mallet.classify.Classifier;
import cc.mallet.classify.Trial;
import cc.mallet.pipe.CharSequence2TokenSequence;
import cc.mallet.pipe.FeatureSequence2FeatureVector;
import cc.mallet.pipe.Input2CharSequence;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.PrintInputAndTarget;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.Target2Label;
import cc.mallet.pipe.TokenSequence2FeatureSequence;
import cc.mallet.pipe.TokenSequenceLowercase;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import com.google.common.base.Joiner;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Created by sopello on 01.06.2015.
 */
public class Main {

    private final static Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String [] args) {
        log.info("Main started");
        AllegroAnalyzer allegroAnalyzer = new AllegroAnalyzer();
        String productAndCategory = "iphone 6 128?iphone";
        log.info("Searching " + productAndCategory);
        Set<Pair<Product, Boolean>> productSet = null;
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            productSet = allegroAnalyzer.getProducts(productAndCategory);
            if (productSet.size()<1) {
                log.error("Something went wrong, no products were found! Retry? Exit - enter exit");
                String input = null;
                try {
                    input = br.readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (!input.toLowerCase().contains("exit")) {
                    continue;
                } else {
                    log.error("Exiting classifier learning");
                    return;
                }
            } else {
                break;
            }
        }

        Integer falseCount = 0;
        Integer trueCount = 0;
        Pipe pipe = buildPipe();
        InstanceList trainingInstanceList = new InstanceList(pipe);

        for (Pair<Product,Boolean> pair : productSet) {
            if (pair.getRight().equals(true)) {
                trueCount = trueCount + 1;
            } else if (pair.getRight().equals(false)) {
                falseCount = falseCount + 1;
            }
            Instance currentInstance = new Instance(pair.getLeft().toString(), pair.getRight(), pair.getLeft().getName(), "");
            log.info(pair.getLeft().toString());
            trainingInstanceList.addThruPipe(currentInstance);
        }
        log.info(Joiner.on(", ").join("true: ", trueCount.toString(), ", false:", falseCount.toString()));
        log.info("Trying to perform classifier learning, instance list size: " + trainingInstanceList.size());
        Trainer trainer = new Trainer();
        Classifier allegroNaiveBayes = trainer.trainClassifier(trainingInstanceList);
        log.info("Learned classifier, trying to save it");
        try {
            trainer.saveClassifier(allegroNaiveBayes, new File("allegroclassifier_"
                    + productAndCategory.toLowerCase().replaceAll(" ", "_") +  ".priact"));
        } catch (IOException e) {
            log.error(e.getLocalizedMessage());
            e.printStackTrace();
        }
        log.info("Finished, testing what learned..");
        Trial trial = new Trial(allegroNaiveBayes, trainingInstanceList);
        log.info("Accuracy for the same data as learned: " + trial.getAccuracy());
        Instance testInstance = trainingInstanceList.get(0);
        Classification result = allegroNaiveBayes.classify(testInstance);
        log.info("potential class of " + result.getLabeling().getBestLabel().toString());
        log.info("PriActTrainer has finished working");
    }

    public static Pipe buildPipe() {
        ArrayList pipeList = new ArrayList();

        // Read data from File objects
        pipeList.add(new Input2CharSequence("UTF-8"));

        // Regular expression for what constitutes a token.
        //  This pattern includes Unicode letters, Unicode numbers,
        //   and the underscore character. Alternatives:
        //    "\\S+"   (anything not whitespace)
        //    "\\w+"    ( A-Z, a-z, 0-9, _ )
        //    "[\\p{L}\\p{N}_]+|[\\p{P}]+"   (a group of only letters and numbers OR
        //                                    a group of only punctuation marks)
        Pattern tokenPattern =
                Pattern.compile("[^;;;]+[^;;;]");

        // Tokenize raw strings
        pipeList.add(new CharSequence2TokenSequence(tokenPattern));

        // Normalize all tokens to all lowercase
        pipeList.add(new TokenSequenceLowercase());

        // Rather than storing tokens as strings, convert
        //  them to integers by looking them up in an alphabet.
        pipeList.add(new TokenSequence2FeatureSequence());

        // Do the same thing for the "target" field:
        //  convert a class label string to a Label object,
        //  which has an index in a Label alphabet.
        pipeList.add(new Target2Label());

        // Now convert the sequence of features to a sparse vector,
        //  mapping feature IDs to counts.
        pipeList.add(new FeatureSequence2FeatureVector());

        // Print out the features and the label
        pipeList.add(new PrintInputAndTarget());

        return new SerialPipes(pipeList);
    }
}
