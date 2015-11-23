package d13n.repository;

import org.springframework.stereotype.Repository;

import com.tinkerpop.blueprints.pgm.Vertex;
import com.tinkerpop.pipes.Pipe;

import d13n.domain.Rels;
import d13n.domain.market.ElectricitySpotMarket;
import d13n.domain.market.Segment;
import d13n.domain.market.SegmentLoad;

/**
 * Repository for segment loads
 * @author ejlchappin
 *
 */
@Repository
public class SegmentLoadRepository extends AbstractRepository<SegmentLoad> {

	/**
	 * Finds the segment loads for a certain segment.
	 * @param segment the segment to find the load for
	 * @return the segment load
	 */
    public Iterable<SegmentLoad> findSegmentLoadBySegment(Segment segment) {
        Pipe<Vertex, Vertex> sl = new LabeledEdgePipe(Rels.SEGMENTLOAD_SEGMENT, LabeledEdgePipe.Step.IN_OUT);
        return this.findAllByPipe(segment, sl);
    }

    /**
     * Finds the segment load for a certain segment and market
     * @param segment the segment to find the load for
     * @param market the market to find the load for
     * @return
     */
    public SegmentLoad findSegmentLoadBySegmentAndMarket(Segment segment, ElectricitySpotMarket market) {
        Pipe<Vertex, Vertex> sl = new LabeledEdgePipe(Rels.SEGMENTLOAD_SEGMENT, LabeledEdgePipe.Step.IN_OUT);
        //TODO make this a pipe
        for (SegmentLoad load : this.findAllByPipe(segment, sl)) {
            if (market.getLoadDurationCurve().contains(load)) {
                return load;
            }
        }
        return null;
    }

}
