package d13n.domain.agent;

import org.springframework.data.neo4j.annotation.NodeEntity;

import agentspring.agent.AbstractAgent;

@NodeEntity
public class DecarbonizationAgent extends AbstractAgent {

    private double cash;
    private double co2Allowances;
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getCash() {
        return cash;
    }

    public void setCash(double cash) {
        this.cash = cash;
    }

    public double getCo2Allowances() {
        return co2Allowances;
    }

    public void setCo2Allowances(double co2Allowances) {
        this.co2Allowances = co2Allowances;
    }

    @Override
    public String toString() {
        return getName();
    }
}
