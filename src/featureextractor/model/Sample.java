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

    final private long time;
    final private double valueX;
    final private double valueY;
    final private double valueZ;
    final private double valueV;
    final private double valueDelta;
    final private String action;
    final private int trunk;
    final private int step;
    final private String mode;

    public Sample(long time, double valueX, double valueY, double valueZ, int trunk, String action, int step, String mode) {
        this.time = time;
        this.valueX = valueX;
        this.valueY = valueY;
        this.valueZ = valueZ;
        this.trunk = trunk;
        this.action = action;
        this.valueV = Sample.calculateV(valueX, valueY, valueZ);
        this.valueDelta = Sample.calculateDelta(this.valueV);
        this.mode = mode;
        this.step = (step > 0 ? step : 0);
    }

    public String getMode() {
        return mode;
    }

    public static double calculateV(double valueX, double valueY, double valueZ) {
        return Math.sqrt(Math.pow(valueX, 2) + Math.pow(valueY, 2) + Math.pow(valueZ, 2));
    }

    public static double calculateDelta(double valueV) {
        return Math.abs(valueV - Math.pow(9.81, 2));
    }

    public long getTime() {
        return time;
    }

    public int getStep() {
        return step;
    }

    public String getAction() {
        return action;
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

    public double getValueV() {
        return valueV;
    }

    public double getValueDelta() {
        return valueDelta;
    }

    public int getTrunk() {
        return trunk;
    }

    @Override
    public String toString() {
        return "[" + time + "," + valueX + "," + valueY + "," + valueZ + "]\t(|V|=" + valueV + ")";
    }
}
