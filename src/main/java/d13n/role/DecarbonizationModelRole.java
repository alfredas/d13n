package d13n.role;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import agentspring.role.AbstractRole;
import agentspring.role.Role;
import agentspring.role.ScriptComponent;
import d13n.domain.agent.CommoditySupplier;
import d13n.domain.agent.DecarbonizationModel;
import d13n.domain.agent.EnergyConsumer;
import d13n.domain.agent.EnergyProducer;
import d13n.domain.market.CommodityMarket;
import d13n.repository.Reps;
import d13n.role.investment.DismantlePowerPlantPastTechnicalLifetimeRole;
import d13n.role.investment.InvestInPowerGenerationTechnologiesRole;
import d13n.role.investment.PayForLoansRole;
import d13n.role.market.ClearCommodityMarketRole;
import d13n.role.market.ClearIterativeCO2AndElectricitySpotMarketTwoCountryRole;
import d13n.role.market.ProcessAcceptedBidsRole;
import d13n.role.market.ReassignPowerPlantsToLongTermElectricityContractsRole;
import d13n.role.market.ReceivePowerRevenuesBasedOnPowerPlantDispatchPlansRole;
import d13n.role.market.SelectLongTermElectricityContractsRole;
import d13n.role.market.SubmitBidsToCommodityMarketRole;
import d13n.role.market.SubmitLongTermElectricityContractsRole;
import d13n.role.market.SubmitOffersToCommodityMarketRole;
import d13n.role.market.SubmitOffersToElectricitySpotMarketRole;
import d13n.role.operating.DetermineFuelMixRole;
import d13n.role.operating.PayCO2AuctionRole;
import d13n.role.operating.PayCO2TaxRole;
import d13n.role.operating.PayOperatingAndMaintainanceCostsRole;

/**
 * Main model role.
 * 
 * @author alfredas, ejlchappin
 * 
 */
@ScriptComponent
public class DecarbonizationModelRole extends AbstractRole<DecarbonizationModel> implements Role<DecarbonizationModel> {

    @Autowired
    private PayCO2TaxRole payCO2TaxRole;
    @Autowired
    private PayCO2AuctionRole payCO2AuctionRole;
    @Autowired
    private InvestInPowerGenerationTechnologiesRole investInPowerGenerationTechnologiesRole;
    @Autowired
    private SubmitOffersToElectricitySpotMarketRole submitOffersToElectricitySpotMarketRole;
    @Autowired
    private ClearCommodityMarketRole clearCommodityMarketRole;
    @Autowired
    private SubmitBidsToCommodityMarketRole submitBidsToCommodityMarketRole;
    @Autowired
    private SubmitOffersToCommodityMarketRole submitOffersToCommodityMarketRole;
    @Autowired
    private SubmitLongTermElectricityContractsRole submitLongTermElectricityContractsRole;
    @Autowired
    private SelectLongTermElectricityContractsRole selectLongTermElectricityContractsRole;
    @Autowired
    private DismantlePowerPlantPastTechnicalLifetimeRole dismantlePowerPlantRole;
    @Autowired
    private ReassignPowerPlantsToLongTermElectricityContractsRole reassignPowerPlantsToLongTermElectricityContractsRole;
    @Autowired
    private ClearIterativeCO2AndElectricitySpotMarketTwoCountryRole clearIterativeCO2AndElectricitySpotMarketTwoCountryRole;
    @Autowired
    private DetermineFuelMixRole determineFuelMixRole;
    @Autowired
    private ReceivePowerRevenuesBasedOnPowerPlantDispatchPlansRole receivePowerRevenuesBasedOnPowerPlantDispatchPlansRole;
    @Autowired
    private ProcessAcceptedBidsRole processAcceptedBidsRole;
    @Autowired
    private PayForLoansRole payForLoansRole;
    @Autowired
    private PayOperatingAndMaintainanceCostsRole payOperatingAndMaintainanceCostsRole;

    @Autowired
    Reps reps;

