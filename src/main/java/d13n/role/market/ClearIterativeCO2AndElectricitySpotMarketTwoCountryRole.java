package d13n.role.market;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import agentspring.role.AbstractRole;
import agentspring.role.Role;
import agentspring.role.RoleComponent;
import d13n.domain.agent.CommoditySupplier;
import d13n.domain.agent.DecarbonizationModel;
import d13n.domain.agent.EnergyConsumer;
import d13n.domain.agent.EnergyProducer;
import d13n.domain.agent.Government;
import d13n.domain.contract.LongTermContract;
import d13n.domain.gis.Zone;
import d13n.domain.market.CO2Auction;
import d13n.domain.market.ClearingPoint;
import d13n.domain.market.DecarbonizationMarket;
import d13n.domain.market.ElectricitySpotMarket;
import d13n.domain.market.PowerPlantDispatchPlan;
import d13n.domain.market.Segment;
import d13n.domain.market.SegmentLoad;
import d13n.domain.technology.Interconnector;
import d13n.domain.technology.PowerPlant;
import d13n.domain.technology.Substance;
import d13n.domain.technology.SubstanceShareInFuelMix;
import d13n.repository.Reps;
import d13n.util.MapValueComparator;
import d13n.util.Utils;

/**
 * Creates and clears the {@link ElectricitySpotMarket} for two {@link Zone}s.
 * The market is divided into {@link Segment}s and cleared for each segment. A
 * global CO2 emissions market is cleared. The process is iterative and the
 * target is to let the total emissions match the cap.
 * 
 * @author <a href="mailto:E.J.L.Chappin@tudelft.nl">Emile Chappin</a>
 * 
 * @author <a href="mailto:A.Chmieliauskas@tudelft.nl">Alfredas
 *         Chmieliauskas</a>
 * 
 */
