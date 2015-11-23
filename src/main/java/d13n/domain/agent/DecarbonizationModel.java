package d13n.domain.agent;

import org.springframework.data.neo4j.annotation.NodeEntity;

import agentspring.agent.AbstractAgent;
import agentspring.agent.Agent;
import agentspring.simulation.SimulationParameter;

@NodeEntity
public class DecarbonizationModel extends AbstractAgent implements Agent {
    private int iteration;
    private double absoluteStabilityCriterion;
    private double relativeStabilityCriterion;
    private double relativeBounceCriterion;

    private double iterationSpeedFactor;
    private double iterationSpeedCriterion;
    private double capDeviationCriterion;
    private String name;

    @SimulationParameter(label = "Simulation Length", from = 0, to = 75)
    private double simulationLength;

    @SimulationParameter(label = "CO2 Trading")
    private boolean co2TradingImplemented;

    @SimulationParameter(label = "Long Term Contracts")
    private boolean longTermContractsImplemented;

    public double getIterationSpeedFactor() {
        return iterationSpeedFactor;
    }

    public void setIterationSpeedFactor(double iterationSpeedFactor) {
        this.iterationSpeedFactor = iterationSpeedFactor;
    }

    public double getIterationSpeedCriterion() {
        return iterationSpeedCriterion;
    }

    public void setIterationSpeedCriterion(double iterationSpeedCriterion) {
        this.iterationSpeedCriterion = iterationSpeedCriterion;
    }

    public double getCapDeviationCriterion() {
        return capDeviationCriterion;
    }

    public void setCapDeviationCriterion(double capDeviationCriterion) {
        this.capDeviationCriterion = capDeviationCriterion;
    }

    public double getAbsoluteStabilityCriterion() {
        return absoluteStabilityCriterion;
    }

    public void setAbsoluteStabilityCriterion(double absoluteStabilityCriterion) {
        this.absoluteStabilityCriterion = absoluteStabilityCriterion;
    }

    public double getRelativeStabilityCriterion() {
        return relativeStabilityCriterion;
    }

    public void setRelativeStabilityCriterion(double relativeStabilityCriterion) {
        this.relativeStabilityCriterion = relativeStabilityCriterion;
    }

    public int getIteration() {
        return iteration;
    }

    public void setIteration(int iteration) {
        this.iteration = iteration;
    }

    public boolean isCo2TradingImplemented() {
        return co2TradingImplemented;
    }

    public boolean isLongTermContractsImplemented() {
        return longTermContractsImplemented;
    }

    public void setLongTermContractsImplemented(boolean longTermContractsImplemented) {
        this.longTermContractsImplemented = longTermContractsImplemented;
    }

    public void setCo2TradingImplemented(boolean co2Market) {
        this.co2TradingImplemented = co2Market;
    }

    public double getRelativeBounceCriterion() {
        return relativeBounceCriterion;
    }

    public void setRelativeBounceCriterion(double relativeBounceCriterion) {
        this.relativeBounceCriterion = relativeBounceCriterion;
    }

    public double getSimulationLength() {
        return simulationLength;
    }

    public void setSimulationLength(double simulationLength) {
        this.simulationLength = simulationLength;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
