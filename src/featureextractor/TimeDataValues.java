/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package featureextractor;

/**
 *
 * @author Matteo
 */
public class TimeDataValues {
    
    public double time;
    public double valueX;
    public double valueY;
    public double valueZ;
    
    public TimeDataValues(double time, double valueX, double valueY, double valueZ) {
        this.time = time; this.valueX = valueX; this.valueY = valueY;
        this.valueZ = valueZ;
    }
    
    @Override
    public String toString() {
        return "[" + time + "," + valueX + "," + valueY + "," + valueZ + "]";
    }
}
