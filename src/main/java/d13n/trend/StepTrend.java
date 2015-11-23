package d13n.trend;

import org.springframework.data.neo4j.annotation.NodeEntity;

import agentspring.simulation.SimulationParameter;
import agentspring.trend.Trend;

@NodeEntity
public class StepTrend implements Trend {

    @SimulationParameter(label = "Time steps per step", from = 0, to = 50)
    private double duration;

    @SimulationParameter(label = "Increment per step")
    private double increment;

    private double minValue;

    private double start;

    public double getDuration() {
        return duration;
    }

    public double getMinValue() {
        return minValue;
    }

    public void setMinValue(double minValue) {
        this.minValue = minValue;
    }

    public void setDuration(double duration) {
        this.duration = duration;
    }

    public double getIncrement() {
        return increment;
    }

    public void setIncrement(double increment) {
        this.increment = increment;
    }

    public double getValue(long time) {
        return Math.max(minValue, getStart() + Math.floor(time / duration) * increment);
    }

    public double getStart() {
        return start;
    }

    public void setStart(double start) {
        this.start = start;
    }

}
