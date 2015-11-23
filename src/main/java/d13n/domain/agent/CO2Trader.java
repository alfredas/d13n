package d13n.domain.agent;

import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import agentspring.agent.Agent;
import d13n.domain.market.CurveSection;

@NodeEntity
public class CO2Trader extends DecarbonizationAgent implements Agent {

    @RelatedTo(type = "CURVE_SECTION", elementClass = CurveSection.class, direction = Direction.OUTGOING)
    private Set<CurveSection> demandCurve;

    public Set<CurveSection> getDemandCurve() {
        return demandCurve;
    }

    public void setDemandCurve(Set<CurveSection> demandCurve) {
        this.demandCurve = demandCurve;
    }

}