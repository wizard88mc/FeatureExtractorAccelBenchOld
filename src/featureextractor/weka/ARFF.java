/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package featureextractor.weka;

import featureextractor.model.FeatureSet;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author Nicola Beghin
 */
public class ARFF {

    private String title = "StairDetection";
    private List<ARFFAttribute> attributes = new ArrayList<ARFFAttribute>();
    private List<String> classes = new ArrayList<String>();
    private List<ARFFData> data = new ArrayList<ARFFData>();

    public ARFF(String title, List<ARFFAttribute> attributes) {
        this.title = title;
        this.attributes = attributes;
    }

    public String getTitle() {
        return title;
    }

    public List<ARFFAttribute> getAttributes() {
        return attributes;
    }

    public List<String> getClasses() {
        return classes;
    }

    public List<ARFFData> getData() {
        return data;
    }

    
    public void addClass(String className) {
        if (this.classes.contains(className)==false) this.classes.add(className);
    }
    
    public void writeToFile(File file) throws IOException {
        if (file.exists()) {
            file.delete();
        }
        FileUtils.writeStringToFile(file, this.toString());
    }

    public void addData(List<ARFFData> data) {
        this.data.addAll(data);
    }

    public void addData(String title, FeatureSet featureSet) {
        List<Double> data_row = new ArrayList<Double>();
        data_row.add(featureSet.getMean()); // |V|
        data_row.add(featureSet.getVariance()); // |V|
        data_row.add(featureSet.getStd()); // |V|
        this.addData(new ARFFData(title, data_row));
    }

    public void addData(ARFFData data) {
        this.data.add(data);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("@RELATION " + title);
        for (ARFFAttribute attribute : attributes) {
            sb.append("\n" + attribute);
        }
        sb.append("\n@ATTRIBUTE class {" + StringUtils.join(classes, ",") + "}");
        sb.append("\n@DATA");
        for (ARFFData row : data) {
            sb.append("\n" + row);
        }
        return sb.toString();
    }
}
