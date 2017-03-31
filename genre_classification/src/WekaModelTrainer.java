import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.sound.sampled.UnsupportedAudioFileException;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.StopAudioProcessor;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.mfcc.MFCC;

public class WekaModelTrainer {
	private static final Map<String, String> songClasses;

	static
	{
		songClasses = new HashMap<String, String>();
		String rootPath = "/home/james/workspace/WekaModelTrainer/";
		String musicFiles = rootPath + "genres";
		int maxFiles = 24;
		
		for (int i = 0; i < maxFiles; i++) {
			String f = i < 10 ? "/blues/blues.0000" + i + ".au" : "/blues/blues.000" + i + ".au";
			songClasses.put(musicFiles + f, AudioFeatures.BLUES);
		}

		for (int i = 0; i < maxFiles; i++) {
			String f = i < 10 ? "/classical/classical.0000" + i + ".au" : "/classical/classical.000" + i + ".au";
			songClasses.put(musicFiles + f, AudioFeatures.CLASSICAL);
		}
		
		for (int i = 0; i < maxFiles; i++) {
			String f = i < 10 ? "/country/country.0000" + i + ".au" : "/country/country.000" + i + ".au";
			songClasses.put(musicFiles + f, AudioFeatures.COUNTRY);
		}

		for (int i = 0; i < maxFiles; i++) {
			String f = i < 10 ? "/disco/disco.0000" + i + ".au" : "/disco/disco.000" + i + ".au";
			songClasses.put(musicFiles + f, AudioFeatures.DISCO);
		}

		for (int i = 0; i < maxFiles; i++) {
			String f = i < 10 ? "/hiphop/hiphop.0000" + i + ".au" : "/hiphop/hiphop.000" + i + ".au";
			songClasses.put(musicFiles + f, AudioFeatures.HIPHOP);
		}

		for (int i = 0; i < maxFiles; i++) {
			String f = i < 10 ? "/jazz/jazz.0000" + i + ".au" : "/jazz/jazz.000" + i + ".au";
			songClasses.put(musicFiles + f, AudioFeatures.JAZZ);
		}
	}

	private static final String arffPath = "genres.arff";
	private static final String modelPath = "trained.model";
	private static Instances trainingData;
	private static boolean doneFeaturizing = false;
	
	private static synchronized void addInstance(Instance instance) {
		trainingData.add(instance);
	}

	public static void main(String[] args) {
		// Featurize the audio
		System.out.println("Featurizing training data...");
		final AudioFeaturizer audioFeaturizer = new AudioFeaturizer();
		trainingData = new Instances(audioFeaturizer.getDataset());
		
		for (final Map.Entry<String, String> entry : songClasses.entrySet()) {
			
			final String wavFile = entry.getKey();
			final String wavClass = entry.getValue();
			final ArrayList<Instance> songInstances = new ArrayList<>();
			
			try {
				doneFeaturizing = false;
				AudioDispatcher dispatcher = AudioDispatcherFactory.fromFile(new File(wavFile), 1024, 512);
				
				dispatcher.addAudioProcessor(new AudioProcessor(){
					@Override
					public boolean process(AudioEvent audioEvent) {
						songInstances.add(audioFeaturizer.featurize(audioEvent, wavClass));
						return true;	
					}	

					@Override
					public void processingFinished() {
						doneFeaturizing = true;
					}
				});
				
				dispatcher.run();
				
			} catch (UnsupportedAudioFileException e2) {
				e2.printStackTrace();
			} catch (IOException e2) {
				e2.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		
			// featurize files serially
			while (!doneFeaturizing);
			
			// done featurizing single song instances
			Instance featureVals = new DenseInstance(AudioFeatures.Features.length+1);
			Instances data = new Instances(audioFeaturizer.getDataset());
			featureVals.setDataset(data);
			
			// take the average
			for (int i = 0; i < AudioFeatures.Features.length; i++) {
				double avgC = 0.0;
				for (Instance ins : songInstances) {
					avgC += ins.value(i)/songInstances.size();
				}
				featureVals.setValue(i, avgC);
				featureVals.setValue(AudioFeatures.Features.length, wavClass);
			}
			
			// add the instance
			addInstance(featureVals);
		}

		// write the dataset to disk as an arff
		System.out.println("Done Featurizing files. Writing arff to disk...");
		try {
			BufferedWriter b = new BufferedWriter(new FileWriter(arffPath));
			b.write(trainingData.toString());
			b.newLine();
			b.flush();
			b.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Done!");
		
		// train and evaluate the model, with 10-fold stratified cross-validation
		System.out.println("Training the model...");
		Classifier model = (Classifier) new weka.classifiers.functions.SMO();
		String options[] = {"-C", "1.0", "-L", "0.001", "-P", "1.0E-12", "-N", "0", "-V", "-1", "-W", "1", "-K", "weka.classifiers.functions.supportVector.PolyKernel"};
		try {
			((weka.classifiers.functions.SMO) model).setOptions(options);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		try {
		//	data.stratify(10);
			model.buildClassifier(trainingData);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Couldn't train the model!");
		}
		System.out.println("Done!");
		
		// test the model
		System.out.println("Evaluating the model with 10-fold cross-validation...");
		try {
			Evaluation modelEval = new Evaluation(trainingData);
			modelEval.crossValidateModel(model, trainingData, 10, new Random());
			//modelEval.evaluateModel(model, data);
			System.out.println(modelEval.toSummaryString(true));
			System.out.println(modelEval.toClassDetailsString());
			System.out.println(modelEval.toMatrixString());
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Couldn't load data for evaluation!");
		}
		System.out.println("Done!");
		
		// save the model
		System.out.println("Serializing the model to disk...");
		ObjectOutputStream oos;
		try {
			oos = new ObjectOutputStream(new FileOutputStream(modelPath));
			oos.writeObject(model);
			oos.flush();
			oos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new RuntimeException("Error serializing model!");
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Error serializing model!");
		}
		System.out.println("Done!");
	}
}