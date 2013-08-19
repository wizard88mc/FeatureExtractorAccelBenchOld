/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package featureextractor.model;

/**
 *
 * @author Matteo
 */
public class Sample {
    
    public double time;
    public double valueX;
    public double valueY;
    public double valueZ;
    
    public Sample(double time, double valueX, double valueY, double valueZ) {
        this.time = time; this.valueX = valueX; this.valueY = valueY;
        this.valueZ = valueZ;
    }

    public double getTime() {
        return time;
    }

    public double getValueX() {
        return valueX;
    }

    public double getValueY() {
        return valueY;
    }

    public double getValueZ() {
        return valueZ;
    }
    
    @Override
    public String toString() {
        return "[" + time + "," + valueX + "," + valueY + "," + valueZ + "]";
    }
}
