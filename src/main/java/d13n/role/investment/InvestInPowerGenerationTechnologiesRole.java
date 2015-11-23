package d13n.role.investment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import agentspring.role.Role;
import agentspring.role.RoleComponent;
import d13n.domain.agent.BigBank;
import d13n.domain.agent.EnergyProducer;
import d13n.domain.agent.Government;
import d13n.domain.agent.PowerPlantManufacturer;
import d13n.domain.contract.CashFlow;
import d13n.domain.contract.Loan;
import d13n.domain.gis.Zone;
import d13n.domain.market.CO2Auction;
import d13n.domain.market.ElectricitySpotMarket;
import d13n.domain.market.Segment;
import d13n.domain.market.SegmentLoad;
import d13n.domain.technology.PowerGeneratingTechnology;
import d13n.domain.technology.PowerGridNode;
import d13n.domain.technology.PowerPlant;
import d13n.domain.technology.Substance;
import d13n.domain.technology.SubstanceShareInFuelMix;
import d13n.repository.Reps;
import d13n.role.AbstractEnergyProducerRole;
import d13n.util.MapValueComparator;
import d13n.util.Utils;

/**
 * {@link EnergyProducer}s decide to invest in new {@link PowerPlant}
 * 
 * @author <a href="mailto:E.J.L.Chappin@tudelft.nl">Emile Chappin</a> @author
 *         <a href="mailto:A.Chmieliauskas@tudelft.nl">Alfredas
 *         Chmieliauskas</a>
 * 
 */
@RoleComponent
public class InvestInPowerGenerationTechnologiesRole extends AbstractEnergyProducerRole implements Role<EnergyProducer> {


    @Autowired
    Reps reps;
    
    public Reps getReps() {
		return reps;
	}
    
    // market expectations
    Map<ElectricitySpotMarket, MarketInformation> marketInfoMap = new HashMap<ElectricitySpotMarket, MarketInformation>();
        
