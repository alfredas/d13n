package d13n.role.market;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import agentspring.role.Role;
import agentspring.role.RoleComponent;
import d13n.domain.agent.EnergyProducer;
import d13n.domain.market.Bid;
import d13n.domain.market.ElectricitySpotMarket;
import d13n.domain.market.PowerPlantDispatchPlan;
import d13n.domain.market.Segment;
import d13n.domain.market.SegmentLoad;
import d13n.domain.technology.PowerPlant;
import d13n.repository.Reps;
import d13n.role.AbstractEnergyProducerRole;

/**
 * {@link EnergyProducer} submits offers to the {@link ElectricitySpotMarket}.
 * One {@link Bid} per {@link PowerPlant}.
 * 
 * @author <a href="mailto:A.Chmieliauskas@tudelft.nl">Alfredas
 *         Chmieliauskas</a> @author <a
 *         href="mailto:E.J.L.Chappin@tudelft.nl">Emile Chappin</a>
 * 
 */
@RoleComponent
public class SubmitOffersToElectricitySpotMarketRole extends
		AbstractEnergyProducerRole implements Role<EnergyProducer> {

	@Autowired
    Reps reps;

    @Override
    public Reps getReps() {
        return reps;
    }

	@Transactional
	public void act(EnergyProducer producer) {

		// find all my operating power plants
		for (PowerPlant plant : reps.powerPlantRepository
				.findOperationalPowerPlantsByOwner(producer, getCurrentTick())) {

			// get market for the plant by zone
			ElectricitySpotMarket market = reps.marketRepository
					.findElectricitySpotMarketForZone(plant.getLocation()
							.getZone());

			double mc = calculateMarginalCostExclCO2MarketCost(plant);

			logger.info("Submitting offers for {} with technology {}",
					plant.getName(), plant.getTechnology().getName());

			for (SegmentLoad segmentload : market.getLoadDurationCurve()) {

				Segment segment = segmentload.getSegment();
				long numberOfSegments = reps.segmentRepository.count();
				double capacity = plant.getAvailableCapacity(getCurrentTick(),
						segment, numberOfSegments);
				logger.info("I bid capacity: {} and price: {}", capacity, mc);

				PowerPlantDispatchPlan plan = reps.powerPlantDispatchPlanRepository
						.submitOrUpdatePowerPlantDispatchPlanForSpotMarket(
								plant, producer, market, segment,
								getCurrentTick(), mc, capacity);
				logger.info(
						"Submitted {} for iteration {} to electricity spot market",
						plan);
			}
		}
	}

}
