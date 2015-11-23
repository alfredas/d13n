package d13n.repository;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.tinkerpop.blueprints.pgm.Vertex;
import com.tinkerpop.pipes.Pipe;
import com.tinkerpop.pipes.util.Pipeline;

import d13n.domain.Rels;
import d13n.domain.agent.DecarbonizationAgent;
import d13n.domain.agent.EnergyConsumer;
import d13n.domain.agent.EnergyProducer;
import d13n.domain.contract.Contract;
import d13n.domain.contract.LongTermContract;
import d13n.domain.contract.LongTermContractDuration;
import d13n.domain.contract.LongTermContractOffer;
import d13n.domain.contract.LongTermContractType;
import d13n.domain.gis.Zone;
import d13n.domain.market.Segment;
import d13n.domain.technology.PowerPlant;
import d13n.domain.technology.Substance;

@Repository
public class ContractRepository extends AbstractRepository<Contract> {

    static Logger logger = LoggerFactory.getLogger(ContractRepository.class);

    @Autowired
    Neo4jTemplate template;

    public Iterable<LongTermContract> findLongTermContractsForEnergyProducerActiveAtTime(EnergyProducer energyProducer, long time) {
        Pipe<Vertex, Vertex> contractPipe = new LabeledEdgePipe(Rels.CONTRACT_FROM, LabeledEdgePipe.Step.IN_OUT);
        // filter by time
        Pipe<Vertex, Vertex> pipeline = new Pipeline<Vertex, Vertex>(contractPipe);

        List<LongTermContract> list = new ArrayList<LongTermContract>();
        // Only if current time is between start and finish time
        for (Contract contract : findAllByPipe(energyProducer, pipeline)) {
            if (contract.getStart() <= time && contract.getFinish() >= time) {
                list.add((LongTermContract) contract);
            }
        }
        return list;
    }

    public Iterable<LongTermContract> findLongTermContractsForEnergyProducerForSegmentActiveAtTime(EnergyProducer energyProducer,
            Segment segment, long time) {

        Pipe<Vertex, Vertex> contractPipe = new LabeledEdgePipe(Rels.CONTRACT_FROM, LabeledEdgePipe.Step.IN_OUT);

        Pipe<Vertex, Vertex> pipeline = new Pipeline<Vertex, Vertex>(contractPipe);

        List<LongTermContract> list = new ArrayList<LongTermContract>();
        for (Contract contract : findAllByPipe(energyProducer, pipeline)) {

            // filter by time
            LongTermContract ltc = (LongTermContract) contract;
            if (ltc.getStart() <= time && ltc.getFinish() >= time) {
                if (ltc.getLongTermContractType().getSegments().contains(segment)) {
                    list.add((LongTermContract) contract);
                }
            }
        }
        return list;
    }

    public Iterable<Contract> findLongTermContractsForEnergyConsumerActiveAtTime(EnergyConsumer energyConsumer, long time) {
        Pipe<Vertex, Vertex> contractPipe = new LabeledEdgePipe(Rels.CONTRACT_TO, LabeledEdgePipe.Step.IN_OUT);
        // filter by time
        Pipe<Vertex, Vertex> pipeline = new Pipeline<Vertex, Vertex>(contractPipe);

        List<Contract> list = new ArrayList<Contract>();
        // Only if current time is between start and finish time
        for (Contract contract : findAllByPipe(energyConsumer, pipeline)) {
            if (contract.getStart() <= time && contract.getFinish() >= time) {
                list.add(contract);
            }
        }
        return list;
    }

    public Iterable<LongTermContract> findLongTermContractsForEnergyConsumerForSegmentActiveAtTime(EnergyConsumer consumer,
            Segment segment, long time) {

        List<LongTermContract> list = new ArrayList<LongTermContract>();
        for (Contract contract : findLongTermContractsForEnergyConsumerActiveAtTime(consumer, time)) {

            // filter by time
            LongTermContract ltc = (LongTermContract) contract;
            if (ltc.getStart() <= time && ltc.getFinish() >= time) {
                if (ltc.getLongTermContractType().getSegments().contains(segment)) {
                    list.add((LongTermContract) contract);
                }
            }
        }
        return list;
    }