    public void act(EnergyProducer agent) {
    	
    	long futureTimePoint = getCurrentTick() + agent.getInvestmentFutureTimeHorizon();
    	logger.warn(agent + " is looking at timepoint " + futureTimePoint);
    	
    	// ==== Expectations ===

    	// Fuel Prices
    	Map<Substance, Double> expectedFuelPrices = new HashMap<Substance, Double>();
        for (Substance substance : reps.genericRepository.findAll(Substance.class)) {
            // use last price
            expectedFuelPrices.put(substance, findLastKnownPriceForSubstance(substance));//TODO use expected fuel price
        }
        
        // CO2
        double expectedCO2Price = determineExpectedCO2PriceInclTax(futureTimePoint);//TODO use expected co2 price
        
        // Investment decision
        for (ElectricitySpotMarket market : reps.genericRepository.findAllAtRandom(ElectricitySpotMarket.class)) {
        	
        	MarketInformation marketInformation = new MarketInformation(market, expectedFuelPrices, expectedCO2Price, futureTimePoint);
        	/*
        	if (marketInfoMap.containsKey(market) && marketInfoMap.get(market).time == futureTimePoint) {
        		marketInformation = marketInfoMap.get(market);
        	} else {
        		marketInformation = new MarketInformation(market, expectedFuelPrices, expectedCO2Price, futureTimePoint);
        		marketInfoMap.put(market, marketInformation);
        	}
        	*/
        	        	
            double highestValue = Double.MIN_VALUE;
            PowerGeneratingTechnology bestTechnology = null;
	
            for (PowerGeneratingTechnology technology : reps.genericRepository.findAll(PowerGeneratingTechnology.class)) {

            	PowerPlant plant = createNonPersistentPowerPlant(getCurrentTick(), null, null, technology);
                // if too much capacity of this technology in the pipeline (not limited to the 5 years)
                double installedCapacityOfTechnology = calculateMarketCapacity(market, technology, futureTimePoint);
                double ownedTotalCapacityInMarket = calculateTotalOwnerCapacity(market, futureTimePoint, agent);
                double ownedCapacityInMarketOfThisTechnology = calculateOwnerCapacityOfType(market, technology, futureTimePoint, agent);

                if ((installedCapacityOfTechnology + technology.getCapacity()) / (marketInformation.maxExpectedLoad + technology.getCapacity()) > technology.getMaximumInstalledCapacityFractionInCountry()) {
                    logger.warn(agent + " will not invest in {} technology because there's too much of this type in the market", technology);
                } else if (ownedCapacityInMarketOfThisTechnology > ownedTotalCapacityInMarket * technology.getMaximumInstalledCapacityFractionPerAgent()) {
                    logger.warn(agent + " will not invest in {} technology because there's too much capacity planned by him", technology);
                } else if (plant.getActualInvestedCapital()*(1-agent.getDebtRatioOfInvestments()) > agent.getDownpaymentFractionOfCash() * agent.getCash()) {
                    logger.warn(agent + " will not invest in {} technology as he does not have enough money for downpayment", technology); //TODO: Modifier for investment costs is missing here
                } else {
                    
                	
                    plant.setActualEfficiency(calculateEfficiency(plant));

                    Map<Substance, Double> myFuelPrices = new HashMap<Substance, Double>();
                    for (Substance fuel : technology.getFuels()) {
                        myFuelPrices.put(fuel, expectedFuelPrices.get(fuel));
                    }
                    Set<SubstanceShareInFuelMix> fuelMix = calculateFuelMix(myFuelPrices, plant.getTechnology().getMinimumFuelQuality(), plant.getActualEfficiency(), expectedCO2Price);
                    plant.setFuelMix(fuelMix);

                    double expectedMarginalCost = determineExpectedMarginalCost(plant, expectedFuelPrices, expectedCO2Price);
                    double runningHours = 0d;
                    double expectedGrossProfit = 0d;
                    
                    logger.warn("Agent {}  found that the installed capacity in the market {} in future to be " + marketInformation.capacitySum + "and expectde maximum demand to be " + marketInformation.maxExpectedLoad, agent, market);
                    long numberOfSegments = reps.segmentRepository.count();
                    
                    //TODO somehow the prices of long-term contracts could also be used here to determine the expected profit. Maybe not though... 
                    for (SegmentLoad segmentLoad : market.getLoadDurationCurve()) {
                        double expectedElectricityPrice = marketInformation.expectedElectricityPricesPerSegment.get(segmentLoad.getSegment());
                        double hours = segmentLoad.getSegment().getHours();
                        if (expectedMarginalCost <= expectedElectricityPrice) {
                            runningHours += hours;
                            expectedGrossProfit += (expectedElectricityPrice - expectedMarginalCost) * hours * plant.getAvailableCapacity(futureTimePoint, segmentLoad.getSegment(), numberOfSegments);
                        }
                    }

                    logger.warn(agent + "expects technology {} to have {} running", technology, runningHours);
                    // expect to meet minimum running hours?
                    if(runningHours < plant.getTechnology().getMinimumRunningHours()){
                    	logger.warn(agent + " will not invest in {} technology as he expect to have {} running, which is lower then required", technology, runningHours);
                    } else {

						double fixedOMCost = calculateFixedOperatingCost(plant);// /
																				// plant.getTechnology().getCapacity();

						double operatingProfit = expectedGrossProfit
								- fixedOMCost; // TODO should we not exclude
												// fixed cost, or name that NET
												// profit?

						// TODO Alter discount rate on the basis of the amount
						// in long-term contracts?
						// TODO Alter discount rate on the basis of other stuff,
						// such as amount of money, market share, portfolio
						// size.
						
						//Calculation of weighted average cost of capital, based on the companies debt-ratio
						double wacc = (1-agent.getDebtRatioOfInvestments())*agent.getEquityInterestRate()
								+agent.getDebtRatioOfInvestments()*agent.getLoanInterestRate();
						
						//Creation of out cash-flow during power plant building phase (note that the cash-flow is negative!)
						TreeMap<Integer, Double> discountedProjectCapitalOutflow = 
								calculateSimplePowerPlantInvestmentCashFlow(technology.getDepreciationTime(), 
										technology.getExpectedLeadtime(), plant.getActualInvestedCapital(), 
										0);
						//Creation of in cashflow during operation
						TreeMap<Integer, Double> discountedProjectCashInflow = 
								calculateSimplePowerPlantInvestmentCashFlow(technology.getDepreciationTime(), 
										technology.getExpectedLeadtime(), 0, 
										operatingProfit);
						

						double discountedCapitalCosts = npv(discountedProjectCapitalOutflow, wacc);// are defined negative!!
																// technology.getCapacity();
						
						logger.warn("Agent {}  found that the discounted capital for technology {} to be " + discountedCapitalCosts, agent, technology);

						double discountedOpProfit = npv(discountedProjectCashInflow, wacc);
						
						//logger.warn("Agent {}  found the expected prices to be {}", agent, marketInformation.expectedElectricityPricesPerSegment);
						logger.warn("Agent {}  found that the projected discounted inflows for technology {} to be " + discountedOpProfit, agent, technology);

						double projectValue = discountedOpProfit
								+ discountedCapitalCosts;
						
						logger.warn("Agent {}  found the project value for technology {} to be " + projectValue, agent, technology);
						

						// double projectTotalValue = projectValuePerMW *
						// plant.getTechnology().getCapacity();

						//double projectReturnOnInvestment = discountedOpProfit
							//	/ (-discountedCapitalCosts);

						/*Divide by capacity, in order not to favour large power plants (which have the single
						 * largest NPV
						 */
						 
						
						if (projectValue > 0
								&& projectValue/plant.getTechnology().getCapacity() > highestValue) {
							highestValue = projectValue/plant.getTechnology().getCapacity();
							bestTechnology = plant.getTechnology();
						}
                    }

                }
            }
            
            if (bestTechnology != null) {
                logger.warn("Agent {} invested in technology {} at tick " + getCurrentTick(), agent, bestTechnology);

                PowerPlant plant = reps.powerPlantRepository.createPowerPlant(getCurrentTick(), agent, getNodeForZone(market.getZone()), bestTechnology);
                PowerPlantManufacturer manufacturer = reps.genericRepository.findFirst(PowerPlantManufacturer.class);
                BigBank bigbank = reps.genericRepository.findFirst(BigBank.class);
                
                double investmentCostPayedByEquity = plant.getActualInvestedCapital()*(1-agent.getDebtRatioOfInvestments());
                double investmentCostPayedByDebt = plant.getActualInvestedCapital()*agent.getDebtRatioOfInvestments();
                double downPayment = investmentCostPayedByEquity;
                createSpreadOutDownPayments(agent, manufacturer, downPayment, plant);

                double amount = determineLoanAnnuities(investmentCostPayedByDebt, plant.getTechnology().getDepreciationTime(), agent.getLoanInterestRate());
                Loan loan = reps.loanRepository.createLoan(agent, bigbank, amount, plant.getTechnology().getDepreciationTime(), getCurrentTick(), plant);
                // Create the loan
                reps.powerPlantRepository.setLoan(plant, loan);

            } else {
                logger.warn("{} found no suitable technology anymore to invest in at tick " + getCurrentTick(), agent);
                // agent will not participate in the next round of investment if he does not invest now
                setNotWillingToInvest(agent);
            }
        }
    }
    
    
    //Creates n downpayments of equal size in each of the n building years of a power plant
    @Transactional
    private void createSpreadOutDownPayments(EnergyProducer agent, PowerPlantManufacturer manufacturer, double totalDownPayment, PowerPlant plant){
    	int buildingTime = plant.getTechnology().getExpectedLeadtime();
    	for(int i=0 ; i<buildingTime; i++){
    		reps.cashFlowRepository.createCashFlow(agent, manufacturer, totalDownPayment/buildingTime, CashFlow.DOWNPAYMENT, getCurrentTick()+i, plant);
    	}
    }
    
