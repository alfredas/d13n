package d13n.repository;

import java.util.Iterator;
import java.util.List;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.tinkerpop.blueprints.pgm.Vertex;
import com.tinkerpop.gremlin.pipes.filter.PropertyFilterPipe;
import com.tinkerpop.pipes.Pipe;
import com.tinkerpop.pipes.filter.FilterPipe;
import com.tinkerpop.pipes.util.Pipeline;
import d13n.domain.Rels;
import d13n.domain.market.ClearingPoint;
import d13n.domain.market.DecarbonizationMarket;
import d13n.domain.market.Segment;
import d13n.domain.market.SegmentClearingPoint;
import d13n.util.Utils;

/**
 * Repository for {ClearingPoint}s
 * @author ejlchappin
 *
 */
@Repository
public class ClearingPointRepository extends AbstractRepository<ClearingPoint> {

    public ClearingPoint findClearingPointForSegmentAndTime(Segment segment, long time) {
        Iterator<ClearingPoint> i = findClearingPointsForSegmentAndTime(segment, time).iterator();
        if (i.hasNext()) {
            return i.next();
        }
        return null;
    }

      public Iterable<ClearingPoint> findClearingPointsForSegmentAndTime(Segment segment, long time) {
        Pipe<Vertex, Vertex> clearingPointsPipe2 = new LabeledEdgePipe(Rels.SEGMENT_POINT, LabeledEdgePipe.Step.IN_OUT);
        // filter by time
        Pipe<Vertex, Vertex> timeFilter = new PropertyFilterPipe<Vertex, Long>("time", time,
        		FilterPipe.Filter.EQUAL);
        Pipeline<Vertex, Vertex> clearingPoint = new Pipeline<Vertex, Vertex>(clearingPointsPipe2, timeFilter);
        return findAllByPipe(segment, clearingPoint);
    }


    public ClearingPoint findClearingPointForMarketAndTime(DecarbonizationMarket market, long time) {

        Iterator<ClearingPoint> i = findClearingPointsForMarketAndTime(market, time).iterator();
        if (i.hasNext()) {
            return i.next();
        }
        return null;
    }

    public Iterable<ClearingPoint> findClearingPointsForMarketAndTime(DecarbonizationMarket market, long time) {
        // TODO: test this
        Pipe<Vertex, Vertex> clearingPoints = new LabeledEdgePipe(Rels.MARKET_POINT, LabeledEdgePipe.Step.IN_OUT);
        // filter by time
        Pipe<Vertex, Vertex> timeFilter = new PropertyFilterPipe<Vertex, Long>("time", time,
                FilterPipe.Filter.EQUAL);
        Pipeline<Vertex, Vertex> clearingPoint = new Pipeline<Vertex, Vertex>(clearingPoints, timeFilter);

        return findAllByPipe(market, clearingPoint);
    }


  
    @Transactional
    public void setClearingPointForMarket(DecarbonizationMarket market, ClearingPoint point) {
        point.setAbstractMarket(market);
    }

    @Transactional
    public ClearingPoint createOrUpdateClearingPoint(DecarbonizationMarket abstractMarket, double price, double volume, long time,
            int iteration) {
    	ClearingPoint point = null;
    	if(findClearingPointsForMarketAndTime(abstractMarket,time).iterator().hasNext()){
    		point = findClearingPointsForMarketAndTime(abstractMarket,time).iterator().next();
    	} else{ 
    		point = new ClearingPoint().persist();	
    	}
        point.setAbstractMarket(abstractMarket);
        point.setIteration(iteration);
        point.setPrice(price);
        point.setTime(time);
        point.setVolume(volume);
        return point;
    }

    @Transactional
    public SegmentClearingPoint createOrUpdateSegmentClearingPoint(Segment segment, DecarbonizationMarket abstractMarket,
            double price, double volume, long time, int iteration) {
    	SegmentClearingPoint point = null;
    	//TODO make this a pipe
    	List<SegmentClearingPoint> points = Utils.asCastedList(findClearingPointsForMarketAndTime(abstractMarket,time));
    	for(SegmentClearingPoint onepoint : points){
    		if(onepoint.getSegment().equals(segment)){
    			point=onepoint;
    		}
    	}
    	if(point==null){
    		point = new SegmentClearingPoint().persist();	
    	}
        point.setAbstractMarket(abstractMarket);
        point.setIteration(iteration);
        point.setPrice(price);
        point.setTime(time);
        point.setVolume(volume);
        point.setSegment(segment);
        return point;
    }

}
