package d13n.role.investment;

import org.springframework.beans.factory.annotation.Autowired;

import agentspring.role.AbstractRole;
import agentspring.role.Role;
import agentspring.role.RoleComponent;
import d13n.domain.agent.EnergyProducer;
import d13n.domain.technology.PowerPlant;
import d13n.repository.Reps;

/**
 * {@link EnergyProducer}s dismantle {@link PowerPlant}s that are pass the
 * technical life
 * 
 * @author <a href="mailto:E.J.L.Chappin@tudelft.nl">Emile Chappin</a> @author
 *         <a href="mailto:A.Chmieliauskas@tudelft.nl">Alfredas
 *         Chmieliauskas</a>
 * 
 */

@RoleComponent
public class DismantlePowerPlantPastTechnicalLifetimeRole extends AbstractRole<EnergyProducer> implements Role<EnergyProducer> {


    @Autowired
    Reps reps;

    public void act(EnergyProducer producer) {

        logger.info("Dismantling plants if passed technical lifetime");

        // dismantle plants when passed technical lifetime
        for (PowerPlant plant : reps.powerPlantRepository.findOperationalPowerPlantsByOwner(producer, getCurrentTick())) {

            int prolongYearsOfDismantlng = producer.getDismantlingProlongingYearsAfterTechnicalLifetime();
            if (!plant.isWithingTechnicalLifetime(getCurrentTick() + prolongYearsOfDismantlng)) {
                logger.info("       Dismantling power plant because the technical life time has passed: " + plant);
                reps.powerPlantRepository.dismantlePowerPlant(plant, getCurrentTick());
            }
        }
    }

}
