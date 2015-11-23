package d13n.role.operating;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import agentspring.role.Role;
import agentspring.role.RoleComponent;
import d13n.domain.agent.EnergyProducer;
import d13n.domain.agent.Government;
import d13n.domain.agent.PowerPlantMaintainer;
import d13n.domain.contract.CashFlow;
import d13n.domain.technology.PowerPlant;
import d13n.repository.Reps;
import d13n.role.AbstractEnergyProducerRole;

/**
 * {@link EnergyProducer}s pay CO2 taxes to the {@link Government}.
 * 
 * @author <a href="mailto:A.Chmieliauskas@tudelft.nl">Alfredas
 *         Chmieliauskas</a> @author <a
 *         href="mailto:E.J.L.Chappin@tudelft.nl">Emile Chappin</a>
 */
@RoleComponent
public class PayOperatingAndMaintainanceCostsRole extends AbstractEnergyProducerRole implements Role<EnergyProducer> {

	@Autowired
    Reps reps;

    public Reps getReps() {
        return reps;
    }
    
    @Transactional
    public void act(EnergyProducer producer) {
        logger.info("Pay the Operating and Maintainance cost tax");

        PowerPlantMaintainer maintainer = reps.genericRepository.findFirst(PowerPlantMaintainer.class);
        int i = 0;
        for (PowerPlant plant : reps.powerPlantRepository.findOperationalPowerPlantsByOwner(producer, getCurrentTick())) {
            i++;
            double money = plant.getTechnology().getFixedOperatingCost();
            // TODO calculate actual based on modifier.
            logger.info("Im paying {} for O and M of plant {}", money, plant.getName());
            reps.cashFlowRepository.createCashFlow(producer, maintainer, money, CashFlow.FIXEDOMCOST, getCurrentTick(), plant);
        }
        logger.info("I: {} have paid for {} plants ", producer, i);
    }
}
