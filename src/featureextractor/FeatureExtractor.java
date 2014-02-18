/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package featureextractor;

import featureextractor.comparator.MeanComparator;
import featureextractor.extractor.db.AccelBenchException;
import featureextractor.extractor.db.DBDataManager;
import featureextractor.utils.SamplesUtils;
import featureextractor.extractor.db.DbExtractor;
import featureextractor.model.Sample;
import featureextractor.model.Batch;
import featureextractor.model.DataTime;
import featureextractor.model.FeatureSet;
import featureextractor.model.FeaturesSlidingWindow;
import featureextractor.model.SingleCoordinateSet;
import featureextractor.model.SlidingWindow;
import featureextractor.model.TimeFeature;
import featureextractor.model.TrunkFixSpec;
import featureextractor.plot.Plot;
import featureextractor.plot.PlotForDB;
import featureextractor.weka.ARFF;
import featureextractor.weka.ARFFAttribute;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author Matteo
 */
public class FeatureExtractor {

    public static boolean GRAVITY_REMOVE = false;
    public boolean linear = false;
    private static final String ARFF_RELATION = "StairDetection";
    private ARFF arff;
    private DbExtractor db_extractor = null;
    private DBDataManager dbDataManager = null;
    private List<Batch> batches = null;
    private int range = 1000; // default
    private int start = 0; // default
    private int max = range; // default
    private boolean arff_enabled = true;
    private boolean feature_enabled = true;
    private long time_range = 488000000; // ms
    private long sizeSlidingWindow = 500000000; // milliseconds
    private int numberOverlappingWindows = 1;
    List<SlidingWindow> slidingWindowsDownstairs, slidingWindowsUpstairs, slidingWindowsNoStairs;
    private String[] features_types = new String[]{"std", "mean", "variance"};

    public enum BATCH_CREATION_MODE {

        ALL, // all samples
        NON_INTERLAPPING_FIXED_SIZE, // non interlapping sliding window
        INTERLAPPING_FIXED_SIZE, // interlapping sliding window
        RANGE_FROM_START, // range from beginning 
        RANGE, // range from given index
        BY_TRUNK, // group by trunk
        FIXED_TIME_LAPSE, // extract by fixed time lapse (in ms),
        BY_STEP, // group by step
        INTERLAPPING_SIZE_BY_STEP_AVG, // interlapping sliding window with size = average samples per step
        NON_INTERLAPPING_SIZE_BY_STEP_AVG // interlapping sliding window with size = average samples per step
    };
    private int batch_size = 40; // default
    private BATCH_CREATION_MODE mode = BATCH_CREATION_MODE.NON_INTERLAPPING_FIXED_SIZE; // default
    private int axis_to_be_considered = 3; // (4 == |V|)

    public FeatureExtractor() {
//        this.initialize_ARFF();
        //this.initialize_std_ARFF(axis_to_be_considered, true, true, true, true, 
         //       true, true, true, true, true);
        this.initializeARFF();
        
    }
    
    public void setLinearOrNot(boolean linear) {
        this.linear = linear;
    }
    
    public void setNumberOverlappingWindow(int number) {
        numberOverlappingWindows = number;
    }
    
    public DBDataManager getDBManager() {
        return dbDataManager;
    }

    public void setDb(String db_path) throws Exception {
        File file = new File(db_path);
        if (file.exists() == false) {
            throw new FileNotFoundException(file.getAbsolutePath() + " not found");
        }
        db_extractor = new DbExtractor(file);
    }
    
    public void createFinalDB() {
        try {
            dbDataManager = new DBDataManager((int)sizeSlidingWindow / 1000000);
        }
        catch(IOException exc) {
            System.out.println(exc);
        }
    }

    public long getTime_range() {
        return time_range;
    }

    public void setTime_range(long time_range) {
        this.time_range = time_range;
    }

    public float getAverageStepDuration() throws Exception {
        float avg_for_step = db_extractor.getAvgSamplesForStep(this.linear);
        float sampling_rate = db_extractor.getSamplingRate(this.linear);
        float ratio = avg_for_step / sampling_rate;
        System.out.println("Avg samples for step: " + avg_for_step);
        System.out.println("Sampling rate: " + sampling_rate);
        System.out.println("ratio: " + ratio);
        return ratio;
    }

