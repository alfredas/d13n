package d13n.domain.market;

import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;
import org.neo4j.graphdb.Direction;

@NodeEntity
public class SegmentClearingPoint extends ClearingPoint {

    @RelatedTo(type = "SEGMENT_POINT", elementClass = Segment.class, direction = Direction.OUTGOING)
    private Segment segment;

    public Segment getSegment() {
        return segment;
    }

    public void setSegment(Segment segment) {
        this.segment = segment;
    }
}
