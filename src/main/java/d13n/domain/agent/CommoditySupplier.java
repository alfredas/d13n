package d13n.domain.agent;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import agentspring.agent.Agent;
import d13n.domain.technology.Substance;
import d13n.trend.TriangularTrend;

@NodeEntity
public class CommoditySupplier extends DecarbonizationAgent implements Agent {

    @RelatedTo(type = "SUBSTANCE", elementClass = Substance.class, direction = Direction.OUTGOING)
    private Substance substance;

    @RelatedTo(type = "TREND", elementClass = TriangularTrend.class, direction = Direction.OUTGOING)
    private TriangularTrend priceOfCommodity;

    public Substance getSubstance() {
        return substance;
    }

    public void setSubstance(Substance substance) {
        this.substance = substance;
    }

    public TriangularTrend getPriceOfCommodity() {
        return priceOfCommodity;
    }

    public void setPriceOfCommodity(TriangularTrend priceOfCommodity) {
        this.priceOfCommodity = priceOfCommodity;
    }

    public double getAmountOfCommodity() {
        return Double.MAX_VALUE;
    }
}