    public DbExtractor getDbExtractor() {
        return this.db_extractor;
    }

    public void setFeatureEnabled(boolean feature_enabled) {
        this.feature_enabled = feature_enabled;
    }

    public void setArffEnabled(boolean arff_enabled) {
        this.arff_enabled = arff_enabled;
    }

    public void setBatchSize(int batch_size) {
        this.batch_size = batch_size;
    }

    public void setBatchCreationMode(BATCH_CREATION_MODE mode) {
        this.mode = mode;
    }

    public void setTrunkIDs() throws Exception {
        if (db_extractor == null) {
            throw new Exception("No source DB set");
        }
        db_extractor.setTrunkIDs(this.linear);
    }

    public int getSamplesCount() throws Exception {
        return db_extractor.getSamplesCount(this.linear);
    }

    public int getStairSamplesCount() throws Exception {
        return db_extractor.getStairSamplesCount(this.linear);
    }

    public int getNonstairSamplesCount() throws Exception {
        return db_extractor.getNonStairSamplesCount(this.linear);
    }

    public void applyTrunkFixes(List<TrunkFixSpec> fixes) throws Exception {
        if (db_extractor == null) {
            throw new Exception("No source DB set");
        }
        db_extractor.applyTrunkFixes(fixes, this.linear);
    }

    public void extract() throws Exception {
        this.extract(null, null);
    }

    public void enableMinDiff(float minDiff) throws Exception {
        if (batches == null || batches.isEmpty()) {
            return;
        }
        for (Batch batch : batches) {
            for (SingleCoordinateSet set : batch.getValues()) {
                double last, previous;
                boolean initialized = false;
                int i = 0;
                for (DataTime dt : set.getValues()) {
                    if (!initialized) {
                        last = dt.getValue();
                        initialized = true;
                    } else {
                        previous = set.getValues().get(i - 1).getValue();
                        if (Math.abs(previous - dt.getValue()) < minDiff) {
                            dt.setValue(previous);
                        }
                    }
                    i++;
                }
            }
        }
    }

    public boolean isGravity_remove() {
        return GRAVITY_REMOVE;
    }

    public void setGravity_remove(boolean gravity_remove) {
        this.GRAVITY_REMOVE = gravity_remove;
    }
    
