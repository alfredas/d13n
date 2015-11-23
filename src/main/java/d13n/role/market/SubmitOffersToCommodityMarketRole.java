package d13n.role.market;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import agentspring.role.AbstractRole;
import agentspring.role.Role;
import agentspring.role.RoleComponent;
import d13n.domain.agent.CommoditySupplier;
import d13n.domain.market.Bid;
import d13n.domain.market.CommodityMarket;
import d13n.domain.market.DecarbonizationMarket;
import d13n.repository.Reps;

/**
 * {@link CommoditySupplier}s submit offers to the {@link CommodityMarket}s.
 * 
 * @author <a href="mailto:A.Chmieliauskas@tudelft.nl">Alfredas
 *         Chmieliauskas</a> @author <a
 *         href="mailto:E.J.L.Chappin@tudelft.nl">Emile Chappin</a>
 * 
 */
@RoleComponent
public class SubmitOffersToCommodityMarketRole extends AbstractRole<CommoditySupplier> implements Role<CommoditySupplier> {

    @Autowired
    private Reps reps;

    @Transactional
    public void act(CommoditySupplier supplier) {
        logger.info("Submitting offers to commodity market");

        DecarbonizationMarket market = reps.marketRepository.findMarketBySubstance(supplier.getSubstance());

        double price = supplier.getPriceOfCommodity().getValue(getCurrentTick());
        double amount = supplier.getAmountOfCommodity();

        Bid bid = reps.bidRepository.submitBidToMarket(market, supplier, getCurrentTick(), true, price, amount);
        logger.info("Submitted " + bid);
    }
}