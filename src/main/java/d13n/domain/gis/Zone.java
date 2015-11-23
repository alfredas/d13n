package d13n.domain.gis;

import org.springframework.data.neo4j.annotation.NodeEntity;

@NodeEntity
public class Zone {

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String toString() {
        return "Zone " + name;
    }

}