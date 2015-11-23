package d13n.domain.market;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import d13n.domain.agent.DecarbonizationAgent;
import d13n.domain.gis.Zone;
import d13n.domain.technology.Substance;

@NodeEntity
public abstract class DecarbonizationMarket extends DecarbonizationAgent {

    @RelatedTo(type = "TRADED_SUBSTANCE", elementClass = Substance.class, direction = Direction.OUTGOING)
    private Substance substance;

    @RelatedTo(type = "ZONE", elementClass = Zone.class, direction = Direction.OUTGOING)
    private Zone zone;

    private boolean auction;
    private double referencePrice;
    private String name;

    public Substance getSubstance() {
        return substance;
    }

    public void setSubstance(Substance substance) {
        this.substance = substance;
    }

    public void setZone(Zone location) {
        this.zone = location;
    }

    public Zone getZone() {
        return zone;
    }

    public String toString() {
        if (this.getName() == "") {
            if (auction) {
                return this.getSubstance().getName() + " auction";
            }
            return this.getSubstance().getName() + " market";
        } else {
            return this.getName();
        }
    }

    public boolean isAuction() {
        return auction;
    }

    public void setAuction(boolean auction) {
        this.auction = auction;
    }

    public double getReferencePrice() {
        return referencePrice;
    }

    public void setReferencePrice(double referencePrice) {
        this.referencePrice = referencePrice;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
