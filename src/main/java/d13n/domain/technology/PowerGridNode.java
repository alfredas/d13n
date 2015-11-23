package d13n.domain.technology;


import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;
import org.neo4j.graphdb.Direction;

import d13n.domain.gis.Zone;

@NodeEntity
public class PowerGridNode extends AbstractTechnology {

    @RelatedTo(type = "REGION", elementClass = Zone.class, direction = Direction.OUTGOING)
    private Zone zone;

    public void setZone(Zone zone) {
        this.zone = zone;
    }

    public Zone getZone() {
        return zone;
    }
    
    

}