    @Transactional
    private void setNotWillingToInvest(EnergyProducer agent) {
    	agent.setWillingToInvest(false);
    }
    
    //Create a powerplant investment and operation cash-flow in the form of a map. If only investment, or operation costs should be considered set totalInvestment or operatingProfit to 0
    private TreeMap<Integer,Double> calculateSimplePowerPlantInvestmentCashFlow(int depriacationTime, int buildingTime, double totalInvestment, double operatingProfit){
		TreeMap<Integer,Double> investmentCashFlow = new TreeMap<Integer, Double>();
		double equalTotalDownPaymentInstallement = totalInvestment / buildingTime;
		for(int i=0; i<buildingTime;i++){
			investmentCashFlow.put(new Integer(i), -equalTotalDownPaymentInstallement);
		}
		for(int i=buildingTime; i<depriacationTime+buildingTime; i++){
			investmentCashFlow.put(new Integer(i), operatingProfit);
		}
    	
    	return investmentCashFlow;
    }
    
    private double npv(TreeMap<Integer,Double> netCashFlow, double wacc){
    	double npv = 0;
    	for(Integer iterator:netCashFlow.keySet()){
    		npv += netCashFlow.get(iterator).doubleValue()/Math.pow(1+wacc,iterator.intValue());
    	}
    	return npv;
    }

    private double determineExpectedCO2PriceInclTax(long futureTimePoint) {
        double co2Price = 0d;
        co2Price += reps.genericRepository.findFirst(Government.class).getCO2Tax(futureTimePoint);
        co2Price += findLastKnownPriceOnMarket(reps.genericRepository.findFirst(CO2Auction.class));
        return co2Price;
    }

