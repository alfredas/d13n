package d13n.role;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math.optimization.GoalType;
import org.apache.commons.math.optimization.OptimizationException;
import org.apache.commons.math.optimization.RealPointValuePair;
import org.apache.commons.math.optimization.linear.LinearConstraint;
import org.apache.commons.math.optimization.linear.LinearObjectiveFunction;
import org.apache.commons.math.optimization.linear.Relationship;
import org.apache.commons.math.optimization.linear.SimplexSolver;

import agentspring.role.AbstractRole;
import agentspring.trend.GeometricTrend;
import d13n.domain.agent.CommoditySupplier;
import d13n.domain.agent.EnergyProducer;
import d13n.domain.agent.Government;
import d13n.domain.market.CO2Auction;
import d13n.domain.market.ClearingPoint;
import d13n.domain.market.DecarbonizationMarket;
import d13n.domain.market.ElectricitySpotMarket;
import d13n.domain.market.PowerPlantDispatchPlan;
import d13n.domain.technology.PowerGeneratingTechnology;
import d13n.domain.technology.PowerPlant;
import d13n.domain.technology.Substance;
import d13n.domain.technology.SubstanceShareInFuelMix;
import d13n.repository.Reps;

public abstract class AbstractEnergyProducerRole extends AbstractRole<EnergyProducer> {

    /**
     * Endogenous efficiency modifier: the efficiency of a *new* plant improves
     * by rate of adoption. More capacity installed will result in a higher
     * efficiency at the time of construction.
     * 
     * @param powerPlant
     * @return
     */
    public double calculateEndogenousInvestmentCostModifier(PowerPlant powerPlant) {

        double invModEndo = powerPlant.getTechnology().getInvestmentCostModifierEndogenous();

        double startCap = 0d;

        double currentCap = calculateMarketCapacityEverInstalledUpToGivenTime(powerPlant.getTechnology(), getCurrentTick());
        int i = 0;
        while (startCap == 0 && i <= getCurrentTick()) {
            startCap = calculateMarketCapacityEverInstalledUpToGivenTime(powerPlant.getTechnology(), i);
            i++;
        }

        GeometricTrend trendEndo = new GeometricTrend();
        trendEndo.setGrowthRate(invModEndo);
        trendEndo.setStart(1);
        // Put in -1 here otherwise you'll get a modifier>1 at the start
        double trendEndoValue = trendEndo.getValue((long) (currentCap / startCap) - 1);
        logger.info("Investment endomod value of plant{} is {}", powerPlant, trendEndoValue);
        return trendEndoValue;
    }

    /**
     * Calculates the actual efficiency of a power plant, by using exogenous and
     * endogenous modifiers.
     * 
     * @param powerPlant
     * @return the actual efficiency
     */
    public double calculateEfficiency(PowerPlant powerPlant) {

        double effNorm = powerPlant.getTechnology().getEfficiency();
        double currentEff = effNorm * calculateExogenousEfficiencyModifier(powerPlant) * calculateEndogenousEfficiencyModifier(powerPlant);

        logger.info("Efficiency of plant {} is {}", powerPlant, currentEff);
        if (currentEff > 1) {
            logger.error("Efficiency of plant {} is > 1: {}", powerPlant, currentEff);
            currentEff = 1;
        }
        return currentEff;
    }

    /**
     * Exogenous efficiency modifier: the efficiency of a *new* plant improves
     * over time. Therefore, the construction time determines the actual
     * efficiency of this plant.
     * 
     * @param powerPlant
     *            the PowerPlant that we need the modifier for
     * @return the exogenous modifier
     */
    public double calculateExogenousEfficiencyModifier(PowerPlant powerPlant) {
        double effModExo = powerPlant.getTechnology().getEfficiencyModifierExogenous();

        GeometricTrend trendExo = new GeometricTrend();
        trendExo.setGrowthRate(effModExo);
        trendExo.setStart(1);
        double trendExoValue = trendExo.getValue(powerPlant.getConstructionStartTime());
        logger.info("Efficiency exomod value of plant{} is {}", powerPlant, trendExoValue);

        return trendExoValue;
    }

