package d13n.domain.agent;

import org.springframework.data.neo4j.annotation.NodeEntity;

import agentspring.agent.Agent;
import agentspring.simulation.SimulationParameter;

@NodeEntity
public class EnergyConsumer extends DecarbonizationAgent implements Agent {

    @SimulationParameter(label = "Maximum coverage fraction of long-term contracts", from = 0, to = 0.25)
    private double ltcMaximumCoverageFraction;

    @SimulationParameter(label = "Contract duration preference factor", from = 0, to = 1)
    private double contractDurationPreferenceFactor;

    @SimulationParameter(label = "Contract willingness to pay factor", from = 1, to = 2)
    private double contractWillingnessToPayFactor;

    public double getLtcMaximumCoverageFraction() {
        return ltcMaximumCoverageFraction;
    }

    public void setLtcMaximumCoverageFraction(double ltcMaximumCoverageFraction) {
        this.ltcMaximumCoverageFraction = ltcMaximumCoverageFraction;
    }

    public double getContractDurationPreferenceFactor() {
        return contractDurationPreferenceFactor;
    }

    public void setContractDurationPreferenceFactor(double contractDurationPreferenceFactor) {
        this.contractDurationPreferenceFactor = contractDurationPreferenceFactor;
    }

    public double getContractWillingnessToPayFactor() {
        return contractWillingnessToPayFactor;
    }

    public void setContractWillingnessToPayFactor(double contractWillingnessToPayFactor) {
        this.contractWillingnessToPayFactor = contractWillingnessToPayFactor;
    }
}
