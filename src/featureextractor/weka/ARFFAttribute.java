/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package featureextractor.weka;

/**
 *
 * @author Nicola Beghin
 */
public class ARFFAttribute {
    private String name;
    private String type;
    
    public ARFFAttribute(String name, String type) {
        this.name = name;
        this.type = type;
    }
    
    @Override
    public String toString() {
        return "@ATTRIBUTE "+name+" "+type;
    }

    public String getName() {
        return name;
    }
    
    
}