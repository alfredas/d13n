package d13n.util;

import org.neo4j.graphdb.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.aspects.core.NodeBacked;
import org.springframework.data.neo4j.support.Neo4jTemplate;

import agentspring.facade.Filters;
import agentspring.trend.Trend;

import com.tinkerpop.blueprints.pgm.impls.neo4j.Neo4jVertex;

import d13n.domain.agent.CommoditySupplier;
import d13n.domain.contract.LongTermContract;
import d13n.domain.market.ClearingPoint;
import d13n.domain.market.DecarbonizationMarket;
import d13n.domain.technology.PowerPlant;
import d13n.domain.technology.Substance;
import d13n.repository.Reps;
import d13n.role.AbstractEnergyProducerRole;

public class FiltersImpl implements Filters {
    @Autowired
    Neo4jTemplate template;

    @Autowired
    Reps reps;

    private Dummy dummy;

    public void init() {
        dummy = new Dummy();
    }

    public boolean plantIsOperational(Object node, long tick) {
        NodeBacked entity = this.getEntity(node);
        if (!(entity instanceof PowerPlant))
            throw new RuntimeException("Vertex is not a Power plant");
        PowerPlant plant = (PowerPlant) entity;
        return plant.isOperational(tick);
    }

    public boolean plantIsExpectedToBeOperational(Object node, long tick) {
        NodeBacked entity = this.getEntity(node);
        if (!(entity instanceof PowerPlant))
            throw new RuntimeException("Vertex is not a Power plant");
        PowerPlant plant = (PowerPlant) entity;
        return plant.isExpectedToBeOperational(tick);
    }

    public boolean plantIsInPipeline(Object node, long tick) {
        NodeBacked entity = this.getEntity(node);
        if (!(entity instanceof PowerPlant))
            throw new RuntimeException("Vertex is not a Power plant");
        PowerPlant plant = (PowerPlant) entity;
        return plant.isInPipeline(tick);
    }

    public double calculateCO2Emissions(Object node, long tick) {
        NodeBacked entity = this.getEntity(node);
        if (!(entity instanceof PowerPlant))
            throw new RuntimeException("Vertex is not a Power plant");
        PowerPlant plant = (PowerPlant) entity;
        return this.dummy.calculateCO2EmissionsAtTime(plant, tick);
    }

    public double getTrendValue(Object node, long tick) {
        NodeBacked entity = this.getEntity(node);
        if (!(entity instanceof Trend)) {
            throw new RuntimeException("Vertex is not a Trend");
        }
        Trend trend = (Trend) entity;
        return trend.getValue(tick);
    }

    public double findLastKnownPriceOnMarket(Object node, long tick) {
        NodeBacked entity = this.getEntity(node);
        if (!(entity instanceof DecarbonizationMarket)) {
            throw new RuntimeException("Vertex is not a Market");
        }
        DecarbonizationMarket market = (DecarbonizationMarket) entity;

        // Emiliano stuff:
        Double average = calculateAverageMarketPriceBasedOnClearingPoints(reps.clearingPointRepository.findClearingPointsForMarketAndTime(
                market, tick));
        Substance substance = market.getSubstance();

        if (average != null) {
            return average;
        }

        average = calculateAverageMarketPriceBasedOnClearingPoints(reps.clearingPointRepository.findClearingPointsForMarketAndTime(market,
                tick - 1));
        if (average != null) {
            return average;
        }

        if (market.getReferencePrice() > 0) {
            return market.getReferencePrice();
        }

        for (CommoditySupplier supplier : reps.genericRepository.findAll(CommoditySupplier.class)) {
            if (supplier.getSubstance().equals(substance)) {
                return supplier.getPriceOfCommodity().getValue(tick);
            }
        }

        return 0d;
    }

    public double findLastKnownPriceOnMarketInGJ(Object node, long tick) {
        double price = findLastKnownPriceOnMarket(node, tick);

        NodeBacked entity = this.getEntity(node);
        if (!(entity instanceof DecarbonizationMarket)) {
            throw new RuntimeException("Vertex is not a Market");
        }
        DecarbonizationMarket market = (DecarbonizationMarket) entity;

        Substance substance = market.getSubstance();

        return price / substance.getEnergyDensity();
    }

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

    private NodeBacked getEntity(Object node) {
        if (!(node instanceof Neo4jVertex))
            throw new RuntimeException("Object is not neo4j vertex");
        Neo4jVertex vertex = (Neo4jVertex) node;
        Node n = vertex.getRawVertex();
        NodeBacked entity = template.createEntityFromStoredType(n);
        return entity;
    }

    public boolean ltcIsActive(Object node, long tick) {
        NodeBacked entity = this.getEntity(node);
        if (!(entity instanceof LongTermContract))
            throw new RuntimeException("Vertex is not a Power plant");
        LongTermContract contract = (LongTermContract) entity;
        if (contract.getStart() <= tick & contract.getFinish() >= tick) {
            return true;
        } else {
            return false;
        }
    }

    private class Dummy extends AbstractEnergyProducerRole {

        public Reps getReps() {
            return reps;
        }

    }
}
