package featureextractor.position_analysis;

import featureextractor.model.DataTime;
import featureextractor.model.FeatureSet;
import static featureextractor.model.FeaturesSlidingWindow.AXIS;
import featureextractor.model.SingleCoordinateSet;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Matteo
 */
public class FeaturesMovement {
    
    public class FeaturesSet {
        
        public List<SingleCoordinateSet> values;
        public List<Double> means = new ArrayList<Double>();
        public List<Double> stds = new ArrayList<Double>();
        public List<Double> variances = new ArrayList<Double>();
        public List<Double> correlations = new ArrayList<Double>();
        public List<Double> mins = new ArrayList<Double>();
        public List<Double> maxes = new ArrayList<Double>();
        public List<Double> differenceMinMax = new ArrayList<Double>();
        public List<Double> ratios = new ArrayList<Double>();
        public List<Double> intelligentRatios = new ArrayList<Double>();
        public Double signalMagnitudeArea = 0.0;
        public Double magnitudeMean = 0.0;
        
        final private String[] AXIS = new String[] 
            {"X", "Y", "Z", "|V|", "(X+Y)/2"};
        final private String[] VALUES = new String[]
            {"mean", "std", "variance", "diffMinMax"};
        
        public FeaturesSet(List<SingleCoordinateSet> values, int frequency) {
            
            this.values = values;
            
            calculateMeans(frequency);
            calculateStds(frequency); calculateVariances(frequency);
            calculateCorrelations(frequency); calculateMins(frequency);
            calculateMaxes(frequency); calculateDifferencesMinMax();
            calculateIntelligentRatiosMinsMaxes();
            calculateMagnitudeMean();
            calculateSingalMagnitudeArea(frequency);
            calculateRatios();
        }
        
        private void calculateMeans(int frequency) {

            for (int i = 0; i < values.size(); i++) {
                means.add(values.get(i).getMean(frequency));
            }
        }
        
        private void calculateMagnitudeMean() {
        
            magnitudeMean = Math.sqrt(Math.pow(means.get(0), 2) +
                    Math.pow(means.get(1), 2) + 
                    Math.pow(means.get(2), 2));
        }
        
        private void calculateSingalMagnitudeArea(int frequency) {
        
            double minDelta = (double)1000000000 / frequency;
            double lastTimestamp = 0.0; int numberOfElements = 0;

            for (int i = 0; i < values.get(0).getValues().size(); i++) {
                if (values.get(0).getValues().get(i).getTime() - lastTimestamp > minDelta) {

                    signalMagnitudeArea += Math.abs(values.get(0).getValues().get(i).getValue()) + 
                            Math.abs(values.get(1).getValues().get(i).getValue()) + 
                            Math.abs(values.get(2).getValues().get(i).getValue());

                    numberOfElements++; 
                    lastTimestamp = values.get(0).getValues().get(i).getTime();
                }
            }

            signalMagnitudeArea /= (double)numberOfElements;
        }
        
        private void calculateVariances(int frequency) {
            
            for (int i = 0; i < values.size(); i++) {
                variances.add(values.get(i).getVariance(frequency));
            }
        }
        
        private void calculateStds(int frequency) {
            
            for (int i = 0; i < values.size(); i++) {
                stds.add(values.get(i).getStandardDeviation(frequency));
            }
        }
        
        private void calculateMins(int frequency) {
            
            for (int i = 0; i < values.size(); i++) {
                mins.add(values.get(i).getMin(frequency));
            }
        }
        
        private void calculateMaxes(int frequency) {

            for (int i = 0; i < values.size(); i++) {
                maxes.add(values.get(i).getMax(frequency));
            }
        }
        