    public double determineExpectedMarginalCost(PowerPlant plant, Map<Substance, Double> expectedFuelPrices, double expectedCO2Price) {
        double mc = determineExpectedMarginalFuelCost(plant, expectedFuelPrices);
        double co2Intensity = calculateCO2Intensity(plant);
        mc += co2Intensity * expectedCO2Price;
        return mc;
    }

    public double determineExpectedMarginalFuelCost(PowerPlant powerPlant, Map<Substance, Double> expectedFuelPrices) {
        double fc = 0d;
        for (SubstanceShareInFuelMix mix : powerPlant.getFuelMix()) {
            double amount = mix.getShare();
            double fuelPrice = expectedFuelPrices.get(mix.getSubstance());
            fc += amount * fuelPrice;
        }
        return fc;
    }

    public PowerPlant createNonPersistentPowerPlant(long time, EnergyProducer energyProducer, PowerGridNode location,
            PowerGeneratingTechnology technology) {
        PowerPlant plant = new PowerPlant();
        String label = technology.getName();
        plant.setName(label);
        plant.setTechnology(technology);
        plant.setOwner(energyProducer);
        plant.setLocation(location);
        plant.setConstructionStartTime(time);
        plant.calculateAndSetActualInvestedCapital(time);
        return plant;
    }

    private PowerGridNode getNodeForZone(Zone zone) {
        for (PowerGridNode node : reps.genericRepository.findAll(PowerGridNode.class)) {
            if (node.getZone().equals(zone)) {
                return node;
            }
        }
        return null;
    }

    private class MarketInformation {
    	
        Map<Segment, Double> expectedElectricityPricesPerSegment;
        double maxExpectedLoad = 0d;
        Map<PowerPlant, Double> meritOrder;
        double capacitySum;
        
        
        MarketInformation(ElectricitySpotMarket market, Map<Substance, Double> fuelPrices, double co2price, long time) {
        	// determine expected power prices
        	expectedElectricityPricesPerSegment = new HashMap<Segment, Double>();
            Map<PowerPlant, Double> marginalCostMap = new HashMap<PowerPlant, Double>();
            capacitySum=0d;

            // get merit order for this market
            for (PowerPlant plant : reps.powerPlantRepository.findExpectedOperationalPowerPlantsInMarket(market, time)) { //TODO: Change to findOperationalPowerPlantsInMarket, so that the loop later does not need to iterate over all power plants
                double plantMarginalCost = determineExpectedMarginalCost(plant, fuelPrices, co2price);
                marginalCostMap.put(plant, plantMarginalCost);
                capacitySum += plant.getTechnology().getCapacity();
            }
            
            

            MapValueComparator comp = new MapValueComparator(marginalCostMap);
            meritOrder = new TreeMap<PowerPlant, Double>(comp);
            meritOrder.putAll(marginalCostMap);
                
           long numberOfSegments = reps.segmentRepository.count();
            
            double demandFactor = market.getDemandGrowthTrend().getValue(time);
            
            // find expected prices per segment given merit order
            for (SegmentLoad segmentLoad : market.getLoadDurationCurve()) {

                double expectedSegmentLoad = segmentLoad.getBaseLoad() * demandFactor;

                if (expectedSegmentLoad > maxExpectedLoad) {
                    maxExpectedLoad = expectedSegmentLoad;
                }

                double segmentSupply = 0d;
                double segmentPrice = 0d;
                
                for (Entry<PowerPlant, Double> plantCost : meritOrder.entrySet()) {
                    PowerPlant plant = plantCost.getKey();
                    double plantCapacity =0d;
                    // Determine available capacity in the future in this segment
                    plantCapacity = plant.getExpectedAvailableCapacity(time, segmentLoad.getSegment(), numberOfSegments);

                    //logger.warn("Capacity of plant " + plant.toString() + " is " + plantCapacity/plant.getTechnology().getCapacity());
                    if (segmentSupply < expectedSegmentLoad) {
                    	segmentSupply += plantCapacity;
                    	segmentPrice = plantCost.getValue();
                    }
                    
                }
                
                //logger.warn("Segment " + segmentLoad.getSegment().getSegmentID() + " supply equals " + segmentSupply + " and segment demand equals " + expectedSegmentLoad);
                
                if (segmentSupply >= expectedSegmentLoad) {
                	expectedElectricityPricesPerSegment.put(segmentLoad.getSegment(), segmentPrice);
                } else {
                	expectedElectricityPricesPerSegment.put(segmentLoad.getSegment(), market.getValueOfLostLoad());
                }
                
            }
        }
    }
    
}