@RoleComponent
public class ClearIterativeCO2AndElectricitySpotMarketTwoCountryRole extends
		AbstractRole<DecarbonizationModel> implements
		Role<DecarbonizationModel> {

	@Autowired
	private Reps reps;

	class MarketSegmentClearingOutcome {

		double load;
		double supply;
		double price;

	}

	public void act(DecarbonizationModel model) {

		logger.info("Clearing the CO2 and electricity spot markets using iteration for 2 countries");

		// find all operational power plants and store the ones operational to a
		// list.
		List<PowerPlant> operationalPowerPlants = new ArrayList<PowerPlant>();
		for (PowerPlant plant : reps.powerPlantRepository.findAll()) {
			if (plant.isOperational(getCurrentTick())) {
				operationalPowerPlants.add(plant);
			}
		}

		// find all markets
		List<ElectricitySpotMarket> electricitySpotMarkets = Utils
				.asList(reps.genericRepository.findAll(ElectricitySpotMarket.class));

		// find all fuel prices
		Map<Substance, Double> fuelPriceMap = new HashMap<Substance, Double>();
		for (Substance substance : reps.genericRepository.findAll(Substance.class)) {
			fuelPriceMap.put(substance,
					findLastKnownPriceForSubstance(substance));
		}

		// find all interconnectors
		Interconnector interconnector = reps.genericRepository
				.findFirst(Interconnector.class);

		// find all segments
		List<Segment> segments = Utils.asList(reps.segmentRepository.findAll());

		// find the government
		Government government = reps.genericRepository.findFirst(Government.class);

		CO2Auction co2Auction = reps.genericRepository.findFirst(CO2Auction.class);

		if (model.isCo2TradingImplemented()) {
			boolean stable = false;
			double co2Price = findLastKnownPriceOnMarket(co2Auction);
			double co2Cap = government.getCo2Cap(getCurrentTick());
			double co2Emissions = 0d;
			double minimumCo2Price = government
					.getMinCo2Price(getCurrentTick());
			double co2Penalty = government.getCo2Penalty();
			long totalHours = 0;
			double iterationSpeedFactor = model.getIterationSpeedFactor();
			double iterationSpeedCriterion = model.getIterationSpeedCriterion();
			double capDeviationCriterion = model.getCapDeviationCriterion();
			boolean positive = true;

			while (!stable) {
				// Clear the electricity markets with the expected co2Price
				double voll = electricitySpotMarkets.get(0)
						.getValueOfLostLoad();
				Map<PowerPlant, Double> marginalCostMap = determineAndSortMarginalCostsOfPowerPlants(
						operationalPowerPlants, segments, co2Price, voll);
				
				determineCommitmentOfPowerPlantsOnTheBasisOfLongTermContracts(
						operationalPowerPlants, segments, marginalCostMap);

				long shortageHoursCount = 0;
				totalHours = 0;
				for (Segment segment : segments) {
					MarketSegmentClearingOutcome outcome = clearElectricityMarketSegment(
							operationalPowerPlants, electricitySpotMarkets,
							interconnector.getCapacity(), segment, government,
							co2Price, marginalCostMap);
					// shortage
					if (outcome.supply < outcome.load) {
						shortageHoursCount += segment.getHours();
					}
					totalHours += segment.getHours();
				}

				co2Emissions = determineTotalEmissionsBasedOnPowerPlantDispatchPlan();

				// Determine the deviation from the cap.
				logger.warn("Cap {} (euro/ton) vs emissions {} (euro/ton)",
						co2Cap, co2Emissions);
				double deviation = (co2Emissions - co2Cap) / co2Cap;
				logger.warn("Tick {} Deviation: {} %", getCurrentTick(),
						deviation * 100);

				// check if the deviation is smaller then the criterion --> 1.
				// Close to the cap or almost stopped moving
				if (Math.abs(deviation) < capDeviationCriterion) {
					logger.warn("Deviation is less than capDeviationCriterion");
					stable = true;
				} else if (iterationSpeedFactor < iterationSpeedCriterion) {
					logger.warn("Deviation iterationSpeedFactor is less than iterationSpeedCriterion");
					stable = true;
				} else if (co2Price == minimumCo2Price && co2Emissions < co2Cap) {
					logger.warn("Deviation CO2 price has reached minimum");
					// check if stable enough --> 2. Cap is met with a co2Price
					// equal to the minimum co2 price
					stable = true;
				} else if (shortageHoursCount > 0 && co2Emissions > co2Cap) {
					logger.warn("Experiencing shortage for {} hours",
							((double) shortageHoursCount / (double) totalHours));
					double shortageCo2Price = 0d;
					for (double mc : marginalCostMap.values()) {
						if (mc < voll) {
							shortageCo2Price = voll - mc;
						}
					}
					co2Price = ((double) shortageHoursCount / (double) totalHours)
							* shortageCo2Price
							+ (1 - ((double) shortageHoursCount / (double) totalHours))
							* co2Price;
					logger.warn("Segment weighted CO2 price {}", co2Price);
					stable = true;
				} else if (co2Price >= co2Penalty && co2Emissions >= co2Cap) {// Only
																				// IF
																				// above
																				// the
																				// cap...
					logger.warn("CO2 price ceiling reached {}", co2Price);
					stable = true;
				} else {
					double newCO2Price = co2Price
							* (1 + deviation * iterationSpeedFactor);
					co2Price = newCO2Price;
					logger.warn("Deviation updated CO2 price to {}", co2Price);
				}

				if (!stable) {

					// if price is 0, but the cap is not met, we have to
					// change it, otherwise, you could never get out of 0.
					if (co2Price == 0 && co2Emissions >= co2Cap) {
						logger.warn("Deviation resetting CO2 price to 10");
						co2Price = 10;
					}

					// make the speed smaller if we passed by the target
					if ((positive && deviation < 0)
							|| (!positive && deviation > 0)) {
						iterationSpeedFactor = iterationSpeedFactor / 2;
						logger.warn("Deviation speed factor decreased {}",
								iterationSpeedFactor);
					}

					// record whether the last change was positive or not
					if (deviation < 0) {
						positive = false;
					} else {
						positive = true;
					}

					// If we are below the cap and close to or below the minimum
					// CO2
					// price set the price to the minimum co2
					// price.
					if ((co2Price < (1 + minimumCo2Price))
							&& (co2Emissions < co2Cap)) {
						logger.warn("Deviation reseting CO2 price to minimum");
						co2Price = minimumCo2Price;
					}
				}
			}
			// Save the resulting CO2 price to the Co2 auction
			reps.clearingPointRepository.createOrUpdateClearingPoint(co2Auction,
					co2Price, co2Emissions, getCurrentTick(), reps.modelRepository
							.findModel().getIteration());
		} else {
			Map<PowerPlant, Double> marginalCostMap = determineAndSortMarginalCostsOfPowerPlants(
					operationalPowerPlants, segments, 0, electricitySpotMarkets
							.get(0).getValueOfLostLoad());
			determineCommitmentOfPowerPlantsOnTheBasisOfLongTermContracts(
					operationalPowerPlants, segments, marginalCostMap);
			for (Segment segment : segments) {
				clearElectricityMarketSegment(operationalPowerPlants,
						electricitySpotMarkets, interconnector.getCapacity(),
						segment, government, 0, marginalCostMap);
			}
		}

	}

	private Map<PowerPlant, Double> determineAndSortMarginalCostsOfPowerPlants(
			List<PowerPlant> plants, List<Segment> segments, double co2Price,
			double valueOfLostLoad) {
		// Bid
		Map<PowerPlant, Double> costMap = new HashMap<PowerPlant, Double>();

		// Take one segment to get the bids with the marginal cost excl CO2 cost
		Segment segment = segments.get(0);

		PowerPlant lastPlant = null;

		for (PowerPlant plant : plants) {

			// the cost for this plant in this segment
			PowerPlantDispatchPlan plan = reps.powerPlantDispatchPlanRepository
					.findPowerPlantDispatchPlanForPowerPlantForSegmentForTime(
							plant, segment, getCurrentTick());

			// Is there a bid?
			if (plan != null) {

				// Determine the total marginal cost (including CO2 price)
				double costOfPlantPerMWh = plan.getMarginalCostExclCO2()
						+ (determineEmissionIntensityPowerPlant(plant) * co2Price);

				// Store the marginal cost
				costMap.put(plant, costOfPlantPerMWh);

			} else {

				// If there's no bid, assume it cannot produce, therefore don't
				// store it
				logger.warn("Found no bid for plant {}", plant);
			}
			lastPlant = plant;
		}
		costMap.put(lastPlant, valueOfLostLoad);

		// Sort the plants on marginal cost
		MapValueComparator comp = new MapValueComparator(costMap);
		Map<PowerPlant, Double> sortedMap = new TreeMap<PowerPlant, Double>(
				comp);
		sortedMap.putAll(costMap);

		return sortedMap;
	}

	/**
	 * Determine for each power plant whether it will be covered (partially) by
	 * long-term contracts for each of the segments
	 * 
	 * @param plants
	 *            all plants
	 * @param segments
	 *            segments
	 * @param co2Price
	 *            the co2 price
	 * @param marginalCostMap 
	 * 			  the marginal cost (including Co2 cost)        
	 */
	private void determineCommitmentOfPowerPlantsOnTheBasisOfLongTermContracts(
			List<PowerPlant> plants, List<Segment> segments, 
			Map<PowerPlant, Double> marginalCostMap) {

		for (EnergyProducer producer : reps.genericRepository
				.findAll(EnergyProducer.class)) {

			for (Segment segment : segments) {

				// How much capacity is contracted by long term contracts in
				// this segment?
				double contractedCapacityInSegment = 0;

				for (LongTermContract ltc : reps.contractRepository
						.findLongTermContractsForEnergyProducerForSegmentActiveAtTime(
								producer, segment, getCurrentTick())) {
					contractedCapacityInSegment += ltc.getCapacity();
				}

				logger.info(
						"{} has {} capacity contracted by long term contracts in segment"
								+ segment, producer,
						contractedCapacityInSegment);

				// for all power plants in the sorted marginal cost map
				for (PowerPlant plant : marginalCostMap.keySet()) {
					// if it is mine{
					if (plant.getOwner().equals(producer)) {
						PowerPlantDispatchPlan plan = reps.powerPlantDispatchPlanRepository
								.findPowerPlantDispatchPlanForPowerPlantForSegmentForTime(
										plant, segment, getCurrentTick());

						double availableCapacity = plant.getAvailableCapacity(
								getCurrentTick(), segment, segments.size());
						
						//logger.warn("Capacity of plant " + plant.toString() + " is " + availableCapacity);

						if (plant.getTechnology()
								.isApplicableForLongTermContract()) {
							if (contractedCapacityInSegment - availableCapacity > 0) {

								// the whole plant has to be used for long term
								// contract
								reps.powerPlantDispatchPlanRepository
										.updateCapacityLongTermContract(plan,
												availableCapacity);
							}
						} else {
							// use the contractedCapacity left for long term
							// contract
							reps.powerPlantDispatchPlanRepository
									.updateCapacityLongTermContract(plan,
											contractedCapacityInSegment);
						}
						reps.powerPlantDispatchPlanRepository
								.updateCapacitySpotMarket(
										plan,
										availableCapacity
												- plan.getCapacityLongTermContract());
						contractedCapacityInSegment -= plan
								.getCapacityLongTermContract();
						if (plan.getCapacityLongTermContract() > 0
								&& plan.getCapacitySpotMarket() > 0) {
							logger.warn("plan (position 1): {}", plan);
						}
					}
				}
			}
		}
		
	}

	/**
	 * Clears a time segment of all electricity markets for a given CO2 price.
	 * 
	 * @param powerPlants
	 *            to be used
	 * @param markets
	 *            to clear
	 * @return the total CO2 emissions
	 */
	@Transactional
	private MarketSegmentClearingOutcome clearElectricityMarketSegment(
			List<PowerPlant> powerPlants, List<ElectricitySpotMarket> markets,
			double interconnectorCapacity, Segment segment,
			Government government, double co2Price,
			Map<PowerPlant, Double> marginalCostMap) {

		MarketSegmentClearingOutcome outcome = new MarketSegmentClearingOutcome();

		Map<ElectricitySpotMarket, Double> loadInMarket = new HashMap<ElectricitySpotMarket, Double>();

		// Determine total demand in this segment
		double totalDemand = 0d;

		for (ElectricitySpotMarket market : markets) {
			SegmentLoad segmentload = reps.segmentLoadRepository
					.findSegmentLoadBySegmentAndMarket(segment, market);

			double thisDemand = segmentload.getBaseLoad()
					* market.getDemandGrowthTrend().getValue(getCurrentTick());
			loadInMarket.put(market, thisDemand);
			totalDemand += thisDemand;
		}

		// Determine long term contracted capacity and remove it from the load
		double totalDemandCoveredByLongTermContracts = 0d;

		// for each energy consumer
		for (EnergyConsumer consumer : reps.genericRepository
				.findAll(EnergyConsumer.class)) {
			// for each active LTC
			for (LongTermContract ltc : reps.contractRepository
					.findLongTermContractsForEnergyConsumerForSegmentActiveAtTime(
							consumer, segment, getCurrentTick())) {
				totalDemandCoveredByLongTermContracts += ltc.getCapacity();
			}
		}
		// Remove the contracted load from total demand in the spot market
		totalDemand -= totalDemandCoveredByLongTermContracts;
		outcome.load = totalDemand;

		logger.info("Segment {}, total demand {}", segment, totalDemand);
		double totalSupply = 0; // In MW

		// Keep track of supply per market. Start at 0.
		HashMap<ElectricitySpotMarket, Double> supplyInMarket = new HashMap<ElectricitySpotMarket, Double>();
		for (ElectricitySpotMarket m : markets) {
			supplyInMarket.put(m, 0d);
		}

		// empty list of plants that are supplying.
		int supplyingPlants = 0;
		double marginalPlantMarginalCost = Double.MAX_VALUE;

		// For each plant in the cost-ordered list
		for (Entry<PowerPlant, Double> plantCost : marginalCostMap.entrySet()) {

			PowerPlant plant = plantCost.getKey();
			ElectricitySpotMarket myMarket = getMarketForPowerPlant(
					plantCost.getKey(), markets);

			// Make it produce as long as there is load.
			double plantSupply = determineProductionOnSpotMarket(plant,
					segment, totalSupply, totalDemand);

			if (plantSupply > 0) {
				// Plant is producing, store the information to determine price
				// and so on.
				totalSupply += plantSupply;
				supplyingPlants++;
				marginalPlantMarginalCost = plantCost.getValue();
				supplyInMarket.put(myMarket, supplyInMarket.get(myMarket)
						+ plantSupply);

			}
		}
		logger.info(
				"Before market coupling found {} plants supplying out of {} available",
				supplyingPlants, marginalCostMap.size());
		logger.info("Before market coupling supply: {}", supplyInMarket);

		// Determine the flow over the interconnector.
		ElectricitySpotMarket firstMarket = markets.get(0);
		double loadInFirstMarket = loadInMarket.get(firstMarket);
		double supplyInFirstMarket = supplyInMarket.get(firstMarket);
		
		// Interconnector flow defined as from market A --> market B = positive
		double interconnectorFlow = supplyInFirstMarket - loadInFirstMarket;

		logger.info(
				"Before market coupling interconnector flow: {}, available interconnector capacity {}",
				interconnectorFlow, interconnectorCapacity);

		// if interconnector is not limiting, there is one price
		if (Math.abs(interconnectorFlow) <= interconnectorCapacity) {
			// Set the price to the bid of the marginal plant.
			for (ElectricitySpotMarket market : markets) {
				double supplyInThisMarket = supplyInMarket.get(market);

				outcome.supply += supplyInThisMarket;
				outcome.price = marginalPlantMarginalCost;

				reps.clearingPointRepository.createOrUpdateSegmentClearingPoint(
						segment, market, marginalPlantMarginalCost,
						supplyInThisMarket, getCurrentTick(), reps.modelRepository
								.findModel().getIteration());
				logger.info("Stored a system-uniform price for market "
						+ market + " / segment " + segment + " -- supply "
						+ supplyInThisMarket + " -- price: "
						+ marginalPlantMarginalCost);
			}
		} else {
			// else there are two prices
			logger.info("There should be multiple prices, but first we should do market coupling.");

			boolean firstImporting = true;
			if (interconnectorFlow > 0) {
				firstImporting = false;
			}

			boolean first = true;
			for (ElectricitySpotMarket market : markets) {

				// Determine load for this market. Which is market's true load
				// +/- the full interconnector capacity, based on direction of
				// the flow
				double marketLoad = 0d;
				if ((first && firstImporting) || (!first && !firstImporting)) {
					marketLoad = loadInMarket.get(market)
							- interconnectorCapacity;
				} else {
					marketLoad = loadInMarket.get(market)
							+ interconnectorCapacity;
				}
				first = false;

				// for each energy consumer
				for (EnergyConsumer consumer : reps.genericRepository
						.findAll(EnergyConsumer.class)) {
					// for each active LTC
					for (LongTermContract ltc : reps.contractRepository
							.findLongTermContractsForEnergyConsumerForSegmentForZoneActiveAtTime(
									consumer, segment, market.getZone(),
									getCurrentTick())) {
						totalDemandCoveredByLongTermContracts += ltc
								.getCapacity();
					}
				}
				// Remove the contracted load from total demand in the spot
				// market
				totalDemand -= totalDemandCoveredByLongTermContracts;
				outcome.load = totalDemand;

				double marketSupply = 0; // In MW

				// empty list of plants that are supplying.
				supplyingPlants = 0;
				marginalPlantMarginalCost = Double.MAX_VALUE;

				// For each plant in the cost-ordered list
				for (Entry<PowerPlant, Double> plantCost : marginalCostMap
						.entrySet()) {

					// If it is in the right market
					PowerPlant plant = plantCost.getKey();
					if (getMarketForPowerPlant(plant, markets).equals(market)) {

						// Make it produce as long as there is load.
						double plantSupply = determineProductionOnSpotMarket(
								plant, segment, marketSupply, marketLoad);
						if (plantSupply > 0) {
							// Plant is producing, store the information to
							// determine price and so on.
							marketSupply += plantSupply;
							marginalPlantMarginalCost = plantCost.getValue();
							supplyingPlants++;
						}
					}
				}
				outcome.supply += marketSupply;
				outcome.price = marginalPlantMarginalCost;

				reps.clearingPointRepository.createOrUpdateSegmentClearingPoint(
						segment, market, marginalPlantMarginalCost,
						marketSupply, getCurrentTick(), reps.modelRepository
								.findModel().getIteration());
				logger.info("Stored a market specific price for market "
						+ market + " / segment " + segment + " -- supply "
						+ marketSupply + " -- price: "
						+ marginalPlantMarginalCost);
			}
		}
		return outcome;
	}

	public double determineProductionOnSpotMarket(PowerPlant plant,
			Segment segment, double supplySoFar, double load) {

		// Find out the capacity available capacity for spot trading
		PowerPlantDispatchPlan plan = reps.powerPlantDispatchPlanRepository
				.findPowerPlantDispatchPlanForPowerPlantForSegmentForTime(
						plant, segment, getCurrentTick());
		double plantCapacity = plan.getCapacitySpotMarket();
		double plantSupply = 0d;

		// if after adding the supply of this extra plant demand
		// is not yet met
		if ((supplySoFar + plantCapacity) < load) {

			// Plant will be supplying completely
			plantSupply = plantCapacity;
		} else {

			// Plant will by partly supplying and this is the
			// final plant or is not supplying at all
			plantSupply = load - supplySoFar;
		}

		// Override the plan of this plant
		reps.powerPlantDispatchPlanRepository.updateCapacitySpotMarket(plan,
					plantSupply);
		
		if (plan.getCapacityLongTermContract() > 0
				&& plan.getCapacitySpotMarket() > 0) {
			logger.warn("plan (position 2): {}", plan);
		}
		return plantSupply;
	}

	public double determineTotalEmissionsBasedOnPowerPlantDispatchPlan() {
		double totalEmissions = 0d;
		int counter = 0;
		for (PowerPlantDispatchPlan plan : reps.powerPlantDispatchPlanRepository
				.findPowerPlantDispatchPlansForTime(getCurrentTick())) {
			double operationalCapacity = plan.getCapacityLongTermContract()
					+ plan.getCapacitySpotMarket();
			double emissionIntensity = determineEmissionIntensityPowerPlant(plan
					.getPowerPlant());
			double hours = plan.getSegment().getHours();
			totalEmissions += operationalCapacity * emissionIntensity * hours;
			counter++;
		}
		logger.warn(
				"Total emissions: {} based on {} power plant dispatch plans",
				totalEmissions, counter);
		return totalEmissions;
	}

	// TODO the methods below are copied w/ small edits from
	// AbstracEnergyProducerRole.
	/**
	 * Finds the last known price for a substance. We try to find the market for
	 * it and get it get the price on that market for this tick, previous tick,
	 * or from a possible supplier directly. If multiple prices are found, the
	 * average is returned. This is the case for electricity spot markets, as
	 * they may have segments.
	 * 
	 * @param substance
	 *            the price we want for
	 * @return the (average) price found
	 */
	private double findLastKnownPriceForSubstance(Substance substance) {

		DecarbonizationMarket market = reps.marketRepository
				.findMarketBySubstance(substance);
		if (market == null) {
			logger.warn("No market found for {} so no price can be found",
					substance.getName());
			return 0d;
		} else {
			return findLastKnownPriceOnMarket(market);
		}
	}

	/**
	 * Finds the last known price on a specific market. We try to get it for
	 * this tick, previous tick, or from a possible supplier directly. If
	 * multiple prices are found, the average is returned. This is the case for
	 * electricity spot markets, as they may have segments.
	 * 
	 * @param substance
	 *            the price we want for
	 * @return the (average) price found
	 */
	private double findLastKnownPriceOnMarket(DecarbonizationMarket market) {
		Double average = calculateAverageMarketPriceBasedOnClearingPoints(reps.clearingPointRepository
				.findClearingPointsForMarketAndTime(market, getCurrentTick()));
		Substance substance = market.getSubstance();

		if (average != null) {
			logger.info("Average price found on market for this tick for {}",
					substance.getName());
			return average;
		}

		average = calculateAverageMarketPriceBasedOnClearingPoints(reps.clearingPointRepository
				.findClearingPointsForMarketAndTime(market,
						getCurrentTick() - 1));
		if (average != null) {
			logger.info(
					"Average price found on market for previous tick for {}",
					substance.getName());
			return average;
		}

		if (market.getReferencePrice() > 0) {
			logger.info("Found a reference price found for market for {}",
					substance.getName());
			return market.getReferencePrice();
		}

		for (CommoditySupplier supplier : reps.genericRepository
				.findAll(CommoditySupplier.class)) {
			if (supplier.getSubstance().equals(substance)) {

				logger.info(
						"Price found for {} by asking the supplier {} directly",
						substance.getName(), supplier.getName());
				return supplier.getPriceOfCommodity()
						.getValue(getCurrentTick());
			}
		}

		logger.info("No price has been found for {}", substance.getName());
		return 0d;
	}

	/**
	 * Calculates the volume-weighted average price on a market based on a set
	 * of clearingPoints.
	 * 
	 * @param clearingPoints
	 *            the clearingPoints with the volumes and prices
	 * @return the weighted average
	 */
	private Double calculateAverageMarketPriceBasedOnClearingPoints(
			Iterable<ClearingPoint> clearingPoints) {
		double priceTimesVolume = 0d;
		double volume = 0d;

		for (ClearingPoint point : clearingPoints) {
			priceTimesVolume += point.getPrice() * point.getVolume();
			volume += point.getVolume();
		}
		if (volume > 0) {
			return priceTimesVolume / volume;
		}
		return null;
	}

	private double determineEmissionIntensityPowerPlant(PowerPlant plant) {

		double emission = 0d;
		for (SubstanceShareInFuelMix sub : plant.getFuelMix()) {
			Substance substance = sub.getSubstance();
			double fuelAmount = sub.getShare();
			double co2density = substance.getCo2Density();

			// determine the total cost per MWh production of this plant
			double emissionForThisFuel = fuelAmount * co2density;
			logger.info(
					plant
							+ " -- adding cost {} euro/MWh and emission {} ton/MWh for fuel "
							+ sub.getSubstance().getName(),
					emissionForThisFuel);

			emission += emissionForThisFuel;
		}
		return emission;
	}

	private ElectricitySpotMarket getMarketForPowerPlant(PowerPlant plant,
			List<ElectricitySpotMarket> markets) {
		for (ElectricitySpotMarket market : markets) {
			if (plant.getLocation().getZone().equals(market.getZone())) {
				return market;
			}
		}
		return null;
	}

	public Reps getReps() {
		return reps;
	}

}