    /**
     * Endogenous efficiency modifier: the efficiency of a *new* plant improves
     * by rate of adoption. More capacity installed will result in a higher
     * efficiency at the time of construction.
     * 
     * @param powerPlant
     * @return
     */
    public double calculateEndogenousEfficiencyModifier(PowerPlant powerPlant) {

        double effModEndo = powerPlant.getTechnology().getEfficiencyModifierEndogenous();

        double startCap = 0d;

        double currentCap = calculateMarketCapacityEverInstalledUpToGivenTime(powerPlant.getTechnology(), getCurrentTick());
        int i = 0;
        while (startCap == 0 && i <= getCurrentTick()) {
            startCap = calculateMarketCapacityEverInstalledUpToGivenTime(powerPlant.getTechnology(), i);
            i++;
        }

        GeometricTrend trendEndo = new GeometricTrend();
        trendEndo.setGrowthRate(effModEndo);
        trendEndo.setStart(1);
        // Put in -1 here otherwise you'll get a modifier>1 at the start
        double trendEndoValue = trendEndo.getValue((long) (currentCap / startCap) - 1);
        logger.info("Efficiency endomod value of plant{} is {}", powerPlant, trendEndoValue);
        return trendEndoValue;
    }

    public double calculateCO2Intensity(Set<SubstanceShareInFuelMix> fuelMix) {
        double co2Intensity = 0d;
        for (SubstanceShareInFuelMix mix : fuelMix) {
            co2Intensity += mix.getShare() * mix.getSubstance().getCo2Density();
        }
        return co2Intensity;
    }

    public double calculateCO2Intensity(PowerPlant plant) {
        return calculateCO2Intensity(plant.getFuelMix());
    }

    public double calculateCO2EmissionsAtTime(PowerPlant plant, long time) {
        return calculateCO2Intensity(plant) * calculateElectricityOutputAtTime(plant, time);
    }

    public double calculateMarketCapacity(PowerGeneratingTechnology technology, long time) {
        double capacity = 0d;
        for (PowerPlant plant : getReps().powerPlantRepository.findOperationalPowerPlantsByTechnology(technology, time)) {
            capacity += plant.getAvailableCapacity(getCurrentTick());
        }
        logger.info("Capacity for technology {} is {}", technology.getName(), capacity);
        return capacity;
    }

    public double calculateMarketCapacity(ElectricitySpotMarket market, PowerGeneratingTechnology technology, long time) {
        double capacity = 0d;
        for (PowerPlant plant : getReps().powerPlantRepository.findOperationalPowerPlantsByTechnology(technology, time)) {
            if (plant.getLocation().getZone().equals(market.getZone())) {
                capacity += plant.getAvailableCapacity(time);
            }
        }
        logger.info("Capacity for technology {} is {}", technology.getName(), capacity);
        return capacity;
    }

    public double calculateOwnerCapacityOfType(ElectricitySpotMarket market, PowerGeneratingTechnology technology, long time,
            EnergyProducer owner) {
        double capacity = 0d;
        for (PowerPlant plant : getReps().powerPlantRepository.findOperationalPowerPlantsByTechnology(technology, time)) {
            if (plant.getLocation().getZone().equals(market.getZone()) && plant.getOwner().equals(owner)) {
                capacity += plant.getAvailableCapacity(time);
            }
        }
        logger.info("Capacity for technology {} is {}", technology.getName(), capacity);
        return capacity;
    }

    public double calculateTotalOwnerCapacity(ElectricitySpotMarket market, long time, EnergyProducer owner) {
        double capacity = 0d;
        getReps().powerPlantRepository.findOperationalPowerPlantsByOwnerAndMarket((d13n.domain.agent.EnergyProducer) owner, market, time);
        for (PowerPlant plant : getReps().powerPlantRepository.findOperationalPowerPlantsByOwnerAndMarket(owner, market, time)) {
            capacity += plant.getAvailableCapacity(time);
        }
        logger.info("Capacity for owner {} is {}", owner, capacity);
        return capacity;
    }

    public double calculateTotalOwnerCapacityInPipeline(ElectricitySpotMarket market, long time, EnergyProducer owner) {
        double capacity = 0d;
        for (PowerPlant plant : getReps().powerPlantRepository.findPowerPlantsByOwnerAndMarketInPipeline(owner, market, getCurrentTick())) {
            capacity += plant.getAvailableCapacity(time);
        }
        logger.info("Capacity in pipeline for owner {} is {}", owner, capacity);
        return capacity;
    }