    public LongTermContract findLongTermContractForPowerPlantActiveAtTime(PowerPlant plant, long time) {

        for (Contract c : findAll()) {
            LongTermContract ltc = (LongTermContract) c;
            // It active
            if (ltc.getStart() <= time && ltc.getFinish() >= time) {
                if (ltc.getUnderlyingPowerPlant().equals(plant)) {
                    return ltc;
                }
            }
        }
        return null;
    }

    public Iterable<LongTermContract> findLongTermContractsForEnergyConsumerForSegmentForZoneActiveAtTime(EnergyConsumer consumer,
            Segment segment, Zone zone, long currentTick) {
        List<LongTermContract> list = new ArrayList<LongTermContract>();
        for (LongTermContract ltc : findLongTermContractsForEnergyConsumerForSegmentActiveAtTime(consumer, segment, currentTick)) {
            if (ltc.getZone().equals(zone)) {
                list.add(ltc);
            }
        }
        return list;
    }

    /**
     * Creates a long term contract
     * 
     * @return
     */
    // TODO not transactional, so make it transactional when used.
    public LongTermContract submitLongTermContractForElectricity(PowerPlant plant, DecarbonizationAgent seller, DecarbonizationAgent buyer,
            Zone zone, double price, double capacity, LongTermContractType longTermContractType, long time,
            LongTermContractDuration duration, boolean signed, Substance mainFuel, double fuelPassThroughFactor,
            double co2PassThroughFactor, double fuelPriceStart, double co2PriceStart) {

        LongTermContract contract = new LongTermContract().persist();
        contract.setUnderlyingPowerPlant(plant);
        contract.setFrom(seller);
        contract.setTo(buyer);
        contract.setZone(zone);
        contract.setPricePerUnit(price);
        contract.setCapacity(capacity);
        contract.setLongTermContractType(longTermContractType);
        contract.setStart(time);
        contract.setFinish(time + duration.getDuration() - 1);
        contract.setDuration(duration);
        contract.setSigned(signed);
        contract.setMainFuel(mainFuel);
        contract.setFuelPassThroughFactor(fuelPassThroughFactor);
        contract.setCo2PassThroughFactor(co2PassThroughFactor);
        contract.setFuelPriceStart(fuelPriceStart);
        contract.setCo2PriceStart(co2PriceStart);
        return contract;
    }

    // TODO not transactional, so make it transactional when used.
    public LongTermContractOffer submitLongTermContractOfferForElectricity(EnergyProducer seller, PowerPlant plant, Zone zone,
            double price, double capacity, LongTermContractType longTermContractType, long time, LongTermContractDuration duration,
            Substance mainFuel, double fuelPassThroughFactor, double co2PassThroughFactor, double fuelPriceStart, double co2PriceStart) {

        LongTermContractOffer offer = new LongTermContractOffer().persist();
        offer.setSeller(seller);
        offer.setUnderlyingPowerPlant(plant);
        offer.setZone(zone);
        offer.setPrice(price);
        offer.setCapacity(capacity);
        offer.setLongTermContractType(longTermContractType);
        offer.setStart(time);
        offer.setDuration(duration);
        offer.setMainFuel(mainFuel);
        offer.setFuelPassThroughFactor(fuelPassThroughFactor);
        offer.setCo2PassThroughFactor(co2PassThroughFactor);
        offer.setFuelPriceStart(fuelPriceStart);
        offer.setCo2PriceStart(co2PriceStart);
        return offer;
    }

    @Transactional
    public void removeOffer(LongTermContractOffer offer) {
        offer.remove();
    }

    @Transactional
    public void removeAllOffers() {
        for (LongTermContractOffer offer : template.repositoryFor(LongTermContractOffer.class).findAll()) {
            offer.remove();
        }
    }

    @Transactional
    public void reassignLongTermContractToNewPowerPlant(LongTermContract longTermContract, PowerPlant plant) {
        longTermContract.setUnderlyingPowerPlant(plant);
    }

}
