package d13n.domain.market;

import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;
import org.neo4j.graphdb.Direction;

@NodeEntity
public class SegmentLoad {
	
	@RelatedTo(type = "SEGMENTLOAD_SEGMENT", elementClass = Segment.class, direction = Direction.OUTGOING)
	private Segment segment;
	
	private double baseLoad;

	public Segment getSegment() {
		return segment;
	}

	public void setSegment(Segment segment) {
		this.segment = segment;
	}


	public double getBaseLoad() {
		return baseLoad;
	}

	public void setBaseLoad(double baseLoad) {
		this.baseLoad = baseLoad;
	}

	@Override
    public String toString() {
    	return "segment: " + segment + " load: " + getBaseLoad();
    }

}
