package d13n.domain.contract;


import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;
import org.neo4j.graphdb.Direction;

import d13n.domain.agent.DecarbonizationAgent;

@NodeEntity
public class Contract {

    @RelatedTo(type = "CONTRACT_FROM", elementClass = DecarbonizationAgent.class, direction = Direction.OUTGOING)
    private DecarbonizationAgent from;

    @RelatedTo(type = "CONTRACT_TO", elementClass = DecarbonizationAgent.class, direction = Direction.OUTGOING)
    private DecarbonizationAgent to;

    private double pricePerUnit;
    private boolean signed;
    private long start;
    private long finish;

    public double getPricePerUnit() {
        return pricePerUnit;
    }

    public void setPricePerUnit(double pricePerUnit) {
        this.pricePerUnit = pricePerUnit;
    }

    public boolean isSigned() {
        return signed;
    }

    public void setSigned(boolean signed) {
        this.signed = signed;
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getFinish() {
        return finish;
    }

    public void setFinish(long finish) {
        this.finish = finish;
    }

    public DecarbonizationAgent getFrom() {
        return from;
    }

    public void setFrom(DecarbonizationAgent from) {
        this.from = from;
    }

    public DecarbonizationAgent getTo() {
        return to;
    }

    public void setTo(DecarbonizationAgent to) {
        this.to = to;
    }
}
