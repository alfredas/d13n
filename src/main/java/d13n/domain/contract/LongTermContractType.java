/**
 * 
 */
package d13n.domain.contract;

import java.util.Set;

import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;
import org.neo4j.graphdb.Direction;

import d13n.domain.market.Segment;

/**
 * @author ejlchappin
 *
 *
 */
@NodeEntity
public class LongTermContractType {

	@RelatedTo(type = "LONGTERMCONTRACTTYPE_SEGMENTS", elementClass = Segment.class, direction = Direction.OUTGOING)
	private Set<Segment> segments;
	
	private String label;
	
	public String getName() {
		return label;
	}

	public void setName(String label) {
		this.label = label;
	}

	public Set<Segment> getSegments() {
		return segments;
	}

	public void setSegments(Set<Segment> segments) {
		this.segments = segments;
	}
    public String toString() {
        return "LTC type " + label;
    }

}
