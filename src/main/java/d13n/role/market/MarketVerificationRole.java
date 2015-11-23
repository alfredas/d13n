package d13n.role.market;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import agentspring.role.Role;
import agentspring.role.RoleComponent;
import d13n.domain.gis.Zone;
import d13n.domain.market.ElectricitySpotMarket;
import d13n.domain.market.Segment;
import d13n.domain.technology.PowerPlant;
import d13n.repository.Reps;

/**
 * Creates and clears the {@link ElectricitySpotMarket} for two {@link Zone}s.
 * The market is divided into {@link Segment}s and cleared for each segment.
 * Also the emissions cap is adhered to and a global CO2 emissions market is
 * cleared.
 * 
 * @author <a href="mailto:E.J.L.Chappin@tudelft.nl">Emile Chappin</a>
 * 
 * @author <a href="mailto:A.Chmieliauskas@tudelft.nl">Alfredas
 *         Chmieliauskas</a>
 * 
 */
@RoleComponent
public class MarketVerificationRole extends AbstractMarketRole<ElectricitySpotMarket> implements Role<ElectricitySpotMarket> {

    @Autowired
    private Reps reps;


    public void act(ElectricitySpotMarket aRandomMarketNotToBeUsed) {

        logger.info("Validating the markets");

        // find all power plants and store the ones operational to a list.
        List<PowerPlant> powerPlants = new ArrayList<PowerPlant>();
        for (PowerPlant plant : reps.powerPlantRepository.findAll()) {
            if (plant.isOperational(getCurrentTick())) {
                powerPlants.add(plant);
            }
        }
    }

    @Override
    public Reps getReps() {
        return reps;
    }
    
}