    public double calculateMarketCapacityEverInstalledUpToGivenTime(PowerGeneratingTechnology technology, long time) {
        double capacity = 0d;

        for (PowerPlant plant : getReps().powerPlantRepository.findPowerPlantsByTechnology(technology)) {
            if (plant.getConstructionStartTime() <= time) {
                capacity += plant.getAvailableCapacity(getCurrentTick());
            }
        }
        logger.info("Capacity for technology {} is {}", technology.getName(), capacity);
        return capacity;
    }

    public double calculateMarginalCost(PowerPlant powerPlant) {
        double mc = 0d;
        // fuel cost
        mc += calculateMarginalCostExclCO2MarketCost(powerPlant);
        mc += calculateCO2MarketMarginalCost(powerPlant);
        logger.info("Margincal cost for plant {} is {}", powerPlant.getName(), mc);
        return mc;
    }


    public double calculateMarginalCO2Cost(PowerPlant powerPlant) {
        double mc = 0d;
        // fuel cost
        mc += calculateCO2TaxMarginalCost(powerPlant);
        mc += calculateCO2MarketMarginalCost(powerPlant);
        logger.info("Margincal cost for plant {} is {}", powerPlant.getName(), mc);
        return mc;
    }

    public double calculateMarginalCostExclCO2MarketCost(PowerPlant powerPlant) {
        double mc = 0d;
        // fuel cost
        mc += calculateMarginalFuelCost(powerPlant);
        mc += calculateCO2TaxMarginalCost(powerPlant);
        logger.info("Margincal cost excluding CO2 auction/market cost for plant {} is {}", powerPlant.getName(), mc);
        return mc;
    }

