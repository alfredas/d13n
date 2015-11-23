package d13n.repository;


import org.springframework.stereotype.Repository;

import com.tinkerpop.blueprints.pgm.Vertex;
import com.tinkerpop.pipes.Pipe;

import d13n.domain.Rels;
import d13n.domain.gis.Zone;
import d13n.domain.market.DecarbonizationMarket;
import d13n.domain.market.ElectricitySpotMarket;
import d13n.domain.market.Segment;
import d13n.domain.market.SegmentLoad;
import d13n.domain.technology.Substance;


/**
 * The repository for markets.
 * @author ejlchappin
 *
 */
@Repository
public class MarketRepository extends AbstractRepository<DecarbonizationMarket> {

	/**
	 * Gives the electricity spot market for a specific zone
	 * @param zone the electricity market should be found for
	 * @return the found electricity spot market
	 */
    public ElectricitySpotMarket findElectricitySpotMarketForZone(Zone zone) {
        Pipe<Vertex, Vertex> markets = new LabeledEdgePipe(Rels.ZONE, LabeledEdgePipe.Step.IN_OUT);
        return (ElectricitySpotMarket) this.findAllByPipe(zone, markets).iterator().next();
    }

    public SegmentLoad findSegmentLoadForElectricitySpotMarketForZone(Zone zone, Segment segment) {
        ElectricitySpotMarket market = (ElectricitySpotMarket) findElectricitySpotMarketForZone(zone);
        for (SegmentLoad load : market.getLoadDurationCurve()) {
			if (load.getSegment().equals(segment)) {
				return load;
			}
        }
        return null;
    }

    /**
     * Gives the market for a specific substance
     * @param substance the substance the market should be found for
	 * @return the found market
	 */
    public DecarbonizationMarket findMarketBySubstance(Substance substance) {
        Pipe<Vertex, Vertex> market = new LabeledEdgePipe(Rels.TRADED_SUBSTANCE, LabeledEdgePipe.Step.IN_OUT);
        return findAllByPipe(substance, market).iterator().next();
    }

}
