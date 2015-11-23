package d13n.domain.technology;

import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;
import org.neo4j.graphdb.Direction;

@NodeEntity
public class SubstanceShareInFuelMix {

    private double share;

    @RelatedTo(type = "SUBSTANCE", elementClass = Substance.class, direction = Direction.OUTGOING)
    private Substance substance;

    public double getShare() {
        return share;
    }

    public void setShare(double share) {
        this.share = share;
    }

    public Substance getSubstance() {
        return substance;
    }

    public void setSubstance(Substance substance) {
        this.substance = substance;
    }
    
    public String toString(){
    	return this.substance + ": "+ this.share;
    }
}