    public void populateDatabase() {
        
        try {
            /**
             * Creates the batch using steps
             */
            List<Batch> baseBatchesDownstairs = db_extractor.extractByTrunkAndAction("STAIR_DOWNSTAIRS");
            List<Batch> baseBatchesUpstairs = db_extractor.extractByTrunkAndAction("STAIR_UPSTAIRS");
            List<Batch> baseBatchesNoStairs = db_extractor.extractByTrunkAndAction("NON_STAIR");
            /**
             * Once I have the batches, for each batch I have to create the corresponding 
             * set of sliding window 
             */
            List<SlidingWindow> windowsAccelerometerNoGravityDownstairs = new ArrayList<SlidingWindow>();
            List<SlidingWindow> windowsLinearDownstairs = new ArrayList<SlidingWindow>();
            List<SlidingWindow> windowsAccelerometerNoGravityUpstairs = new ArrayList<SlidingWindow>();
            List<SlidingWindow> windowsLinearUpstairs = new ArrayList<SlidingWindow>();
            List<SlidingWindow> windowsAccelerometerNoGravityNoStairs = new ArrayList<SlidingWindow>();
            List<SlidingWindow> windowsLinearNoStairs = new ArrayList<SlidingWindow>();
            
            for (int i = 0; i < baseBatchesDownstairs.size(); i++) {
                
                windowsAccelerometerNoGravityDownstairs.addAll(
                        SamplesUtils.getBatchesWithSlidingWindowAndFixedTime(baseBatchesDownstairs.get(i), sizeSlidingWindow, 
                                numberOverlappingWindows, false));
                
                windowsLinearDownstairs.addAll(
                    SamplesUtils.getBatchesWithSlidingWindowAndFixedTime(baseBatchesDownstairs.get(i), sizeSlidingWindow, 
                            numberOverlappingWindows, true));
            }
            
            for (int i = 0; i < baseBatchesUpstairs.size(); i++) {
                
                windowsAccelerometerNoGravityUpstairs.addAll(
                    SamplesUtils.getBatchesWithSlidingWindowAndFixedTime(baseBatchesUpstairs.get(i), sizeSlidingWindow, 
                            numberOverlappingWindows, false));
                
                windowsLinearUpstairs.addAll(
                    SamplesUtils.getBatchesWithSlidingWindowAndFixedTime(baseBatchesUpstairs.get(i), sizeSlidingWindow, 
                            numberOverlappingWindows, true));
            }
            
            for (int i = 0; i < baseBatchesNoStairs.size(); i++) {
                
                windowsAccelerometerNoGravityNoStairs.addAll(
                    SamplesUtils.getBatchesWithSlidingWindowAndFixedTime(baseBatchesNoStairs.get(i), sizeSlidingWindow, 
                            numberOverlappingWindows, false));
                
                windowsLinearNoStairs.addAll(
                    SamplesUtils.getBatchesWithSlidingWindowAndFixedTime(baseBatchesNoStairs.get(i), sizeSlidingWindow, 
                            numberOverlappingWindows, true));
            }
            int k = 0;
            for (SlidingWindow window: windowsAccelerometerNoGravityNoStairs) {
                if (k % 20 == 0) {
                    System.out.println("Accelerometro: Inserisco " + k + "di " + windowsAccelerometerNoGravityNoStairs.size());
                }
                dbDataManager.addNewSlidingWindow(window, App.NO_STAIR, false);
                k++;
            }
            k = 0;
            for (SlidingWindow window: windowsLinearNoStairs) {
                if (k % 20 == 0) {
                    System.out.println("Linear: Inserisco " + k + "di " + windowsLinearNoStairs.size());
                }
                dbDataManager.addNewSlidingWindow(window, App.NO_STAIR, true);
                k++;
            }
            
            for (int i = windowsAccelerometerNoGravityDownstairs.size() - 1; 
                    i >= 0; i--) {
                
                new PlotForDB(windowsAccelerometerNoGravityDownstairs.get(i), dbDataManager, false);
                if (i%10 == 0) {
                    System.out.println("waiting");
                }
            }
            
            for (int i = windowsLinearDownstairs.size() - 1; i>= 0; i--) {
                new PlotForDB(windowsLinearDownstairs.get(i), dbDataManager, true);
                if (i%10 == 0) {
                    System.out.println("waiting");
                }
            }
            
            for (int i = windowsAccelerometerNoGravityUpstairs.size() - 1; 
                    i >= 0; i--) {
                
                new PlotForDB(windowsAccelerometerNoGravityUpstairs.get(i), dbDataManager, false);
                if (i%10 == 0) {
                    System.out.println("waiting");
                }
            }
            
            for (int i = windowsLinearUpstairs.size() - 1; i>= 0; i--) {
                new PlotForDB(windowsLinearUpstairs.get(i), dbDataManager, true);
                if (i%10 == 0) {
                    System.out.println("waiting");
                }
            }
        }
        catch(Exception exc) {
            exc.printStackTrace();
            System.out.println(exc);
        }
        
    }