    /**
     * Main model script. Executes other roles in the right sequence.
     */
    public void act(DecarbonizationModel model) {

        logger.warn("***** STARTING TICK {} *****", getCurrentTick());

        /*
         * Determine fuel mix of power plants
         */
        logger.warn("  1. Determining fuel mix");
        for (EnergyProducer producer : reps.genericRepository.findAllAtRandom(EnergyProducer.class)) {
            producer.act(determineFuelMixRole);
        }

        /*
         * Submit and select long-term electricity contracts
         */
        if (model.isLongTermContractsImplemented()) {
            logger.warn("  2. Submit and select long-term electricity contracts");
            for (EnergyProducer producer : reps.genericRepository.findAllAtRandom(EnergyProducer.class)) {
                producer.act(submitLongTermElectricityContractsRole);
            }

            for (EnergyConsumer consumer : reps.genericRepository.findAllAtRandom(EnergyConsumer.class)) {
                consumer.act(selectLongTermElectricityContractsRole);
            }
        }

        /*
         * Clear electricity spot and CO2 markets and determine also the
         * commitment of powerplants.
         */
        logger.warn("  3. Clearing electricity spot and CO2 markets");
        for (EnergyProducer producer : reps.genericRepository.findAllAtRandom(EnergyProducer.class)) {
            producer.act(submitOffersToElectricitySpotMarketRole);
        }

        model.act(clearIterativeCO2AndElectricitySpotMarketTwoCountryRole);

        for (EnergyProducer producer : reps.genericRepository.findAll(EnergyProducer.class)) {
            producer.act(receivePowerRevenuesBasedOnPowerPlantDispatchPlansRole);
        }

        /*
         * Maintenance and CO2
         */
        logger.warn("  4. Paying for maintenance & co2");
        for (EnergyProducer producer : reps.genericRepository.findAllAtRandom(EnergyProducer.class)) {
            // do accounting
            producer.act(payOperatingAndMaintainanceCostsRole);
            // pay tax
            producer.act(payCO2TaxRole);
            // pay for CO2 auction only if CO2 trading
            if (model.isCo2TradingImplemented()) {
                producer.act(payCO2AuctionRole);
            }
        }

        /*
         * COMMODITY MARKETS
         */
        logger.warn("  5. Purchasing commodities");

        // SUPPLIER (supply for commodity markets)
        for (CommoditySupplier supplier : reps.genericRepository.findAllAtRandom(CommoditySupplier.class)) {
            // 1) first submit the offers
            supplier.act(submitOffersToCommodityMarketRole);
        }

        // PRODUCER (demand for commodity markets)
        for (EnergyProducer producer : reps.genericRepository.findAllAtRandom(EnergyProducer.class)) {
            // 2) submit bids
            producer.act(submitBidsToCommodityMarketRole);
        }

        for (CommodityMarket market : reps.genericRepository.findAllAtRandom(CommodityMarket.class)) {
            market.act(clearCommodityMarketRole);
            market.act(processAcceptedBidsRole);
        }

        logger.warn("  6. Dismantling & paying loans");
        for (EnergyProducer producer : reps.genericRepository.findAllAtRandom(EnergyProducer.class)) {
            producer.act(dismantlePowerPlantRole);
            producer.act(payForLoansRole);
        }
        logger.warn("  7. Investing");
        if (getCurrentTick() > 1) {
            boolean someOneStillWillingToInvest = true;
            while (someOneStillWillingToInvest) {
                someOneStillWillingToInvest = false;
                for (EnergyProducer producer : reps.genericRepository.findAllAtRandom(EnergyProducer.class)) {
                    // invest in new plants
                    if (producer.isWillingToInvest()) {
                        producer.act(investInPowerGenerationTechnologiesRole);
                        someOneStillWillingToInvest = true;
                    }
                }
            }
            resetWillingnessToInvest();
        }
        if (model.isLongTermContractsImplemented()) {
            logger.warn("  8. Reassign LTCs");
            for (EnergyProducer producer : reps.genericRepository.findAllAtRandom(EnergyProducer.class)) {
                producer.act(reassignPowerPlantsToLongTermElectricityContractsRole);
            }

            if (getCurrentTick() >= model.getSimulationLength()) {
                agentspring.simulation.Schedule.getSchedule().stop();
            }
        }
    }

    @Transactional
    private void resetWillingnessToInvest() {
        for (EnergyProducer producer : reps.genericRepository.findAllAtRandom(EnergyProducer.class)) {
            producer.setWillingToInvest(true);
        }
    }
}
