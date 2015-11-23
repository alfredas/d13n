package d13n.domain.market;


import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;
import org.neo4j.graphdb.Direction;

import d13n.domain.agent.DecarbonizationAgent;
import d13n.domain.technology.PowerPlant;

/**
 * The Electricity Long Term Bid is the reflecting commitment
 * to meet long term contracts. Before being able to make a(n updated) 
 * valid bid on the spot market, we need to define what part of 
 * capacity of a certain power plant in a certain segment is covered 
 * by long-term contracts. This needs to be updated every iteration in the
 * market clearing algorithm. 
 * @author ejlchappin
 *
 */
@NodeEntity
public class PowerPlantDispatchPlan {

	@RelatedTo(type = "SELLING_AGENT", elementClass = DecarbonizationAgent.class, direction = Direction.INCOMING)
    private DecarbonizationAgent sellingAgent;

	@RelatedTo(type = "BUYING_AGENT_SPOT", elementClass = DecarbonizationAgent.class, direction = Direction.INCOMING)  
    private DecarbonizationAgent buyingAgentSpot;                                                                     
	
	@RelatedTo(type = "SEGMENT_DISPATCHPLAN", elementClass = Segment.class, direction = Direction.OUTGOING)
    private Segment segment;

    @RelatedTo(type = "POWERPLANT_DISPATCHPLAN", elementClass = PowerPlant.class, direction = Direction.OUTGOING)
    private PowerPlant powerPlant;

    private double capacityLongTermContract;
    private double capacitySpotMarket;
    private double marginalCostExclCO2;

	private long time;
       
    public DecarbonizationAgent getSellingAgent() {
		return sellingAgent;
	}

	public void setSellingAgent(DecarbonizationAgent sellingAgent) {
		this.sellingAgent = sellingAgent;
	}

	public Segment getSegment() {
		return segment;
	}

	public void setSegment(Segment segment) {
		this.segment = segment;
	}

	public PowerPlant getPowerPlant() {
		return powerPlant;
	}

	public void setPowerPlant(PowerPlant powerPlant) {
		this.powerPlant = powerPlant;
	}

	public double getCapacityLongTermContract() {
		return capacityLongTermContract;
	}

	public void setCapacityLongTermContract(double capacityLongTermContract) {
		this.capacityLongTermContract = capacityLongTermContract;
	}

	public double getCapacitySpotMarket() {
		return capacitySpotMarket;
	}

	public void setCapacitySpotMarket(double capacitySpotMarket) {
		this.capacitySpotMarket = capacitySpotMarket;
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

    public double getMarginalCostExclCO2() {
		return marginalCostExclCO2;
	}

	public void setMarginalCostExclCO2(double marginalCostExclCO2) {
		this.marginalCostExclCO2 = marginalCostExclCO2;
	}
	
	public DecarbonizationAgent getBuyingAgentSpot() {
		return buyingAgentSpot;
	}

	public void setBuyingAgentSpot(DecarbonizationAgent buyingAgentSpot) {
		this.buyingAgentSpot = buyingAgentSpot;
	}

	@Override
    public String toString() {
        return  "for " + getSellingAgent() + " power plant: " + getPowerPlant() + " in segment "  + segment + " plans to sell long term: " + getCapacityLongTermContract() + " plans to sell capacity spot: " + getCapacitySpotMarket();
    }
}