    public void extract(String action, String className) throws Exception {
        if (db_extractor == null) {
            throw new Exception("No source DB set");
        }
        try {
            System.out.println("Detected sampling rate: " + db_extractor.getSamplingRate(true) + "Hz");
            // create samples from db rows            
            ArrayList<Sample> samplesAccelerometer = db_extractor.extract(action, false);
              
            // create samples batches by selected mode
            batches = null;
            switch (mode) {
                case INTERLAPPING_FIXED_SIZE:
                    System.out.println("Selected interlapping sliding window with a fixed size of " + batch_size + " samples");
                    batches = SamplesUtils.getInterlappingFixedSizeBatches(samplesAccelerometer, batch_size);
                    break;
                case INTERLAPPING_SIZE_BY_STEP_AVG:
                    try {
                        batch_size = db_extractor.getAvgSamplesForStep(this.linear);
                    } catch (ArithmeticException e) { // no step detected: get average batch size by calculating on sampling delay
                        batch_size = (int) (0.77 * (float) db_extractor.getSamplingRate(this.linear));
                    }
                    if (batch_size % 2 == 1) {
                        batch_size++; // make sure it's an even number
                    }
                    System.out.println("Selected interlapping sliding window with a fixed size of " + batch_size + " samples (average step sampling)");
                    batches = SamplesUtils.getInterlappingFixedSizeBatches(samplesAccelerometer, batch_size);
                    break;
                case NON_INTERLAPPING_SIZE_BY_STEP_AVG:
                    batch_size = db_extractor.getAvgSamplesForStep(this.linear);
                    System.out.println("Selected non-interlapping sliding window with a fixed size of " + batch_size + " samples (average step sampling)");
                    batches = SamplesUtils.getNonInterlappingFixedSizeBatches(samplesAccelerometer, batch_size);
                    break;
                case NON_INTERLAPPING_FIXED_SIZE:
                    System.out.println("Selected non-interlapping sliding window with a fixed size of " + batch_size + " samples");
                    batches = SamplesUtils.getNonInterlappingFixedSizeBatches(samplesAccelerometer, batch_size);
                    break;
                case RANGE:
                    System.out.println("Selected range " + start + " - " + max);
                    batches = SamplesUtils.getRangeBatch(samplesAccelerometer, start, max);
                    break;
                case RANGE_FROM_START:
                    System.out.println("Selected first " + range + " samples");
                    batches = SamplesUtils.getSingleFixedSizeBatch(samplesAccelerometer, range);
                    break;
                case FIXED_TIME_LAPSE:
                    System.out.println("Selected fixed time range (" + time_range + " ms)");
                    batches = SamplesUtils.getBatchesByTimeRange(samplesAccelerometer, time_range);
                    break;
                case BY_TRUNK:
                    System.out.println("Selected batches by trunk");
                    batches = SamplesUtils.getBatchesByTrunk(samplesAccelerometer, db_extractor, this.linear);
                    break;
                case BY_STEP:
                    System.out.println("Selected batches by step");
                    batches = SamplesUtils.getBatchesByStep(samplesAccelerometer);
                    break;
                case ALL:
                    System.out.println("Selected a single batch with all samples");
                    //batches = SamplesUtils.getAll(samples);
                    break;
                default:
                    throw new Exception("Unknown batch creation mode");
            }

            // loop through batches
            int i = 1;
            arff.addClass(className);
            for (Batch batch : batches) {
//                System.out.println("\n*** Batch " + i + " *** (" + batch.size() + " samples)");
                
                List<FeatureSet> features = null;
                if (feature_enabled) {
                    features = batch.getFeatures();
//                    batch.printFeatures();
                    if (arff_enabled) {
                        
                        FeatureSet featuresV = features.get(axis_to_be_considered);
                        
                        features = features.subList(0, axis_to_be_considered); // remove |V|
                        Collections.sort(features, new MeanComparator());
                        
                        TimeFeature timeFeature = new TimeFeature(batch, features);
                        
                        List<Double> ratios = calculateRatios(features, true, true, true, true);
                        
                        features.add(featuresV);
                        
                        arff.addAllFeaturesData(className, features, timeFeature, ratios);
                    }
                }
                i++;
            }
        } catch (AccelBenchException e) {
            System.err.println(e.getMessage());
        } catch (Exception e) {
            System.err.println("ECCEZIONE: " + e.getMessage());
            e.printStackTrace();
            System.exit(-1);
        }
    }
    
    public void retrieveSlidingWindows(boolean linear) {
        if (slidingWindowsDownstairs != null) {
            slidingWindowsDownstairs.clear(); slidingWindowsUpstairs.clear();
            slidingWindowsNoStairs.clear();
        }
        System.out.println("Starting getting sliding windows");
        slidingWindowsDownstairs = dbDataManager.getSlidinwWindows((int)sizeSlidingWindow / 1000000, App.STAIR_DOWNSTAIRS, linear);
        System.out.println("Got all Sliding windows downstairs");
        slidingWindowsUpstairs = dbDataManager.getSlidinwWindows((int)sizeSlidingWindow / 1000000, App.STAIR_UPSTAIRS, linear);
        System.out.println("Got all sliding windows upstairs");
        slidingWindowsNoStairs = dbDataManager.getSlidinwWindows((int)sizeSlidingWindow / 1000000, App.NO_STAIR, linear);
        System.out.println("Got all sliding window no stairs");
        System.out.println("Got all Sliding windows");
    }

