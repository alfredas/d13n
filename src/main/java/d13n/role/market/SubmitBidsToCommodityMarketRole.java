package d13n.role.market;

import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import agentspring.role.Role;
import agentspring.role.RoleComponent;
import d13n.domain.agent.EnergyProducer;
import d13n.domain.market.Bid;
import d13n.domain.market.CommodityMarket;
import d13n.domain.technology.PowerPlant;
import d13n.domain.technology.Substance;
import d13n.domain.technology.SubstanceShareInFuelMix;
import d13n.repository.Reps;
import d13n.role.AbstractEnergyProducerRole;

/**
 * {@link EnergyProducer}s submit bids to the {@link CommodityMarket}. They buy
 * fuel needed to fuel their {@link PowerPlant}s. Interesting twist with
 * co-combustion and multiple fuels
 * 
 * @author <a href="mailto:E.J.L.Chappin@tudelft.nl">Emile Chappin</a> @author
 *         <a href="mailto:A.Chmieliauskas@tudelft.nl">Alfredas
 *         Chmieliauskas</a>
 * 
 */
@RoleComponent
public class SubmitBidsToCommodityMarketRole extends AbstractEnergyProducerRole
		implements Role<EnergyProducer> {

	@Autowired
    Reps reps;

    @Override
    public Reps getReps() {
        return reps;
    }

	@Transactional
	public void act(EnergyProducer producer) {

		logger.info("Purchasing commodities");

		HashMap<Substance, Double> fuelAmounts = new HashMap<Substance, Double>();

		for (PowerPlant plant : reps.powerPlantRepository
				.findOperationalPowerPlantsByOwner(producer, getCurrentTick())) {

			double totalSupply = calculateElectricityOutputAtTime(plant,
					getCurrentTick());

			for (SubstanceShareInFuelMix share : plant.getFuelMix()) {

				double amount = share.getShare() * totalSupply;
				Substance substance = share.getSubstance();

				// already in? Than add to total
				if (fuelAmounts.containsKey(substance)) {
					amount += fuelAmounts.get(substance);
				}
				fuelAmounts.put(substance, amount);
			}
		}

		for (Substance substance : fuelAmounts.keySet()) {
			// find the totals and the right market. Place one bid for each
			// substance (fuel)
			Bid bid = reps.bidRepository.submitBidToMarket(
					reps.marketRepository.findMarketBySubstance(substance),
					producer, getCurrentTick(), false, Double.MAX_VALUE,
					fuelAmounts.get(substance));
			logger.info("Submited bid " + bid);
		}
	}

}
