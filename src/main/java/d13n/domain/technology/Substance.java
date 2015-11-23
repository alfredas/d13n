package d13n.domain.technology;

import org.springframework.data.neo4j.annotation.NodeEntity;

@NodeEntity
public class Substance {

    private String name;
    private double quality;
    private double energyDensity;
    private double co2Density;

    public String getName() {
        return name;
    }

    public void setName(String label) {
        this.name = label;
    }

    public double getQuality() {
        return quality;
    }

    public void setQuality(double quality) {
        this.quality = quality;
    }

    public double getEnergyDensity() {
        return energyDensity;
    }

    public void setEnergyDensity(double energyDensity) {
        this.energyDensity = energyDensity;
    }

    public double getCo2Density() {
        return co2Density;
    }

    public void setCo2Density(double co2Density) {
        this.co2Density = co2Density;
    }
    
    public String toString(){
    	return this.getName();
    }
}
