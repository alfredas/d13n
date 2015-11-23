package d13n.role.operating;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import agentspring.role.Role;
import agentspring.role.RoleComponent;
import d13n.domain.agent.EnergyProducer;
import d13n.domain.agent.Government;
import d13n.domain.market.CO2Auction;
import d13n.domain.technology.PowerPlant;
import d13n.domain.technology.Substance;
import d13n.domain.technology.SubstanceShareInFuelMix;
import d13n.repository.Reps;
import d13n.role.AbstractEnergyProducerRole;

/**
 * Run the business. Buy supplies, pay interest, account profits
 * 
 * @author <a href="mailto:E.J.L.Chappin@tudelft.nl">Emile Chappin</a> @author
 *         <a href="mailto:A.Chmieliauskas@tudelft.nl">Alfredas
 *         Chmieliauskas</a>
 */
@RoleComponent
public class DetermineFuelMixRole extends AbstractEnergyProducerRole implements Role<EnergyProducer> {
	
	@Autowired
    Reps reps;

    public Reps getReps() {
        return reps;
    }
    
    @Transactional
    public void act(EnergyProducer producer) {

        logger.info("Determining operation mode of power plants");

        int ops = 0;
        for (@SuppressWarnings("unused")
        PowerPlant pp : reps.powerPlantRepository.findOperationalPowerPlantsByOwner(producer, getCurrentTick())) {
            ops++;
        }
        logger.info("number of operational pps: {}", ops);

        // get the co2 tax and market prices
        CO2Auction market = reps.genericRepository.findFirst(CO2Auction.class);
        double co2AuctionPrice = findLastKnownPriceOnMarket(market);
        Government government = reps.genericRepository.findFirst(Government.class);
        double co2TaxLevel = government.getCO2Tax(getCurrentTick());

        for (PowerPlant plant : reps.powerPlantRepository.findOperationalPowerPlantsByOwner(producer, getCurrentTick())) {
            logger.info("Found operational power plant {} ", plant.getTechnology());

            // Fuels
            Set<Substance> possibleFuels = plant.getTechnology().getFuels();
            Map<Substance, Double> substancePriceMap = new HashMap<Substance, Double>();

            for (Substance substance : possibleFuels) {
                substancePriceMap.put(substance, findLastKnownPriceForSubstance(substance));
            }
            Set<SubstanceShareInFuelMix> fuelMix = calculateFuelMix(substancePriceMap, plant.getTechnology().getMinimumFuelQuality(),
                    calculateEfficiency(plant), co2TaxLevel + co2AuctionPrice);
            plant.setFuelMix(fuelMix);
        }
    }

}