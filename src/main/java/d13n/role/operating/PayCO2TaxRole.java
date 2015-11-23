package d13n.role.operating;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import agentspring.role.Role;
import agentspring.role.RoleComponent;
import d13n.domain.agent.EnergyProducer;
import d13n.domain.agent.Government;
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
public class PayCO2TaxRole extends AbstractEnergyProducerRole implements Role<EnergyProducer> {

	@Autowired
    Reps reps;

    public Reps getReps() {
        return reps;
    }
    
    @Transactional
    public void act(EnergyProducer producer) {
        logger.info("Pay the CO2 tax");

        Government government = reps.genericRepository.findFirst(Government.class);

        for (PowerPlant plant : reps.powerPlantRepository.findOperationalPowerPlantsByOwner(producer, getCurrentTick())) {
            double money = calculateCO2Tax(plant);
            CashFlow cf = reps.cashFlowRepository.createCashFlow(producer, government, money, CashFlow.CO2TAX, getCurrentTick(), plant);
            logger.info("Cash flow created: {}", cf);
        }
    }

}
