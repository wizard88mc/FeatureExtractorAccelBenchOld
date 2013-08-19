/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package featureextractor;

import featureextractor.extractor.db.DbExtractor;
import featureextractor.model.Sample;
import featureextractor.ui.DeltaTimesGraph;
import featureextractor.ui.AxisValuesGraph;
import featureextractor.extractor.text.FileContentExtractor;
import featureextractor.model.SamplesBatch;
import java.io.File;
import java.util.ArrayList;
import javax.swing.JFrame;

/**
 *
 * @author Matteo
 */
public class FeatureExtractor extends JFrame {

    private static ArrayList<ArrayList<Sample>> valuesExtracted;
    private static boolean dbMode = true;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            if (args.length == 0) {
                throw new Exception("No argument provided");
            }
            if (dbMode) {
                if (args.length < 2) {
                    throw new Exception("DB mode: 2 arguments required (db path and action)");
                }
                String db_path = args[0];
                String action = args[1];
                System.out.println("Detected db path: "+args[0]);
                System.out.println("Detected action: "+args[1]);
                DbExtractor dbExtractor = new DbExtractor(new File(db_path));
                valuesExtracted = dbExtractor.extract(action);
                int i=1;
                for(ArrayList<Sample> batch: valuesExtracted) {
                    System.out.println("\tBatch "+i+" ("+batch.size()+" samples)");
                    DataAnalyzer analyzer = new DataAnalyzer(batch);
                    analyzer.searchForMaxOrMin();
                    analyzer.normalize();
                    analyzer.evaluateDeltaTimes();
                    analyzer.calculateFeatures();
                    System.out.println("Risultati: ");
                    System.out.println(analyzer);
                    SamplesBatch samplesBatch=new SamplesBatch(batch);
                    samplesBatch.getFeatures();
                    i++;
                }
            } 
//            else {
//                if (args.length < 1) {
//                    throw new Exception("Text mode: 1 argument required");
//                }
//                String fileName = args[0];
//                String fileSeparator = System.getProperty("file.separator");
//                String userDir = System.getProperty("user.dir");
//                String sourceFileString = userDir.concat(fileSeparator).concat("data").concat(fileSeparator).concat(fileName).concat(".txt");
//                FileContentExtractor extractor = new FileContentExtractor(sourceFileString);
//                valuesExtracted = extractor.extractValueFromFile();
//            }

            

//            DeltaTimesGraph graph = new DeltaTimesGraph(analyzer.startingData.get(0));
//            graph.setVisible(true);

//            AxisValuesGraph timeGraph = new AxisValuesGraph(analyzer.startingData);
//            timeGraph.setVisible(true);
        } catch (Exception e) {
            System.err.println("ECCEZIONE: " + e.getMessage());
            System.exit(-1);
        }
    }
}
