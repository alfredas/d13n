package d13n.domain.market;


import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;
import org.neo4j.graphdb.Direction;

import d13n.domain.technology.PowerPlant;

@NodeEntity
public class CO2Bid extends Bid {

    @RelatedTo(type = "SEGMENT_CO2", elementClass = Segment.class, direction = Direction.OUTGOING)
    private Segment segment;

    @RelatedTo(type = "POWERPLANT_CO2", elementClass = PowerPlant.class, direction = Direction.OUTGOING)
    private PowerPlant powerPlant;

    public Segment getSegment() {
        return segment;
    }

    public void setSegment(Segment segment) {
        this.segment = segment;
    }

    public void setPowerPlant(PowerPlant powerPlant) {
        this.powerPlant = powerPlant;
    }

    public PowerPlant getPowerPlant() {
        return powerPlant;
    }

    @Override
    public String toString() {
        String str = super.toString();
        return str + " powerplant: " + getPowerPlant() + " segment: " + getSegment();
    }

}