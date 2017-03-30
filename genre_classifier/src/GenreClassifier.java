import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import javax.sound.sampled.UnsupportedAudioFileException;

import weka.classifiers.Classifier;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.StopAudioProcessor;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;

public class GenreClassifier {
	private static final ArrayList<String> songs;
	private static final String rootPath = "/home/james/workspace/WekaModelTrainer/";
	static
	{
		songs = new ArrayList<String>();
		
		String musicFiles = rootPath + "test_genres";
		int maxFiles = 26;
		int start = 24;
		
		for (int i = start; i < maxFiles; i++) {
			String f = i < 10 ? "/blues/blues.0000" + i + ".au" : "/blues/blues.000" + i + ".au";
			songs.add(musicFiles + f);
		}

		for (int i = start; i < maxFiles; i++) {
			String f = i < 10 ? "/classical/classical.0000" + i + ".au" : "/classical/classical.000" + i + ".au";
			songs.add(musicFiles + f);
		}
		
		for (int i = start; i < maxFiles; i++) {
			String f = i < 10 ? "/country/country.0000" + i + ".au" : "/country/country.000" + i + ".au";
			songs.add(musicFiles + f);
		}

		for (int i = start; i < maxFiles; i++) {
			String f = i < 10 ? "/disco/disco.0000" + i + ".au" : "/disco/disco.000" + i + ".au";
			songs.add(musicFiles + f);
		}

		for (int i = start; i < maxFiles; i++) {
			String f = i < 10 ? "/hiphop/hiphop.0000" + i + ".au" : "/hiphop/hiphop.000" + i + ".au";
			songs.add(musicFiles + f);
		}
	}

	private static final String arffPath = "test_genres.arff";
	private static Instances testData;
	private static boolean doneFeaturizing = false;
	
	private static synchronized void addInstance(Instance instance) {
		testData.add(instance);
	}

	public static void main(String[] args) {
		// Featurize the audio
		System.out.println("Featurizing training data...");
		final AudioFeaturizer audioFeaturizer = new AudioFeaturizer();
		testData = new Instances(audioFeaturizer.getDataset());
		
		for (final String song : songs) {
			final ArrayList<Instance> songInstances = new ArrayList<>();
			
			try {
				doneFeaturizing = false;
				AudioDispatcher dispatcher = AudioDispatcherFactory.fromFile(new File(song), 1024, 512);
				dispatcher.addAudioProcessor(new StopAudioProcessor(5));
				dispatcher.addAudioProcessor(new AudioProcessor(){
					@Override
					public boolean process(AudioEvent audioEvent) {
						songInstances.add(audioFeaturizer.featurize(audioEvent, null));
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
			}
			
			// add the instance
			addInstance(featureVals);
		}
		System.out.println("Done Featurizing files. Labelling and writing arff to disk...");
		
		Classifier c = null;
		try {
			c = (Classifier) weka.core.SerializationHelper.read(rootPath + "trained.model");
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		
		// label instances
		double clsLabel = 0.0;
		 for (int i = 0; i < testData.numInstances(); i++) {
			try {
				clsLabel = c.classifyInstance(testData.instance(i));
				System.out.println("Song: " + songs.get(i) + " classified as " + AudioFeatures.ClassFeature.value((int)clsLabel));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		   testData.instance(i).setClassValue(clsLabel);
		 }
		 
		// write the dataset with labels to disk as an arff
		try {
			BufferedWriter b = new BufferedWriter(new FileWriter(arffPath));
			b.write(testData.toString());
			b.newLine();
			b.flush();
			b.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println("Done!");
	}
}