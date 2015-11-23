package d13n.role.market;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import agentspring.role.Role;
import agentspring.role.RoleComponent;
import d13n.domain.agent.EnergyConsumer;
import d13n.domain.agent.EnergyProducer;
import d13n.domain.contract.CashFlow;
import d13n.domain.gis.Zone;
import d13n.domain.market.Bid;
import d13n.domain.market.CO2Auction;
import d13n.domain.market.ClearingPoint;
import d13n.domain.market.CommodityMarket;
import d13n.domain.market.DecarbonizationMarket;
import d13n.domain.market.ElectricitySpotMarket;
import d13n.domain.market.Segment;
import d13n.repository.Reps;

/**
 * Creates and clears the {@link ElectricitySpotMarket} for one {@link Zone}.
 * {@link EnergyConsumer} submit bids to purchase electricity;
 * {@link EnergyProducer} submit ask offers to sell power. The market is divided
 * into {@link Segment}s and cleared for each segment.
 * 
 * @author <a href="mailto:E.J.L.Chappin@tudelft.nl">Emile Chappin</a>
 * 
 * @author <a href="mailto:A.Chmieliauskas@tudelft.nl">Alfredas
 *         Chmieliauskas</a>
 * 
 */
@RoleComponent
public class ProcessAcceptedBidsRole extends AbstractMarketRole<DecarbonizationMarket> implements Role<DecarbonizationMarket> {

    @Autowired
    private Reps reps;

    @Override
    public Reps getReps() {
        return reps;
    }


    @Transactional
    public void act(DecarbonizationMarket market) {

        logger.info("Process accepted bids to cash flow now");
        int cashFlowType = 0;
        boolean isCO2Traded = false;
        if (market instanceof CO2Auction) {
            cashFlowType = CashFlow.CO2AUCTION;
            isCO2Traded = true;
        } else if (market instanceof CommodityMarket) {
            cashFlowType = CashFlow.COMMODITY;
        } else {
            cashFlowType = CashFlow.UNCLASSIFIED;
        }

        // clear the market for each segment of the load duration curve
        Iterable<Bid> supplyBids = reps.bidRepository.findOffersForMarketForTime(market, getCurrentTick());
        Iterable<Bid> demandBids = reps.bidRepository.findBidsForMarketForTime(market, getCurrentTick());

        // Assuming only one price on this market for this time step and
        // iteration.
        ClearingPoint clearingPoint = reps.clearingPointRepository.findClearingPointForMarketAndTime(market, getCurrentTick());

        for (Bid bid : supplyBids) {
            if (bid.getStatus() >= Bid.PARTLY_ACCEPTED) {
            	reps.cashFlowRepository.createCashFlow(market, bid.getAgent(), bid.getAmount() * clearingPoint.getPrice(), cashFlowType,
                        getCurrentTick(), null);
                if (isCO2Traded) {
                    bid.getAgent().setCo2Allowances(bid.getAgent().getCo2Allowances() - bid.getAmount());
                }
            }
        }
        for (Bid bid : demandBids) {
            if (bid.getStatus() >= Bid.PARTLY_ACCEPTED) {
            	reps.cashFlowRepository.createCashFlow(bid.getAgent(), market, bid.getAmount() * clearingPoint.getPrice(), cashFlowType,
                        getCurrentTick(), null);
                if (isCO2Traded) {
                    bid.getAgent().setCo2Allowances(bid.getAgent().getCo2Allowances() + bid.getAmount());
                }
            }
        }
    }

}
