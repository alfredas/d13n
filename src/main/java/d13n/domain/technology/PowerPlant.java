package d13n.domain.technology;

import java.util.Set;

import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;
import org.neo4j.graphdb.Direction;

import agentspring.trend.GeometricTrend;
import d13n.domain.agent.EnergyProducer;
import d13n.domain.contract.Loan;
import d13n.domain.market.Segment;

@NodeEntity
public class PowerPlant {

    @RelatedTo(type = "TECHNOLOGY", elementClass = PowerGeneratingTechnology.class, direction = Direction.OUTGOING)
    private PowerGeneratingTechnology technology;

    @RelatedTo(type = "FUEL_MIX", elementClass = SubstanceShareInFuelMix.class, direction = Direction.OUTGOING)
    private Set<SubstanceShareInFuelMix> fuelMix;

    @RelatedTo(type = "POWERPLANT_OWNER", elementClass = EnergyProducer.class, direction = Direction.OUTGOING)
    private EnergyProducer owner;

    @RelatedTo(type = "LOCATION", elementClass = PowerGridNode.class, direction = Direction.OUTGOING)
    private PowerGridNode location;

    @RelatedTo(type = "LOAN", elementClass = Loan.class, direction = Direction.OUTGOING)
    private Loan loan;

    private long dismantleTime;
    private long constructionStartTime;
    private long actualLeadtime;
    private long actualPermittime;
    private long actualLifetime;
    private double biomassCoCombiostionRate;
    private String label;
    private double actualInvestedCapital;
    private double actualEfficiency;


	public boolean isOperational(long currentTick) {

        double finishedConstruction = getConstructionStartTime() + calculateActualPermittime() + calculateActualLeadtime();

        if (finishedConstruction <= currentTick) {
            // finished construction

            if (getDismantleTime() == 0) {
                // No dismantletime set, therefore must be not yet dismantled.
                return true;
            } else if (getDismantleTime() > currentTick) {
                // Dismantle time set, but not yet reached
                return true;
            } else if (getDismantleTime() <= currentTick) {
                // Dismantle time passed so not operational
                return false;
            }
        }
        // Construction not yet finished.
        return false;
    }
	
	public boolean isExpectedToBeOperational(long time) {

        double finishedConstruction = getConstructionStartTime() + calculateActualPermittime() + calculateActualLeadtime();

        if (finishedConstruction <= time) {
            // finished construction

            if (finishedConstruction+getTechnology().getExpectedLifetime()>=time){
            	//Powerplant is not expected to be dismantled
            	return true;
            }
        }
        // Construction not yet finished.
        return false;
    }

    public boolean isInPipeline(long currentTick) {

        double finishedConstruction = getConstructionStartTime() + calculateActualPermittime() + calculateActualLeadtime();

        if (finishedConstruction > currentTick) {
            // finished construction

            if (getDismantleTime() == 0) {
                // No dismantletime set, therefore must be not yet dismantled.
                return true;
            } else if (getDismantleTime() > currentTick) {
                // Dismantle time set, but not yet reached
                return true;
            } else if (getDismantleTime() <= currentTick) {
                // Dismantle time passed so not operational
                return false;
            }
        }
        // Construction finished
        return false;
    }
    
    public double getAvailableCapacity(long currentTick, Segment segment, long numberOfSegments) {
		if (isOperational(currentTick)) {
			double factor = 1;
			if (segment != null) {//if no segment supplied, assume we want full capacity
				double segmentID = segment.getSegmentID();
				double min = getTechnology().getMinimumSegmentDependentAvailability();
				double max = getTechnology().getMaximumSegmentDependentAvailability();
				double segmentPortion = (numberOfSegments - segmentID)/(numberOfSegments-1); //start counting at 1.
				
				double range = max-min;
				
				factor = max - segmentPortion * range;
			}
			return getTechnology().getCapacity() * factor;
		} else {
			return 0;
		}
	}
    
    public double getExpectedAvailableCapacity(long futureTick, Segment segment, long numberOfSegments) {
		if (isExpectedToBeOperational(futureTick)) {
			double factor = 1;
			if (segment != null) {//if no segment supplied, assume we want full capacity
				double segmentID = segment.getSegmentID();
				double min = getTechnology().getMinimumSegmentDependentAvailability();
				double max = getTechnology().getMaximumSegmentDependentAvailability();
				double segmentPortion = (numberOfSegments - segmentID)/(numberOfSegments-1); //start counting at 1.
				
				double range = max-min;
				
				factor = max - segmentPortion * range;
			}
			return getTechnology().getCapacity() * factor;
		} else {
			return 0;
		}
	}
    
    public double getAvailableCapacity(long currentTick) {
        if (isOperational(currentTick)) {
            return getTechnology().getCapacity();
        } else {
            return 0;
        }
    }
    
    public double getExpectedAvailableCapacity(long futureTick) {
        if (isExpectedToBeOperational(futureTick)) {
            return getTechnology().getCapacity();
        } else {
            return 0;
        }
    }

