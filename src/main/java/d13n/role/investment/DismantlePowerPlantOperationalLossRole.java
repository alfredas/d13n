package d13n.role.investment;

import org.springframework.beans.factory.annotation.Autowired;

import agentspring.role.Role;
import agentspring.role.RoleComponent;
import d13n.domain.agent.EnergyProducer;
import d13n.domain.technology.PowerPlant;
import d13n.repository.Reps;
import d13n.role.AbstractEnergyProducerRole;

/**
 * {@link EnergyProducer}s dismantle {@link PowerPlant}s that are out of merit
 * 
 * @author <a href="mailto:E.J.L.Chappin@tudelft.nl">Emile Chappin</a> @author
 *         <a href="mailto:A.Chmieliauskas@tudelft.nl">Alfredas
 *         Chmieliauskas</a>
 * 
 */
@RoleComponent
public class DismantlePowerPlantOperationalLossRole extends AbstractEnergyProducerRole implements Role<EnergyProducer> {

    @Autowired
    Reps reps;
    
    public Reps getReps() {
		return reps;
	}

	public void act(EnergyProducer producer) {

        logger.info("Dismantling plants if out of merit");

        // dis-mantle plants when passed technical lifetime.
        for (PowerPlant plant : reps.powerPlantRepository.findOperationalPowerPlantsByOwner(producer, getCurrentTick())) {
            long horizon = producer.getPastTimeHorizon();

            double requiredProfit = producer.getDismantlingRequiredOperatingProfit();
            if (calculateAveragePastOperatingProfit(plant, horizon) < requiredProfit) {
                logger.info("Dismantling power plant because it has had an operating loss (incl O&M cost) on average in the last "
                        + horizon + " years: " + plant);

                getReps().powerPlantRepository.dismantlePowerPlant(plant, getCurrentTick());

            }
        }
    }

}
