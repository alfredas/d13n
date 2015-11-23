package d13n.role.market;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import agentspring.role.Role;
import agentspring.role.RoleComponent;
import d13n.domain.agent.EnergyProducer;
import d13n.domain.technology.PowerPlant;
import d13n.repository.Reps;
import d13n.role.AbstractEnergyProducerRole;

/**
 * Use of CO2 allowances
 * 
 * @author <a href="mailto:E.J.L.Chappin@tudelft.nl">Emile Chappin</a>
 * 
 * @author <a href="mailto:A.Chmieliauskas@tudelft.nl">Alfredas
 *         Chmieliauskas</a>
 * 
 */
@RoleComponent
public class UseCO2AllowancesRole extends AbstractEnergyProducerRole implements Role<EnergyProducer> {

	@Autowired
    Reps reps;

    @Override
    public Reps getReps() {
        return reps;
    }
    
    @Transactional
    public void act(EnergyProducer producer) {

        logger.info("Using CO2 allowances now");

        double totalCO2Emission = 0d;
        for (PowerPlant plant : reps.powerPlantRepository.findOperationalPowerPlantsByOwner(producer, getCurrentTick())) {
            totalCO2Emission += calculateCO2EmissionsAtTime(plant, getCurrentTick());
        }
        producer.setCo2Allowances(producer.getCo2Allowances() - totalCO2Emission);
    }

}