    public long calculateActualLeadtime() {
        long actual;
        actual = getActualLeadtime();
        if (actual <= 0) {
            actual = getTechnology().getExpectedLeadtime();
        }
        return actual;
    }

    public long calculateActualPermittime() {
        long actual;
        actual = getActualPermittime();
        if (actual <= 0) {
            actual = getTechnology().getExpectedPermittime();
        }
        return actual;
    }

    public long calculateActualLifetime() {
        long actual;
        actual = getActualLifetime();
        if (actual <= 0) {
            actual = getTechnology().getExpectedLifetime();
        }
        return actual;
    }

    /**
     * Determines whether a plant is still in its technical lifetime. The end of
     * the technical lifetime is determined by the construction start time, the
     * permit time, the lead time and the actual lifetime.
     * 
     * @param currentTick
     * @return whether the plant is still in its technical lifetime.
     */
    public boolean isWithingTechnicalLifetime(long currentTick) {
        long endOfTechnicalLifetime = getConstructionStartTime() + calculateActualPermittime() + calculateActualLeadtime()
                + calculateActualLifetime();
        if (endOfTechnicalLifetime <= currentTick) {
            return false;
        }
        return true;
    }
    
    public boolean isWithinExpectedTechnicalLifetime(long futureTick) {
        long endOfTechnicalLifetime = getConstructionStartTime() + calculateActualPermittime() + calculateActualLeadtime()
                + getTechnology().getExpectedLifetime();
        if (endOfTechnicalLifetime <= futureTick) {
            return false;
        }
        return true;
    }

    public double getBiomassCoCombiostionRate() {
        return biomassCoCombiostionRate;
    }

    public void setBiomassCoCombiostionRate(double biomassCoCombiostionRate) {
        this.biomassCoCombiostionRate = biomassCoCombiostionRate;
    }

    public PowerGridNode getLocation() {
        return location;
    }

    public void setLocation(PowerGridNode location) {
        this.location = location;
    }

    public PowerGeneratingTechnology getTechnology() {
        return technology;
    }

    public void setTechnology(PowerGeneratingTechnology technology) {
        this.technology = technology;
    }

    public long getConstructionStartTime() {
        return constructionStartTime;
    }

    public void setConstructionStartTime(long constructionStartTime) {
        this.constructionStartTime = constructionStartTime;
    }

    public EnergyProducer getOwner() {
        return owner;
    }

    public void setOwner(EnergyProducer owner) {
        this.owner = owner;
    }

    public void setActualLifetime(long actualLifetime) {
        this.actualLifetime = actualLifetime;
    }

    public long getActualLifetime() {
        return actualLifetime;
    }

    public void setActualPermittime(long actualPermittime) {
        this.actualPermittime = actualPermittime;
    }

    public long getActualPermittime() {
        return actualPermittime;
    }

    public void setActualLeadtime(long actualLeadtime) {
        this.actualLeadtime = actualLeadtime;
    }

    public long getActualLeadtime() {
        return actualLeadtime;
    }

    public long getDismantleTime() {
        return dismantleTime;
    }

    public void setDismantleTime(long dismantleTime) {
        this.dismantleTime = dismantleTime;
    }

    public String getName() {
        return label;
    }

    public void setName(String label) {
        this.label = label;
    }
    
    

    public double getActualInvestedCapital() {
		return actualInvestedCapital;
	}

	public void setActualInvestedCapital(double actualInvestedCapital) {
		this.actualInvestedCapital = actualInvestedCapital;
	}

	public Set<SubstanceShareInFuelMix> getFuelMix() {
        return fuelMix;
    }

    public void setFuelMix(Set<SubstanceShareInFuelMix> fuelMix) {
        this.fuelMix = fuelMix;
    }

    public Loan getLoan() {
        return loan;
    }

    public void setLoan(Loan loan) {
        this.loan = loan;
    }
    

    public double getActualEfficiency() {
		return actualEfficiency;
	}

	public void setActualEfficiency(double actualEfficiency) {
		this.actualEfficiency = actualEfficiency;
	}

    public String toString() {
        return this.getName() + " power plant";
    }
    
    
    /**
     * Sets the actual capital that is needed to build the power plant. It considers the exogenous modifier
     * and automatically adjusts for the actual building and permit time.
     * @param timeOfPermitorBuildingStart
     */
    public void calculateAndSetActualInvestedCapital(long timeOfPermitorBuildingStart){
    	double invNorm = this.getTechnology().getBaseInvestmentCost();
    	double modifierExo = this.getTechnology().getInvestmentCostModifierExogenous();
    	//Adjust the exogenous modifier to the given time.
    	GeometricTrend trendExo = new GeometricTrend();
        trendExo.setGrowthRate(modifierExo);
        trendExo.setStart(1);
        modifierExo = trendExo.getValue(timeOfPermitorBuildingStart+getActualLeadtime()+getActualPermittime());
    	
    	this.actualInvestedCapital = invNorm * modifierExo;
    }

}