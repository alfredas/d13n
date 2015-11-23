package d13n.domain.technology;

import java.util.Set;

import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;
import org.neo4j.graphdb.Direction;

@NodeEntity
public class Interconnector extends AbstractTechnology {

    @RelatedTo(type = "INTERCONNECTIONS", elementClass = PowerGridNode.class, direction = Direction.OUTGOING) //TODO: Limit the set to the size of two.
    private Set<PowerGridNode> connections;

    //@SimulationParameter(description = "Interconnector capacity MW", label = "Interconnector capacity MW", from = 0, to = 300000, step = 10000)
    private double capacity;

    public Set<PowerGridNode> getConnections() {
        return connections;
    }

    public void setConnections(Set<PowerGridNode> connections) {
        this.connections = connections;
    }

    public double getCapacity() {
        return capacity;
    }

    public void setCapacity(double capacity) {
        this.capacity = capacity;
    }

}