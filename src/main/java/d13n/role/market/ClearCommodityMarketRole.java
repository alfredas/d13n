package d13n.role.market;

import org.springframework.beans.factory.annotation.Autowired;

import agentspring.role.Role;
import agentspring.role.RoleComponent;
import d13n.domain.agent.CommoditySupplier;
import d13n.domain.agent.EnergyProducer;
import d13n.domain.market.Bid;
import d13n.domain.market.ClearingPoint;
import d13n.domain.market.CommodityMarket;
import d13n.repository.Reps;

/**
 * Creates and clears the {@link CommodityMarket}. {@link EnergyProducer} submit
 * bids to purchase commodities; {@link CommoditySupplier} submits ask offers to
 * sell commodities rights
 * 
 * @author <a href="mailto:E.J.L.Chappin@tudelft.nl">Emile Chappin</a>
 * @author <a href="mailto:A.Chmieliauskas@tudelft.nl">Alfredas
 *         Chmieliauskas</a>
 * 
 */
@RoleComponent
public class ClearCommodityMarketRole extends AbstractMarketRole<CommodityMarket> implements Role<CommodityMarket> {

	  @Autowired
	    Reps reps;


    public void act(CommodityMarket market) {

        logger.info("Clearing the commodity market for {}", market.getSubstance());

        // clear the market
        Iterable<Bid> demandBids = reps.bidRepository.findBidsForMarketForTime(market, getCurrentTick());
        Iterable<Bid> supplyBids = reps.bidRepository.findOffersForMarketForTime(market, getCurrentTick());

        ClearingPoint clearingPoint = calculateClearingPoint(market, supplyBids, demandBids, getCurrentTick());

        if (clearingPoint != null) {
        	reps.clearingPointRepository.setClearingPointForMarket(market, clearingPoint);
            logger.info("Clearing: price " + clearingPoint.getPrice() + " / volume " + clearingPoint.getVolume());
        } else {
            logger.warn("{} did not clear!", market);
        }
    }

    @Override
    public Reps getReps() {
        return reps;
    }

}