    public void extract(int frequencyData) throws Exception {
        
        if (dbDataManager == null ) {
            throw new Exception("No DBDataManager");
        }
        
        System.out.println("Starting cleaning Windows");
        List<SlidingWindow> finalWindowsDownstairs = getOnlySuitableSlidingWindows(slidingWindowsDownstairs, frequencyData),
                finalWindowsUpstairs = getOnlySuitableSlidingWindows(slidingWindowsUpstairs, frequencyData),
                finalWindowsNoStairs = getOnlySuitableSlidingWindows(slidingWindowsNoStairs, frequencyData);
        System.out.println("All SlidingWindow cleaned");
        
        System.out.println("Starting calculate features");
        List<FeaturesSlidingWindow> featuresWindowsDownstairs = getFeatures(finalWindowsDownstairs, frequencyData);
        System.out.println("All features for downstairs");
        List<FeaturesSlidingWindow> featuresWindowsUpstairs = getFeatures(finalWindowsUpstairs, frequencyData);
        System.out.println("All features for upstairs");
        List<FeaturesSlidingWindow> featuresWindowsNoStairs = getFeatures(finalWindowsNoStairs, frequencyData);
        System.out.println("Got all features");
        
        arff.addAllFeaturesData(App.STAIR_DOWNSTAIRS, featuresWindowsDownstairs);
        arff.addAllFeaturesData(App.STAIR_UPSTAIRS, featuresWindowsUpstairs);
        arff.addAllFeaturesData(App.NO_STAIR, featuresWindowsNoStairs);
        
        System.out.println("Number of downstairs windows: " + finalWindowsDownstairs.size());
        System.out.println("Number of upstairs windows: " + finalWindowsUpstairs.size());
        System.out.println("Number of no stairs windows: " + finalWindowsNoStairs.size());
        
    }
    
    /**
     * Retrieves the list of the sliding windows suitable for a particular frequency 
     * analysis. It check whether the number of points in the sliding window is 
     * greater or equal to the frequency.
     * 
     * @param possibleWindows: initial windows retrieved from the DB
     * @param frequency: frequency at which we want to perform our analysis
     * @return a list of SlidingWindow that have at least the number of points 
     * equal to the frequency
     */
    private List<SlidingWindow> getOnlySuitableSlidingWindows(List<SlidingWindow> possibleWindows, int frequency) {
        List<SlidingWindow> finalWindows = new ArrayList<SlidingWindow>();
        
        for (SlidingWindow window: possibleWindows) {
            if (window.getValues().get(0).size() >= frequency) {
                finalWindows.add(window);
            }
        }
        
        return finalWindows;
    }
    
    private List<FeaturesSlidingWindow> getFeatures(List<SlidingWindow> slidingWindows, int frequency) {
        
        List<FeaturesSlidingWindow> listFeaturesSet = new ArrayList<FeaturesSlidingWindow>();
        
        for (int i = 0; i < slidingWindows.size(); i++) {
            
            listFeaturesSet.add(new FeaturesSlidingWindow(slidingWindows.get(i), frequency));
        }
        
        return listFeaturesSet;
    }
    
