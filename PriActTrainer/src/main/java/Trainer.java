import cc.mallet.classify.Classifier;
import cc.mallet.classify.ClassifierTrainer;
import cc.mallet.classify.NaiveBayesTrainer;
import cc.mallet.types.InstanceList;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Created by sopello on 02.06.2015.
 */
public class Trainer {

    public Classifier trainClassifier(InstanceList trainingInstances) {

        // Here we use a maximum entropy (ie polytomous logistic regression)
        //  classifier. Mallet includes a wide variety of classification
        //  algorithms, see the JavaDoc API for details.

        ClassifierTrainer trainer = new NaiveBayesTrainer();
        return trainer.train(trainingInstances);
    }

    public Classifier loadClassifier(File serializedFile)
            throws FileNotFoundException, IOException, ClassNotFoundException {

        // The standard way to save classifiers and Mallet data
        //  for repeated use is through Java serialization.
        // Here we load a serialized classifier from a file.

        Classifier classifier;

        ObjectInputStream ois =
                new ObjectInputStream (new FileInputStream(serializedFile));
        classifier = (Classifier) ois.readObject();
        ois.close();

        return classifier;
    }

    public void saveClassifier(Classifier classifier, File serializedFile)
            throws IOException {

        // The standard method for saving classifiers in
        //  Mallet is through Java serialization. Here we
        //  write the classifier object to the specified file.

        ObjectOutputStream oos =
                new ObjectOutputStream(new FileOutputStream(serializedFile));
        oos.writeObject (classifier);
        oos.close();
    }
}