        /**
        * Calculates the correlation between all the set of elements of the window, 
        * that are X, Y, Z, |V| and X+Y / 2
        * @param window: the sliding window with the set of values
        * @param frequency: The frequency at which calculate the correlations and use
        * data
        */
       private void calculateCorrelations(int frequency) {

           for (int i = 0; i < values.size() - 1; i++) {
               for (int j = i+1; j < values.size(); j++) {

                   Double covariance = calculateCovariance(values.get(i).getValues(), 
                           values.get(j).getValues(), frequency);

                   Double correlation = covariance / 
                           (stds.get(i) * stds.get(j));

                   if (Double.isNaN(correlation) || Double.isInfinite(correlation)) {
                       correlation = 0.0;
                   }

                   correlations.add(correlation);
               }
           }
       }

       /**
        * Calculates the covariance between two List of values
        * 
        * @param first: first set of values
        * @param second: second set of values
        * @param frequency: frequency at which use data and calculate covariance
        * @return The covariance between the two sets of data
        */
       private Double calculateCovariance(List<DataTime> first, List<DataTime> second, 
               int frequency) {

           Double covariance = 0.0, sumX = 0.0, sumY = 0.0, product = 0.0; 
           double minDelta = (double)1000000000 / frequency;
           double lastTimestamp = 0.0; int numberOfElements = 0;

           for (int i = 0; i < first.size(); i++) {
               if (first.get(i).getTime() - lastTimestamp >= minDelta) {
                   product += (first.get(i).getValue() * second.get(i).getValue());
                   sumX += first.get(i).getValue();
                   sumY += second.get(i).getValue();

                   numberOfElements++;
                   lastTimestamp = first.get(i).getTime();
               }
           }

           covariance = (product / numberOfElements) - 
                   ((sumX * sumY) / Math.pow(numberOfElements, 2));

           return covariance;
       }
       
       private void calculateDifferencesMinMax() {
           for (int i = 0; i < mins.size(); i++) {
               differenceMinMax.add(maxes.get(i) - mins.get(i));
           }
       }
       
       private void calculateIntelligentRatiosMinsMaxes() {
        
            Double ratioMaxes = maxes.get(2) / maxes.get(4),
                    ratioMins = Math.abs(mins.get(2) / mins.get(4));
            if (Double.isInfinite(ratioMaxes) || Double.isNaN(ratioMaxes)) {
                ratioMaxes = 0.0;
            }
            if (Double.isInfinite(ratioMins) || Double.isNaN(ratioMins)) {
                ratioMins = 0.0;
            }
            intelligentRatios.add(ratioMaxes);
            intelligentRatios.add(ratioMins);
        }
       
       /**
        * Calculates ratios between mean, std, variance and difference min/max values
        * between all the axis
        */
       private void calculateRatios() {

           for (int i = 0; i < means.size() -1 ; i++) {
               for (int j = i+1; j < means.size(); j++) {

                   Double ratioMean = means.get(i) / means.get(j),
                           ratioStd = stds.get(i) / stds.get(j),
                           ratioVariance = variances.get(i) / variances.get(j),
                           ratioMinMax = differenceMinMax.get(i) / differenceMinMax.get(j);
                   
                   if (Double.isNaN(ratioMean) || Double.isInfinite(ratioMean)) {
                       ratioMean = 0.0;
                   }
                   if (Double.isNaN(ratioStd) || Double.isInfinite(ratioStd)) {
                       ratioStd = 0.0;
                   }
                   if (Double.isNaN(ratioVariance) || Double.isInfinite(ratioVariance)) {
                       ratioVariance = 0.0;
                   }
                   if (Double.isNaN(ratioMinMax) || Double.isInfinite(ratioMinMax)) {
                       ratioMinMax = 0.0;
                   }
                   ratios.add(ratioMean);
                   ratios.add(ratioStd);
                   ratios.add(ratioVariance);
                   ratios.add(ratioMinMax);
               }
           }
       }
       
