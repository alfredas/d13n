package d13n.repository;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.tinkerpop.blueprints.pgm.Vertex;
import com.tinkerpop.pipes.Pipe;
import com.tinkerpop.pipes.sideeffect.CountPipe;

import d13n.domain.Rels;
import d13n.domain.agent.EnergyProducer;
import d13n.domain.contract.Loan;
import d13n.domain.market.ElectricitySpotMarket;
import d13n.domain.technology.PowerGeneratingTechnology;
import d13n.domain.technology.PowerGridNode;
import d13n.domain.technology.PowerPlant;

/**
 * Repository for {PowerPlant}s
 * 
 * @author ejlchappin
 * 
 */
@Repository
public class PowerPlantRepository extends AbstractRepository<PowerPlant> {

	/**
	 * Finds plants by owner.
	 * 
	 * @param owner
	 *            of the plants
	 * @return the list of plants
	 */
	public Iterable<PowerPlant> findPowerPlantsByOwner(EnergyProducer owner) {
		Pipe<Vertex, Vertex> plants = new LabeledEdgePipe(
				Rels.POWERPLANT_OWNER, LabeledEdgePipe.Step.IN_OUT);
		return this.findAllByPipe(owner, plants);
	}

	public long countPowerPlantsByOwner(EnergyProducer owner) {
    	Pipe<Vertex, Vertex> plants = new LabeledEdgePipe(Rels.POWERPLANT_OWNER, LabeledEdgePipe.Step.IN_OUT);
    	CountPipe<Vertex> count = new CountPipe<Vertex>();
    	count.setStarts(plants);

        return count.getSideEffect();
    }

	/**
	 * Finds plants by owner and selects only operational plants.
	 * 
	 * @param owner
	 *            of the plants
	 * @param time
	 *            at which the operationality it is checked
	 * @return the list of plants
	 */
	public Iterable<PowerPlant> findOperationalPowerPlantsByOwner(
			EnergyProducer owner, long time) {
		List<PowerPlant> list = new ArrayList<PowerPlant>();

		// TODO make this a pipe
		for (PowerPlant plant : findPowerPlantsByOwner(owner)) {
			if (plant.isOperational(time)) {
				list.add(plant);
			}
		}
		return list;
	}

	/**
	 * Finds plants by owner and selects only operational plants.
	 * 
	 * @param owner
	 *            of the plants
	 * @param time
	 *            at which the operationality it is checked
	 * @return the list of plants
	 */
	public Iterable<PowerPlant> findOperationalPowerPlantsByTechnology(
			PowerGeneratingTechnology technology, long time) {
		List<PowerPlant> list = new ArrayList<PowerPlant>();
		for (PowerPlant plant : findPowerPlantsByTechnology(technology)) {
			if (plant.isOperational(time)) {
				list.add(plant);
			}
		}
		return list;
	}

	public Iterable<PowerPlant> findPowerPlantsByTechnology(
			PowerGeneratingTechnology technology) {
		Pipe<Vertex, Vertex> plants = new LabeledEdgePipe(Rels.TECHNOLOGY,
				LabeledEdgePipe.Step.IN_OUT);
		return this.findAllByPipe(technology, plants);
	}

	public Iterable<PowerPlant> findPowerPlantsByPowerGridNode(
			PowerGridNode node) {
		Pipe<Vertex, Vertex> plants = new LabeledEdgePipe(Rels.LOCATION,
				LabeledEdgePipe.Step.IN_OUT);
		return this.findAllByPipe(node, plants);
	}

	public Iterable<PowerPlant> findOperationalPowerPlantsByPowerGridNode(
			PowerGridNode node, long time) {
		List<PowerPlant> list = new ArrayList<PowerPlant>();
		for (PowerPlant plant : findPowerPlantsByPowerGridNode(node)) {
			if (plant.isOperational(time)) {
				list.add(plant);
			}
		}
		return list;
	}

	public Iterable<PowerPlant> findPowerPlantsByOwnerAndMarket(
			EnergyProducer owner, ElectricitySpotMarket market) {
		List<PowerPlant> list = new ArrayList<PowerPlant>();
		for (PowerPlant plant : findPowerPlantsByOwner(owner)) {
			// TODO make this a pipe
			if (plant.getLocation().getZone().equals(market.getZone())) {
				list.add(plant);
			}
		}
		return list;
	}

	public Iterable<PowerPlant> findOperationalPowerPlantsInMarket(
			ElectricitySpotMarket market, long time) {
		List<PowerPlant> list = new ArrayList<PowerPlant>();
		for (PowerPlant plant : findAll()) {
			// TODO make this a pipe
			if (plant.getLocation().getZone().equals(market.getZone())) {
				if (plant.isOperational(time)) {
					list.add(plant);
				}
			}
		}
		return list;
	}
	
	public Iterable<PowerPlant> findExpectedOperationalPowerPlantsInMarket(
			ElectricitySpotMarket market, long time) {
		List<PowerPlant> list = new ArrayList<PowerPlant>();
		for (PowerPlant plant : findAll()) {
			// TODO make this a pipe
			if (plant.getLocation().getZone().equals(market.getZone())) {
				if (plant.isExpectedToBeOperational(time)) {
					list.add(plant);
				}
			}
		}
		return list;
	}

	public Iterable<PowerPlant> findPowerPlantsInMarket(
			ElectricitySpotMarket market) {
		List<PowerPlant> list = new ArrayList<PowerPlant>();
		for (PowerPlant plant : findAll()) {
			// TODO make this a pipe
			if (plant.getLocation().getZone().equals(market.getZone())) {
				list.add(plant);
			}
		}
		return list;
	}

	public Iterable<PowerPlant> findOperationalPowerPlantsByOwnerAndMarket(
			EnergyProducer owner, ElectricitySpotMarket market, long time) {
		List<PowerPlant> list = new ArrayList<PowerPlant>();
		for (PowerPlant plant : findPowerPlantsByOwnerAndMarket(owner, market)) {
			if (plant.isOperational(time)) {
				list.add(plant);
			}
		}
		return list;
	}

	public Iterable<PowerPlant> findPowerPlantsByOwnerAndMarketInPipeline(
			EnergyProducer owner, ElectricitySpotMarket market, long time) {
		List<PowerPlant> list = new ArrayList<PowerPlant>();
		for (PowerPlant plant : findPowerPlantsByOwnerAndMarket(owner, market)) {
			if (!plant.isOperational(time)) {
				list.add(plant);
			}
		}
		return list;
	}

	@Transactional
	public void dismantlePowerPlant(PowerPlant plant, long time) {
		plant.setDismantleTime(time);
	}

	@Transactional
	public PowerPlant createPowerPlant(long time,
			EnergyProducer energyProducer, PowerGridNode location,
			PowerGeneratingTechnology technology) {
		PowerPlant plant = new PowerPlant().persist();
		String label = energyProducer.getName() + " - "
				+ technology.getName();
		plant.setName(label);
		plant.setTechnology(technology);
		plant.setOwner(energyProducer);
		plant.setLocation(location);
		plant.setConstructionStartTime(time);
		plant.calculateAndSetActualInvestedCapital(time);
		return plant;
	}

	@Transactional
	public void setLoan(PowerPlant plant, Loan loan) {
		plant.setLoan(loan);
	}
}