    public void setRange(int range) {
        this.range = range;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public void setMax(int max) {
        this.max = max;
    }
    
    public void setSlidingWindowSize(long size) {
        this.sizeSlidingWindow = size;
    }

    public void plot() {
        int max_plot = 200;
        for (Batch batch : batches) {
            if (max_plot > 0) {
                new Plot(batch, this.db_extractor, true, false, false, false);
                new Plot(batch, this.db_extractor, true, false, false, true);
                new Plot(batch, this.db_extractor, false, true, false, false);
                new Plot(batch, this.db_extractor, false, true, false, true);
                new Plot(batch, this.db_extractor, false, false, true, false);
                new Plot(batch, this.db_extractor, false, false, true, true);
            }
            max_plot--;
//            GralPlot plot2 = new GralPlot(batch);
//            plot2.setVisible(true);
        }
    }

    public void plot(int start) throws Exception {
        if (start > batches.size()) {
            throw new Exception(start + " > detected batches (" + batches.size() + ")");
        }
        int i = 0;
        for (Batch batch : batches) {
            if (i >= start) {
                new Plot(batch, this.db_extractor, true, false, false, false);
                new Plot(batch, this.db_extractor, true, false, false, true);
                new Plot(batch, this.db_extractor, false, true, false, false);
                new Plot(batch, this.db_extractor, false, true, false, true);
                new Plot(batch, this.db_extractor, false, false, true, false);
                new Plot(batch, this.db_extractor, false, false, true, true);
            }
            i++;
        }
    }

    public void plot(int start, int end) throws Exception {
        if (start > batches.size()) {
            throw new Exception(start + " > detected batches (" + batches.size() + ")");
        }
        int i = 0;
        for (Batch batch : batches) {
            if (i >= start && i <= end) {
                new Plot(batch, this.db_extractor, true, false, false, false);
                new Plot(batch, this.db_extractor, true, false, false, true);
                new Plot(batch, this.db_extractor, false, true, false, false);
                new Plot(batch, this.db_extractor, false, true, false, true);
                new Plot(batch, this.db_extractor, false, false, true, false);
                new Plot(batch, this.db_extractor, false, false, true, true);
            }
            i++;
        }
    }
    
    private List<Double> calculateRatios(List<FeatureSet> features, boolean mean, 
            boolean variance, boolean std, boolean minMax) {
        
        List<Double> ratios = new ArrayList<Double>();
        
        if (mean) {
            for (int i = 0; i < features.size() - 1; i++) {
                for (int j = i + 1; i < features.size(); i++) {
                    ratios.add(Double.valueOf(features.get(i).getMean() / 
                            features.get(j).getMean()));
                }
            }
        }
        
        if (variance) {
            for (int i = 0; i < features.size() - 1; i++) {
                for (int j = i + 1; i < features.size(); i++) {
                    ratios.add(Double.valueOf(features.get(i).getVariance() / 
                            features.get(j).getVariance()));
                }
            }
        }
        
        if (std) {
            for (int i = 0; i < features.size() - 1; i++) {
                for (int j = i + 1; i < features.size(); i++) {
                    ratios.add(Double.valueOf(features.get(i).getStd() / 
                            features.get(j).getStd()));
                }
            }
        }
        
        if (minMax) {
            for (int i = 0; i < features.size() - 1; i++) {
                for (int j = i + 1; j < features.size(); j++) {
                    ratios.add(Double.valueOf(features.get(i).getDifferenceMinMax() / 
                            features.get(j).getDifferenceMinMax()));
                }
            }
        }
        
        return ratios;
    }

    public void dumpARFF(File file) throws IOException {
        System.out.println("\nWriting ARFF file to " + file.getAbsolutePath());
        arff.writeToFile(file);
    }

    private void initialize_std_ARFF(int axes, boolean std, boolean mean, boolean variance, 
            boolean vFeatures, boolean timeFeatures, boolean ratioMean, 
            boolean ratioVariance, boolean ratioStd, boolean ratioMinMax) {
        // default ARFF attributes and initializazion 
        List<ARFFAttribute> attributes = new ArrayList<ARFFAttribute>();

        for (int i = 0; i < axes; i++) {
            if (std) {
                attributes.add(new ARFFAttribute(features_types[0] + i, "REAL"));
            }
            if (mean) {
                attributes.add(new ARFFAttribute(features_types[1] + i, "REAL"));
            }
            if (variance) {
                attributes.add(new ARFFAttribute(features_types[2] + i, "REAL"));
            }
        }
        
        if (vFeatures) {
            if (std) {
                attributes.add(new ARFFAttribute(features_types[0] + "_V", "REAL"));
            }
            if (mean) {
                attributes.add(new ARFFAttribute(features_types[1] + "_V", "REAL"));
            }
            if (variance) {
                attributes.add(new ARFFAttribute(features_types[2] + "_V ", "REAL"));
            }
        }
        
        if (timeFeatures) {
            attributes.add(new ARFFAttribute("MAGNITUDE_MEAN", "REAL"));
            attributes.add(new ARFFAttribute("SIGNAL_MAGNITUDE_AREA", "REAL"));

            for (int i = 0; i < axes - 1; i++) {
                for (int j = i+1; j < axes; j++) {
                    attributes.add(new ARFFAttribute("CORRELATION_" + i + "_" + j, "REAL"));
                }
            }
        }
        
        if (ratioMean || ratioVariance || ratioStd) {
            for (int i = 0; i < axes - 1; i++) {
                for (int j = i+1; j < axes; j++) {
                    if (ratioMean) {
                        attributes.add(new ARFFAttribute("RATIO_MEAN_" + i + "_" + j, "REAL"));
                    }
                    if (ratioVariance) {
                        attributes.add(new ARFFAttribute("RATIO_VARIANCE_" + i + "_" + j, "REAL"));
                    }
                    if (ratioStd) {
                        attributes.add(new ARFFAttribute("RATIO_STD_" + i + "_" + j, "REAL"));
                    }
                }
            }
        }
        
        if (ratioMinMax) {
            for (int i = 0; i < axes - 1; i++) {
                for (int j = i + 1; j < axes; j++) {
                    attributes.add(new ARFFAttribute("RATIO_DIFF_MINMAX_" + i + "_" + j, "REAL"));
                }
            }
        }

        // new ARFF document instance
        arff = new ARFF(ARFF_RELATION, attributes);
    }

    public ARFF getARFF() {
        return this.arff;
    }
    
    public void initializeARFF() {
        
        List<String> attributesString = FeaturesSlidingWindow.getAllAttributesName();
        List<ARFFAttribute> attributes = new ArrayList<ARFFAttribute>();
        
        for (int i = 0; i < attributesString.size(); i++) {
            attributes.add(new ARFFAttribute(attributesString.get(i), "REAL"));
        }
        
        arff = new ARFF(ARFF_RELATION, attributes);
        
    }
    
    private void searchForCopyOfSlidingWindow(List<SlidingWindow> slidingWindows, boolean linear) {
        
        for (int i = 0; i < slidingWindows.size(); i++) {
            
            for (int j = i+1; j < slidingWindows.size(); ) {
                if (slidingWindows.get(i).equals(slidingWindows.get(j))) {

                    // System.out.println("Window " + i + " uguale window " + j);
                    
                    // Remove sliding window from the db and from the List
                    dbDataManager.deleteTrunk((int)sizeSlidingWindow/1000000, 
                            slidingWindows.get(j).getTrunk(), linear, slidingWindows.get(j).getSupposedAction());
                    slidingWindows.remove(j);
                }
                else {
                    j++;
                }
            }
            if (i%30 == 0) {
                System.out.println("Completed test " + i + " of " + slidingWindows.size());
            }
        }
        
    }
    
    public void cleanDB() {
        
        System.out.println("**********Starting sliding window downstairs**********");
        List<SlidingWindow> slidingWindowsDownstairs = dbDataManager.getSlidinwWindows((int)sizeSlidingWindow / 1000000, App.STAIR_DOWNSTAIRS, false);
        System.out.println("Downstairs: " + slidingWindowsDownstairs.size());
        searchForCopyOfSlidingWindow(slidingWindowsDownstairs, false);
        System.out.println("New size Downstairs: " + slidingWindowsDownstairs.size());
        
        System.out.println("**********Starting sliding window upstairs**********");
        List<SlidingWindow> slidingWindowsUpstairs = dbDataManager.getSlidinwWindows((int)sizeSlidingWindow / 1000000, App.STAIR_UPSTAIRS, false);
        System.out.println("Upstairs: " + slidingWindowsUpstairs.size());
        searchForCopyOfSlidingWindow(slidingWindowsUpstairs, false);
        System.out.println("New size Upstairs: " + slidingWindowsUpstairs.size());
        
        System.out.println("**********Starting sliding window downstairs linear**********");
        List<SlidingWindow> slidingWindowsDownstairsLinear = dbDataManager.getSlidinwWindows((int)sizeSlidingWindow / 1000000, App.STAIR_DOWNSTAIRS, true);
        System.out.println("Downstairs Linear: " + slidingWindowsDownstairsLinear.size());
        searchForCopyOfSlidingWindow(slidingWindowsDownstairsLinear, true);
        System.out.println("New size downstairs linear: " + slidingWindowsDownstairsLinear.size());
        
        System.out.println("**********Starting sliding window upstairs linear**********");
        List<SlidingWindow> slidingWindowsUpstairsLinear = dbDataManager.getSlidinwWindows((int)sizeSlidingWindow / 1000000, App.STAIR_UPSTAIRS, true);
        System.out.println("Upstairs Linear: " + slidingWindowsUpstairsLinear.size());
        searchForCopyOfSlidingWindow(slidingWindowsUpstairsLinear, true);
        System.out.println("New size upstairs linear: " + slidingWindowsUpstairsLinear.size());
        
    }
}
