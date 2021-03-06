package actor.mining;

import cc.mallet.classify.Classification;
import cc.mallet.classify.Classifier;
import cc.mallet.pipe.CharSequence2TokenSequence;
import cc.mallet.pipe.FeatureSequence2FeatureVector;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.Target2Label;
import cc.mallet.pipe.TokenSequence2FeatureSequence;
import cc.mallet.pipe.TokenSequenceLowercase;
import cc.mallet.types.Instance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * Created by sopello on 1.06.2015.
 */
public class TextClassifier {

    private static Logger log = LoggerFactory.getLogger(TextClassifier.class);
    private Classifier malletClassifier = null;
    private static Pipe pipe = null;
    private String service;

    public TextClassifier(String productAndCategory, String service) {
        malletClassifier = loadClassifier(productAndCategory, service);
        getPipe();
        this.service = service;
    }

    public Classifier getClassifier() {
        return malletClassifier;
    }

    public boolean classify(Instance product, String link) {
        Classification result = malletClassifier.classify(product);
        if(result.getLabeling().getBestLabel().toString().equals("true")) {
            log.info("TextClassifier (" + service + "): preProcessingProduct " + link + " with classifier ok");
            return true;
        } else {
            log.info("TextClassifier (" + service + "): preProcessingProduct " + link + " with classifier failed");
            return false;
        }
    }

    private cc.mallet.classify.Classifier loadClassifier(String productAndCategory, String service) {
        Logger log = LoggerFactory.getLogger(TextClassifier.class);

        String classifierFileName = service + "classifier_"
                + productAndCategory.toLowerCase().replaceAll(" ", "_") +  ".priact";

        File serializedFile = new File("classifiers/" + classifierFileName);
        cc.mallet.classify.Classifier classifier;

        ObjectInputStream ois = null;

        try {
            ois = new ObjectInputStream(new FileInputStream(serializedFile));
            classifier = (cc.mallet.classify.Classifier) ois.readObject();
            ois.close();
            log.info("TextClassifier: succesfully loaded data of " + classifierFileName);
        } catch (IOException e) {
            log.error("TextClassifier: cannot load data of " + classifierFileName + ": " + e.getLocalizedMessage());
            return null;
        } catch (ClassNotFoundException e) {
            log.error("TextClassifier: cannot load data of " + classifierFileName + ": " + e.getLocalizedMessage());
            return null;
        }

        return classifier;
    }

    public Instance prepareInstance(String name, String url, String currentProductPrice,
                                  String categories, String description, String query,
                                    String desiredCategory) {
        if (malletClassifier != null) {
            StringBuilder sb = new StringBuilder();
//        sb.append(name.toLowerCase());
//        sb.append(";;;");
            sb.append(url);
            sb.append(";;;");
            sb.append(categories.toLowerCase());
            sb.append(";;;");
            boolean categoriesContainsDesired = categories.toLowerCase().contains(desiredCategory.toLowerCase());
            sb.append(categoriesContainsDesired);
            sb.append(";;;");
//        Integer queryCountInDescription = StringUtils.countMatches(description.toLowerCase(), query.toLowerCase());
//        sb.append(queryCountInDescription.toString());
//        sb.append(";;;");
//        Integer queryCountInTitle = StringUtils.countMatches(name.toLowerCase(), query.toLowerCase());
//        sb.append(queryCountInTitle.toString());
//        sb.append(";;;");
//            log.info(sb.toString());
            return getPipe().instanceFrom(new Instance(sb.toString(), "", name, ""));
        } else {
            return null;
        }

    }

    private Pipe buildPipe() {
        ArrayList pipeList = new ArrayList();

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

        return new SerialPipes(pipeList);
    }

    private Pipe getPipe() {
        if(pipe!=null) return pipe;
        else return buildPipe();
    }
}
