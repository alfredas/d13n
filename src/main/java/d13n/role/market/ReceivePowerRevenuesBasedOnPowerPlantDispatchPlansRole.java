package d13n.role.market;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import agentspring.role.Role;
import agentspring.role.RoleComponent;
import d13n.domain.agent.EnergyConsumer;
import d13n.domain.agent.EnergyProducer;
import d13n.domain.contract.CashFlow;
import d13n.domain.contract.LongTermContract;
import d13n.domain.gis.Zone;
import d13n.domain.market.ClearingPoint;
import d13n.domain.market.ElectricitySpotMarket;
import d13n.domain.market.PowerPlantDispatchPlan;
import d13n.domain.market.Segment;
import d13n.domain.market.SegmentClearingPoint;
import d13n.domain.technology.Substance;
import d13n.repository.Reps;
import d13n.role.AbstractEnergyProducerRole;

/**
 * Creates and clears the {@link ElectricitySpotMarket} for one {@link Zone}.
 * {@link EnergyConsumer} submit bids to purchase electricity;
 * {@link EnergyProducer} submit ask offers to sell power. The market is divided
 * into {@link Segment}s and cleared for each segment.
 * 
 * @author <a href="mailto:E.J.L.Chappin@tudelft.nl">Emile Chappin</a>
 * 
 * @author <a href="mailto:A.Chmieliauskas@tudelft.nl">Alfredas
 *         Chmieliauskas</a>
 * 
 */
@RoleComponent
public class ReceivePowerRevenuesBasedOnPowerPlantDispatchPlansRole extends
		AbstractEnergyProducerRole implements Role<EnergyProducer> {
   
	@Autowired
    Reps reps;

    @Override
    public Reps getReps() {
        return reps;
    }

	@Transactional
	public void act(EnergyProducer producer) {

		
		logger.info("Process electricity revenues");

		
		// Receive revenues for all long term contracts
		for (Segment segment : reps.genericRepository.findAll(Segment.class)) {
			for (LongTermContract longTermContract : reps.contractRepository
					.findLongTermContractsForEnergyProducerForSegmentActiveAtTime(
							producer, segment, getCurrentTick())) {

				// Update the price with pass through factors.
				double basePrice = longTermContract.getCapacity()
						* longTermContract.getPricePerUnit()
						* segment.getHours();
				double co2PassThrough = longTermContract
						.getCo2PassThroughFactor();
				double co2PriceStart = longTermContract.getCo2PriceStart();
				double currentCo2Price = findLastKnownCO2Price();
				double fuelPassThrough = longTermContract
						.getFuelPassThroughFactor();
				double fuelPriceStart = longTermContract.getFuelPriceStart();
				double currentFuelPrice = 0d;
				Substance mainFuel = longTermContract.getMainFuel();
				if (mainFuel != null) {
					currentFuelPrice = findLastKnownPriceForSubstance(mainFuel);
				}

				// prevent dividing by 0
				if (fuelPriceStart == 0) {
					fuelPriceStart = 1e-8;
				}
				if (co2PriceStart == 0) {
					co2PriceStart = 1e-8;
				}

				double updatedPrice = basePrice
						* (1 + fuelPassThrough
								* (currentFuelPrice / fuelPriceStart - 1))
						* (1 + co2PassThrough
								* (currentCo2Price / co2PriceStart - 1));

				reps.cashFlowRepository.createCashFlow(longTermContract.getTo(),
						longTermContract.getFrom(), updatedPrice,
						CashFlow.ELECTRICITY_LONGTERM, getCurrentTick(), null);

				long hours = 0;
				for (Segment s : longTermContract.getLongTermContractType()
						.getSegments()) {
					hours += s.getHours();
				}

				double pricePerMWh = updatedPrice / (longTermContract.getCapacity() * hours);
				logger.info("Revenue from long term contract @ {} euro/MWh",pricePerMWh);
			}
		}

		// Receive revenues for all spot trade
		for (PowerPlantDispatchPlan electricityPlan : reps.powerPlantDispatchPlanRepository
				.findPowerPlantDispatchPlansForEnergyProducerForTime(producer,
						getCurrentTick())) {
			logger.info("Found dispatch plan: {}", electricityPlan);
			if (electricityPlan.getCapacitySpotMarket() > 0) {
				double price = 0d;
				for (ClearingPoint point : reps.clearingPointRepository
						.findClearingPointsForSegmentAndTime(
								electricityPlan.getSegment(), getCurrentTick())) {
					SegmentClearingPoint cp = (SegmentClearingPoint) point;
					if (cp.getSegment().equals(electricityPlan.getSegment())) {
						price = cp.getPrice();
					}
				}
				logger.info("Revenue from spot market @ {} euro/MWh",price);
				reps.cashFlowRepository.createCashFlow(
						electricityPlan.getBuyingAgentSpot(),
						electricityPlan.getSellingAgent(),
						electricityPlan.getCapacitySpotMarket()
								* electricityPlan.getSegment().getHours()
								* price, CashFlow.ELECTRICITY_SPOT,
						getCurrentTick(), electricityPlan.getPowerPlant());
			}
		}
		
		
		
	}

}
