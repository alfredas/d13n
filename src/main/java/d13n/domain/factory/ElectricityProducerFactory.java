package d13n.domain.factory;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.annotation.Transactional;

import d13n.domain.agent.EnergyProducer;
import d13n.domain.contract.Loan;
import d13n.domain.gis.Zone;
import d13n.domain.market.ElectricitySpotMarket;
import d13n.domain.market.SegmentLoad;
import d13n.domain.technology.PowerGeneratingTechnology;
import d13n.domain.technology.PowerGridNode;
import d13n.domain.technology.PowerPlant;

public class ElectricityProducerFactory implements InitializingBean {

    private double capacityMargin;

    private Map<PowerGeneratingTechnology, Double> portfolioShares = null;

    private Set<PowerGridNode> nodes;

    private ElectricitySpotMarket market;

    private List<EnergyProducer> producers;

    static final Logger logger = LoggerFactory.getLogger(ElectricityProducerFactory.class);

    @Override
    public void afterPropertiesSet() throws Exception {
        createPowerPlantsForMarket(market);
    }

    // TODO should this not go to repository?
    @Transactional
    private PowerPlant createPowerPlant(PowerGeneratingTechnology technology, EnergyProducer energyProducer, PowerGridNode location) {
        PowerPlant plant = new PowerPlant().persist();
        String label = energyProducer.getName() + " - " + technology.getName();
        plant.setName(label);
        plant.setTechnology(technology);
        plant.setOwner(energyProducer);
        plant.setLocation(location);
        plant.setConstructionStartTime(-(technology.getExpectedLeadtime() + technology.getExpectedPermittime() + Math.round((Math.random() * technology
                .getExpectedLifetime())))); // TODO: Why include expected lead
                                            // time and permit time? Wouldn't it
                                            // be realistic to have some PP in
                                            // the pipeline at the start?
        plant.calculateAndSetActualInvestedCapital(plant.getConstructionStartTime());
        Loan loan = new Loan().persist();
        loan.setFrom(energyProducer);
        loan.setTo(null);
        double amountPerPayment = determineLoanAnnuities(plant.getActualInvestedCapital() * energyProducer.getDebtRatioOfInvestments(),
                plant.getTechnology().getDepreciationTime(), energyProducer.getLoanInterestRate());
        loan.setAmountPerPayment(amountPerPayment);
        loan.setTotalNumberOfPayments(plant.getTechnology().getDepreciationTime());
        loan.setLoanStartTime(plant.getConstructionStartTime());
        loan.setNumberOfPaymentsDone(-plant.getConstructionStartTime());// Some
                                                                        // payments
                                                                        // are
                                                                        // already
                                                                        // made
        plant.setLoan(loan);
        return plant;
    }

    private void createPowerPlantsForMarket(ElectricitySpotMarket market) {

        double maxLoad = Double.MIN_NORMAL;
        // get max load
        for (SegmentLoad segmentLoad : market.getLoadDurationCurve()) {

            if (maxLoad < segmentLoad.getBaseLoad()) {
                maxLoad = segmentLoad.getBaseLoad();
            }
        }
        double requiredCapacity = maxLoad * (1 + capacityMargin);
        logger.info("required capacity for market {} is {}", market, requiredCapacity);
        for (PowerGeneratingTechnology technology : portfolioShares.keySet()) {
            double pctValue = portfolioShares.get(technology);
            double requiredCapacityForTechnology = pctValue * requiredCapacity;
            logger.info("required capacity within this market for technology {} is {}", technology, requiredCapacityForTechnology);
            // logger.info("required capacity: {} for technology {} before creating",
            // requiredCapacityForTechnology, technology);
            while (requiredCapacityForTechnology > 0) {
                EnergyProducer energyProducer = getRandomProducer(producers);
                PowerPlant plant = createPowerPlant(technology, energyProducer, getNodeForZone(market.getZone()));
                requiredCapacityForTechnology -= plant.getAvailableCapacity(0);
            }
            // logger.info("required capacity: {} for technology {} after creating",
            // requiredCapacityForTechnology, technology);
        }

    }

    private EnergyProducer getRandomProducer(List<EnergyProducer> producers) {
        if (producers.size() > 0) {
            int size = producers.size();
            int index = getRandomIndexFromList(size);
            return producers.get(index);
        }
        return null;
    }

    private int getRandomIndexFromList(int size) {
        return (int) Math.min(Math.floor(Math.random() * size), size - 1);
    }

    private PowerGridNode getNodeForZone(Zone zone) {
        for (PowerGridNode node : nodes) {
            if (node.getZone().equals(zone)) {
                return node;
            }
        }
        return null;
    }

    public double getCapacityMargin() {
        return capacityMargin;
    }

    public void setCapacityMargin(double capacityMargin) {
        this.capacityMargin = capacityMargin;
    }

    public Map<PowerGeneratingTechnology, Double> getPortfolioShares() {
        return portfolioShares;
    }

    public void setPortfolioShares(Map<PowerGeneratingTechnology, Double> portfolioShares) {
        this.portfolioShares = portfolioShares;
    }

    public Set<PowerGridNode> getNodes() {
        return nodes;
    }

    public void setNodes(Set<PowerGridNode> nodes) {
        this.nodes = nodes;
    }

    public ElectricitySpotMarket getMarket() {
        return market;
    }

    public void setMarket(ElectricitySpotMarket market) {
        logger.info("setting market {}", market);
        this.market = market;
    }

    public List<EnergyProducer> getProducers() {
        return producers;
    }

    public void setProducers(List<EnergyProducer> producers) {
        this.producers = producers;
    }

    public double determineLoanAnnuities(double totalLoan, double payBackTime, double interestRate) {

        double q = 1 + interestRate;
        double annuity = totalLoan * (Math.pow(q, payBackTime) * (q - 1)) / (Math.pow(q, payBackTime) - 1);

        return annuity;
    }

}