       public List<String> getAllAttributesName() {
        
            List<String> attributes = new ArrayList<String>();

            /**
             * Base features attributes
             */
            for (int i = 0; i < AXIS.length; i++) {
                for (int j = 0; j < means.size(); j++) {
                    attributes.add(AXIS[i] + "_" + VALUES[j]);
                }  
            }

            /**
             * Ratios attributes
             */
            for (int i = 0; i < AXIS.length; i++) {
                for (int k = i+1; k < AXIS.length; k++) {
                    for (int j = 0; j < VALUES.length; j++) {
                        attributes.add("RATIO:"+AXIS[i] + VALUES[j] + "_" + AXIS[k] + VALUES[j]);
                    }  
                }
            }

            attributes.add("RATIO:MAX(Z)_MAX(X+Y/2)");        
            attributes.add("RATIO:|MIN(Z)_MIN(X+Y/2)|");

            /**
             * Covariance attributes
             */
            for (int i = 0; i < AXIS.length; i++) {
                for (int k = i+1; k < AXIS.length; k++) {

                    attributes.add("CORRELATION:"+AXIS[i] + "_" + AXIS[k] );  
                }
            }

            attributes.add("MAGNITUDE_AREA");
            attributes.add("SIGNAL_MAGNITUDE_AREA");

            return attributes;
         }
    }
    
    private String startPosition;
    private String endPosition;
    private List<SingleCoordinateSet> valuesToUseAccelerometer = new ArrayList<SingleCoordinateSet>();
    private List<SingleCoordinateSet> valuesToUseLinear = new ArrayList<SingleCoordinateSet>();
    public FeaturesSet featuresAccelerometer;
    public FeaturesSet featuresLinear;
    
    public FeaturesMovement(Movement movement, int frequency, Double bufferDuration) throws Exception {
        
        this.startPosition = movement.getStartPosition();
        this.endPosition = movement.getEndPosition();
        
        searchForSuitableData(movement, bufferDuration);
        
        featuresAccelerometer = new FeaturesSet(valuesToUseAccelerometer, frequency);
        featuresLinear = new FeaturesSet(valuesToUseLinear, frequency);
    }
    
    /**
     * Retrieves all the data that starts from the beginning to the event
     * when the proximity sensor changes state from a value > 0 to a value = 0
     * and eliminates all the values = null at the beginning of the movement
     * 
     * @param movement: the analyzed movement
     */
    private void searchForSuitableData(Movement movement, Double bufferDuration) throws Exception {
        
        SingleCoordinateSet valuesProximity = movement.getProximityValues();
        boolean endSearch = false; int indexLastValueForBuffer = -1;
        
        for (int i = valuesProximity.size() - 1; i >= 1 && !endSearch; i--) {
            if (valuesProximity.getValues().get(i).getValue() <= 0 && 
                    valuesProximity.getValues().get(i - 1).getValue() > 0) {
                
                endSearch = true;
                indexLastValueForBuffer = i - 1;
                
            }
        }
        
        for (int i = 0; i < movement.getAccelerometerWithoutGravity().size(); i++) {
            
            valuesToUseAccelerometer.add(i, new SingleCoordinateSet(movement.getAccelerometerWithoutGravity().get(i).getSubsetData(0, indexLastValueForBuffer)));
            valuesToUseLinear.add(i, new SingleCoordinateSet(movement.getLinearValues().get(i).getSubsetData(0, indexLastValueForBuffer)));
        }
        
        while (valuesToUseAccelerometer.get(0).size() > 0 && 
                valuesToUseAccelerometer.get(0).getValues().get(0) == null) {
            
            for (int i = 0; i < valuesToUseAccelerometer.size(); i++) {
                valuesToUseAccelerometer.get(i).getValues().remove(0);
                valuesToUseLinear.get(i).getValues().remove(0);
            }
        }
        
        if (valuesToUseAccelerometer.get(0).getValues().get(valuesToUseAccelerometer.get(0).size() - 1).getTime() -
                valuesToUseAccelerometer.get(0).getValues().get(0).getTime() < bufferDuration) {
            
            throw new Exception("Not sufficient data for this movement");
        }
    }
    
    public List<String> getAttributesNames() {
        return featuresAccelerometer.getAllAttributesName();
    }
    
    public List<Double> getMeans(boolean linear) {
        if (!linear) {
            return featuresAccelerometer.means;
        }
        else {
            return featuresLinear.means;
        }
    }
    
    public String getEndPosition() {
        return this.endPosition;
    }
}
