import java.util.ArrayList;
import java.util.HashMap;

import weka.core.Attribute;

public class AudioFeatures {
	public static String BLUES = "0";
	public static String CLASSICAL = "1";
	public static String COUNTRY = "2";
	public static String DISCO = "3";
	public static String HIPHOP = "4";
	public static String JAZZ = "5";
	public static enum Feature {
			MFCC0,
			MFCC1,
			MFCC2,
			MFCC3,
			MFCC4,
			MFCC5,
			MFCC6,
			MFCC7,
			MFCC8,
			MFCC9,
			MFCC10,
			MFCC11,
			MFCC12,
			MFCC13,
			MFCC14,
			MFCC15,
			MFCC16,
			MFCC17,
			MFCC18,
			MFCC19,
			MFCC20,
			MFCC21,
			MFCC22,
			MFCC23,
			MFCC24,
			MFCC25,
			MFCC26,
			MFCC27,
			MFCC28,
			MFCC29,
	};
	public static final HashMap<Feature,String> FeatureNames;
	static
	{
		FeatureNames = new HashMap<Feature, String>();
		FeatureNames.put(Feature.MFCC0, "MFCC0");
		FeatureNames.put(Feature.MFCC1, "MFCC1");
		FeatureNames.put(Feature.MFCC2, "MFCC2");
		FeatureNames.put(Feature.MFCC3, "MFCC3");
		FeatureNames.put(Feature.MFCC4, "MFCC4");
		FeatureNames.put(Feature.MFCC5, "MFCC5");
		FeatureNames.put(Feature.MFCC6, "MFCC6");
		FeatureNames.put(Feature.MFCC7, "MFCC7");
		FeatureNames.put(Feature.MFCC8, "MFCC8");
		FeatureNames.put(Feature.MFCC9, "MFCC9");
		FeatureNames.put(Feature.MFCC10, "MFCC10");
		FeatureNames.put(Feature.MFCC11, "MFCC11");
		FeatureNames.put(Feature.MFCC12, "MFCC12");
		FeatureNames.put(Feature.MFCC13, "MFCC13");
		FeatureNames.put(Feature.MFCC14, "MFCC14");
		FeatureNames.put(Feature.MFCC15, "MFCC15");
		FeatureNames.put(Feature.MFCC16, "MFCC16");
		FeatureNames.put(Feature.MFCC17, "MFCC17");
		FeatureNames.put(Feature.MFCC18, "MFCC18");
		FeatureNames.put(Feature.MFCC19, "MFCC19");
		FeatureNames.put(Feature.MFCC20, "MFCC20");
		FeatureNames.put(Feature.MFCC21, "MFCC21");
		FeatureNames.put(Feature.MFCC22, "MFCC22");
		FeatureNames.put(Feature.MFCC23, "MFCC23");
		FeatureNames.put(Feature.MFCC24, "MFCC24");
		FeatureNames.put(Feature.MFCC25, "MFCC25");
		FeatureNames.put(Feature.MFCC26, "MFCC26");
		FeatureNames.put(Feature.MFCC27, "MFCC27");
		FeatureNames.put(Feature.MFCC28, "MFCC28");
		FeatureNames.put(Feature.MFCC29, "MFCC29");
	}
	public static final Feature[] Features = Feature.values(); 
	public static final Attribute ClassFeature;
	static
	{
		ArrayList<String> attrValues = new ArrayList<String>();
		attrValues.add(BLUES);
		attrValues.add(CLASSICAL);
		attrValues.add(COUNTRY);
		attrValues.add(DISCO);
		attrValues.add(HIPHOP);
		attrValues.add(JAZZ);
		ClassFeature = new Attribute("Genres", attrValues);
	}
}