    public double calculateMarginalFuelCost(PowerPlant powerPlant) {
        double fc = 0d;
        // fuel cost for each fuel
        for (SubstanceShareInFuelMix mix : powerPlant.getFuelMix()) {

            double amount = mix.getShare();
            logger.info("Calculating need for fuel: {} units of {}", mix.getShare(), mix.getSubstance().getName());
            double fuelPrice = findLastKnownPriceForSubstance(mix.getSubstance());
            fc += amount * fuelPrice;
            logger.info("Calculating marginal cost and found a fuel price which is {} per unit of fuel", fuelPrice);
        }

        return fc;
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
    public double findLastKnownPriceOnMarket(DecarbonizationMarket market) {
        Double average = calculateAverageMarketPriceBasedOnClearingPoints(getReps().clearingPointRepository
                .findClearingPointsForMarketAndTime(market, getCurrentTick()));
        Substance substance = market.getSubstance();

        if (average != null) {
            logger.info("Average price found on market for this tick for {}", substance.getName());
            return average;
        }

        average = calculateAverageMarketPriceBasedOnClearingPoints(getReps().clearingPointRepository
                .findClearingPointsForMarketAndTime(market, getCurrentTick() - 1));
        if (average != null) {
            logger.info("Average price found on market for previous tick for {}", substance.getName());
            return average;
        }

        if (market.getReferencePrice() > 0) {
            logger.info("Found a reference price found for market for {}", substance.getName());
            return market.getReferencePrice();
        }

        for (CommoditySupplier supplier : getReps().genericRepository.findAll(CommoditySupplier.class)) {
            if (supplier.getSubstance().equals(substance)) {

                return supplier.getPriceOfCommodity().getValue(getCurrentTick());
            }
        }

        logger.info("No price has been found for {}", substance.getName());
        return 0d;
    }

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
    public double findLastKnownPriceForSubstance(Substance substance) {

        DecarbonizationMarket market = getReps().marketRepository.findMarketBySubstance(substance);
        if (market == null) {
            logger.warn("No market found for {} so no price can be found", substance.getName());
            return 0d;
        } else {
            return findLastKnownPriceOnMarket(market);
        }
    }

    /**
     * Calculates the volume-weighted average price on a market based on a set
     * of clearingPoints.
     * 
     * @param clearingPoints
     *            the clearingPoints with the volumes and prices
     * @return the weighted average
     */
    private Double calculateAverageMarketPriceBasedOnClearingPoints(Iterable<ClearingPoint> clearingPoints) {
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

    public double calculateCO2MarketMarginalCost(PowerPlant powerPlant) {
        double co2Intensity = calculateCO2Intensity(powerPlant);
        CO2Auction auction = getReps().genericRepository.findFirst(CO2Auction.class);
        double co2Price = findLastKnownPriceOnMarket(auction);
        return co2Intensity * co2Price;
    }

    public double calculateCO2MarketCost(PowerPlant powerPlant) {
        double co2Intensity = calculateCO2Intensity(powerPlant);
        CO2Auction auction = getReps().genericRepository.findFirst(CO2Auction.class);
        double co2Price = findLastKnownPriceOnMarket(auction);
        double electricityOutput = calculateElectricityOutputAtTime(powerPlant, getCurrentTick());
        return co2Intensity * co2Price * electricityOutput;
    }

    public double calculateCO2TaxMarginalCost(PowerPlant powerPlant) {
        double co2Intensity = calculateCO2Intensity(powerPlant);
        Government government = getReps().genericRepository.findFirst(Government.class);
        double co2Tax = government.getCO2Tax(getCurrentTick());
        return co2Intensity * co2Tax;
    }

    public double findLastKnownCO2Price(){
    	Government government = getReps().genericRepository.findFirst(Government.class);
        CO2Auction auction = getReps().genericRepository.findFirst(CO2Auction.class);
        double co2Price = findLastKnownPriceOnMarket(auction);
        double co2Tax = government.getCO2Tax(getCurrentTick());
        return co2Price + co2Tax;
    }
    
    
    public double calculateCO2Tax(PowerPlant powerPlant) {
        double co2Intensity = calculateCO2Intensity(powerPlant);
        logger.info("Calculated a co2 intensity for {} of {}", powerPlant, co2Intensity);
        double electricityOutput = calculateElectricityOutputAtTime(powerPlant, getCurrentTick());
        logger.info("Electricity output for {} of {}", powerPlant, electricityOutput);
        Government government = getReps().genericRepository.findFirst(Government.class);
        double co2Tax = government.getCO2Tax(getCurrentTick());
        logger.info("Tax level for {} of {}", government, co2Tax);
        double taxToPay = (co2Intensity * electricityOutput) * co2Tax;
        logger.info("Calculated a co2 tax for {} of {}", powerPlant, taxToPay);
        return taxToPay;
    }

    public double calculateFixedOperatingCost(PowerPlant powerPlant) {

        double norm = powerPlant.getTechnology().getFixedOperatingCost();
        long timeConstructed = powerPlant.getConstructionStartTime() + powerPlant.calculateActualLeadtime();
        double mod = powerPlant.getTechnology().getFixedOperatingCostModifierAfterLifetime();
        long lifetime = powerPlant.calculateActualLifetime();

        GeometricTrend trend = new GeometricTrend();
        trend.setGrowthRate(mod);
        trend.setStart(norm);

        double currentCost = trend.getValue(getCurrentTick() - (timeConstructed + lifetime));
        return currentCost;
    }

    public double calculateAverageEnergyDensityInOperation(PowerPlant powerPlant) {
        double energyDensity = 0d;
        for (SubstanceShareInFuelMix share : powerPlant.getFuelMix()) {
            energyDensity += share.getSubstance().getEnergyDensity() * share.getShare();
        }
        return energyDensity;
    }

    public double calculateAveragePastOperatingProfit(PowerPlant pp, long horizon) {

        double averageFractionInMerit = 0d;
        for (long i = -horizon; i <= 0; i++) {
            averageFractionInMerit += calculatePastOperatingProfitInclFixedOMCost(pp, getCurrentTick() + i) / i;
        }
        return averageFractionInMerit;
    }

    public double calculatePastOperatingProfitInclFixedOMCost(PowerPlant plant, long time) {
        double pastOP = 0d;
        // TODO get all accepted supply bids and calculate income
        // TODO get all accepted demand bids and calculate costs
        // TODO get the CO2 cost
        // TODO get the fixed cost
        pastOP += calculateFixedOperatingCost(plant);
        return pastOP;
    }

    public double calculateElectricityOutputAtTime(PowerPlant plant, long time) {
    	//TODO This is in MWh (so hours of segment included!!) 
        double amount = 0d;
        for (PowerPlantDispatchPlan electricityPlan : getReps().powerPlantDispatchPlanRepository.findPowerPlantDispatchPlansForPowerPlantForTime(plant, time)) {
        	amount += electricityPlan.getSegment().getHours() * (electricityPlan.getCapacityLongTermContract() + electricityPlan.getCapacitySpotMarket());
        }
        return amount;
    }

    /**
     * The fuel mix is calculated with a linear optimization model of the
     * possible fuels and the requirements.
     * 
     * @param substancePriceMap
     *            contains the possible fuels and their market prices
     * @param minimumFuelMixQuality
     *            is the minimum fuel quality needed for the power plant to work
     * @param efficiency
     *            of the plant determines the need for fuel per MWhe
     * @param co2TaxLevel
     *            is part of the cost for CO2
     * @param co2AuctionPrice
     *            is part of the cost for CO2
     * @return the fuel mix
     */
    public Set<SubstanceShareInFuelMix> calculateFuelMix(Map<Substance, Double> substancePriceMap, double minimumFuelMixQuality,
            double efficiency, double co2Price) {
        int numberOfFuels = substancePriceMap.size();
        int numberOfVariables = numberOfFuels + 1;

        if (numberOfFuels == 0) {
            logger.info("No fuels, so no operation mode is set. Empty fuel mix is returned");
            return new HashSet<SubstanceShareInFuelMix>();
        }

        logger.info("The effiency: {}", efficiency);
        double[] fuelAndCO2Costs = new double[numberOfVariables];
        double[] fuelDensities = new double[numberOfVariables];
        double[] fuelPurities = new double[numberOfVariables];
        double[] qualityfactors = new double[numberOfVariables];
        for (int i = 0; i < numberOfFuels; i++) {
            qualityfactors[i] = 1;
        }
        qualityfactors[numberOfFuels] = -1.0 / minimumFuelMixQuality;

        int i = 0;
        for (Substance substance : substancePriceMap.keySet()) {
            fuelAndCO2Costs[i] = substancePriceMap.get(substance) + substance.getCo2Density() * (co2Price);
            fuelDensities[i] = substance.getEnergyDensity();
            fuelPurities[i] = substance.getQuality();
            i++;
        }

        fuelAndCO2Costs[numberOfFuels] = 0;
        fuelDensities[numberOfFuels] = 0;
        fuelPurities[numberOfFuels] = -1;

        logger.info("Fuel prices: {}", fuelAndCO2Costs);
        logger.info("Fuel densities: {}", fuelDensities);
        logger.info("Fuel purities: {}", fuelPurities);

        // Objective function = minimize fuel cost (fuel consumption*fuelprices
        // + CO2 intensity*co2 price/tax)
        LinearObjectiveFunction function = new LinearObjectiveFunction(fuelAndCO2Costs, 0d);

        List<LinearConstraint> constraints = new ArrayList<LinearConstraint>();

        // Constraint 1: total fuel density * fuel consumption should match
        // required energy input
        constraints.add(new LinearConstraint(fuelDensities, Relationship.EQ, (1 / efficiency)));

        // Constraint 2&3: minimum fuel quality (times fuel consumption) required
        constraints.add(new LinearConstraint(fuelPurities, Relationship.EQ, 0));
        constraints.add(new LinearConstraint(qualityfactors, Relationship.EQ, 0));

        try {
            SimplexSolver solver = new SimplexSolver();
            RealPointValuePair solution = solver.optimize(function, constraints, GoalType.MINIMIZE, true);

            logger.info("Succesfully solved a linear optimization for fuel mix");
            Set<SubstanceShareInFuelMix> fuelMix = new HashSet<SubstanceShareInFuelMix>();

            int f = 0;
            for (Substance substance : substancePriceMap.keySet()) {
                double share = solution.getPoint()[f];
                SubstanceShareInFuelMix ssifm = new SubstanceShareInFuelMix().persist();

                double fuelConsumptionPerMWhElectricityProduced = convertFuelShareToMassVolume(share);
                logger.info("Setting fuel consumption for {} to {}", substance.getName(), fuelConsumptionPerMWhElectricityProduced);
                ssifm.setShare(fuelConsumptionPerMWhElectricityProduced);
                ssifm.setSubstance(substance);
                fuelMix.add(ssifm);
                f++;
            }

            logger.info("If single fired, it would have been: {}",
                    calculateFuelConsumptionWhenOnlyOneFuelIsUsed(substancePriceMap.keySet().iterator().next(), efficiency));
            return fuelMix;
        } catch (OptimizationException e) {
            logger.warn(
                    "Failed to determine the correct fuel mix. Adding only fuel number 1 in fuel mix out of {} substances and minimum quality of {}",
                    substancePriceMap.size(), minimumFuelMixQuality);
            logger.info("The fuel added is: {}", substancePriceMap.keySet().iterator().next().getName());

            Set<SubstanceShareInFuelMix> fuelMix = new HashSet<SubstanceShareInFuelMix>();
            SubstanceShareInFuelMix ssifm = new SubstanceShareInFuelMix().persist();
            Substance substance = substancePriceMap.keySet().iterator().next();

            ssifm.setShare(calculateFuelConsumptionWhenOnlyOneFuelIsUsed(substance, efficiency));
            ssifm.setSubstance(substance);
            logger.info("Setting fuel consumption for {} to {}", ssifm.getSubstance().getName(), ssifm.getShare());
            fuelMix.add(ssifm);
            return fuelMix;
        }
    }

    public double convertFuelShareToMassVolume(double share) {
        return share * 3600;
    }

    public double calculateFuelConsumptionWhenOnlyOneFuelIsUsed(Substance substance, double efficiency) {

        double fuelConsumptionPerMWhElectricityProduced = convertFuelShareToMassVolume(1 / (efficiency * substance.getEnergyDensity()));

        return fuelConsumptionPerMWhElectricityProduced;

    }

    
    
    /**
     * Calculates the actual investment cost of a power plant per year, by using
     * the exogenous modifier.
     * 
     * @param powerPlant
     * @return the actual efficiency
     */
    /*
    public double determineAnnuitizedInvestmentCost(PowerPlant powerPlant, long time) {

        double invNorm = powerPlant.getTechnology().getAnnuitizedInvestmentCost();
        double modifierExo = calculateExogenousModifier(powerPlant.getTechnology().getInvestmentCostModifierExogenous(), time);

        double annuitizedInvestmentCost = invNorm * modifierExo;
        logger.info("Investment cost of plant{} is {}", powerPlant, annuitizedInvestmentCost);
        return annuitizedInvestmentCost;
    }
    
    */
    
    public double determineLoanAnnuities(double totalLoan, double payBackTime, double interestRate) {

    	double q = 1+interestRate;
    	double annuity = totalLoan * (Math.pow(q, payBackTime)*(q-1))/(Math.pow(q, payBackTime)-1);
    	
        return annuity;
    }
    
    /**
     * Calculates the actual investment cost of a power plant per year, by using
     * the exogenous modifier.
     * 
     * @param powerPlant
     * @return the actual efficiency
     */
    /*
    public double determineBaseInvestmentCost(PowerPlant powerPlant, long time){
    	double invNorm = powerPlant.getTechnology().getBaseInvestmentCost();
    	double modifierExo = calculateExogenousModifier(powerPlant.getTechnology().getInvestmentCostModifierExogenous(), time);
    	
    	double baseInvestmentCost = invNorm * modifierExo;
    	logger.info("Investment cost of plant{} is {}", powerPlant, baseInvestmentCost);
    	
    	return baseInvestmentCost;
    }
    */

    /**
     * Calculates the downpayment investment cost of a power plant, by using the
     * exogenous modifier.
     * 
     * @param powerPlant
     * @return the actual efficiency
     */
    /*
    public double determineDownpayment(PowerPlant powerPlant, long time) {

        double invNorm = powerPlant.getTechnology().getDownPayment();
        double modifierExo = calculateExogenousModifier(powerPlant.getTechnology().getInvestmentCostModifierExogenous(), time);

        double annuitizedInvestmentCost = invNorm * modifierExo;
        logger.info("Investment downpayment of plant{} is {}", powerPlant, annuitizedInvestmentCost);
        return annuitizedInvestmentCost;
    }
    */

    /**
     * Exogenous modifier: calculates exogenous improvement.
     * 
     * @param powerPlant
     *            the PowerPlant that we need the modifier for
     * @return the exogenous modifier
     */
    public double calculateExogenousModifier(double modifier, long time) {
        GeometricTrend trendExo = new GeometricTrend();
        trendExo.setGrowthRate(modifier);
        trendExo.setStart(1);
        double trendExoValue = trendExo.getValue(time);
        logger.info("Modifier for {} is for given time {}", modifier, trendExoValue);
        return trendExoValue;
    }

    public abstract Reps getReps();
}
