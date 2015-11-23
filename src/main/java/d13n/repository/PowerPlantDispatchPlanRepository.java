package d13n.repository;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.tinkerpop.blueprints.pgm.Vertex;
import com.tinkerpop.gremlin.pipes.filter.PropertyFilterPipe;
import com.tinkerpop.pipes.Pipe;
import com.tinkerpop.pipes.filter.FilterPipe;
import com.tinkerpop.pipes.util.Pipeline;

import d13n.domain.Rels;
import d13n.domain.agent.EnergyProducer;
import d13n.domain.market.ElectricitySpotMarket;
import d13n.domain.market.PowerPlantDispatchPlan;
import d13n.domain.market.Segment;
import d13n.domain.technology.PowerPlant;


@Repository
public class PowerPlantDispatchPlanRepository extends
		AbstractRepository<PowerPlantDispatchPlan> {

	public PowerPlantDispatchPlan findPowerPlantDispatchPlanForPowerPlantForSegmentForTime(
			PowerPlant plant, Segment segment, long time) {
		for (PowerPlantDispatchPlan plan : findPowerPlantDispatchPlansForPowerPlantForTime(
				plant, time)) {
			if (plan.getSegment().equals(segment)) {
				return plan;
			}
		}
		return null;
	}

	public Iterable<PowerPlantDispatchPlan> findPowerPlantDispatchPlansForSegmentForTime(
			Segment segment, long time) {

		// get incoming bids
		Pipe<Vertex, Vertex> bids = new LabeledEdgePipe(
				Rels.SEGMENT_DISPATCHPLAN, LabeledEdgePipe.Step.BOTH_BOTH);
		// filter by time
		Pipe<Vertex, Vertex> timeFilter = new PropertyFilterPipe<Vertex, Long>(
				"time", time, FilterPipe.Filter.EQUAL);
		// create pipeline
		Pipe<Vertex, Vertex> pipeline = new Pipeline<Vertex, Vertex>(bids,
				timeFilter);
		return this.findAllByPipe(segment, pipeline);
	}

	public Iterable<PowerPlantDispatchPlan> findPowerPlantDispatchPlansForTime(
			long time) {
		List<PowerPlantDispatchPlan> list = new ArrayList<PowerPlantDispatchPlan>();
		for (PowerPlantDispatchPlan plan : findAll()) {
			if (plan.getTime() == time) {
				list.add(plan);
			}
		}
		return list;
	}

	public Iterable<PowerPlantDispatchPlan> findPowerPlantDispatchPlansForPowerPlantForTime(
			PowerPlant powerPlant, long time) {
		Pipe<Vertex, Vertex> bids = new LabeledEdgePipe(
				Rels.POWERPLANT_DISPATCHPLAN, LabeledEdgePipe.Step.BOTH_BOTH);
		// filter by time
		Pipe<Vertex, Vertex> timeFilter = new PropertyFilterPipe<Vertex, Long>(
				"time", time, FilterPipe.Filter.EQUAL);
		// create pipeline
		Pipe<Vertex, Vertex> pipeline = new Pipeline<Vertex, Vertex>(bids,
				timeFilter);

		return this.findAllByPipe(powerPlant, pipeline);
	}

	public Iterable<PowerPlantDispatchPlan> findPowerPlantDispatchPlansForEnergyProducerForTime(
			EnergyProducer energyProducer, long time) {
		Pipe<Vertex, Vertex> bids = new LabeledEdgePipe(Rels.SELLING_AGENT,
				LabeledEdgePipe.Step.BOTH_BOTH);
		// filter by time
		Pipe<Vertex, Vertex> timeFilter = new PropertyFilterPipe<Vertex, Long>(
				"time", time, FilterPipe.Filter.EQUAL);
		// create pipeline
		Pipe<Vertex, Vertex> pipeline = new Pipeline<Vertex, Vertex>(bids,
				timeFilter);

		return this.findAllByPipe(energyProducer, pipeline);
	}

	@Transactional
	public PowerPlantDispatchPlan submitOrUpdatePowerPlantDispatchPlanForSpotMarket(
			PowerPlant plant, EnergyProducer producer,
			ElectricitySpotMarket market, Segment segment, long time,
			double price, double capacity) {

		// make a new one if it
		PowerPlantDispatchPlan plan = findPowerPlantDispatchPlanForPowerPlantForSegmentForTime(
				plant, segment, time);
		if (plan == null) {
			plan = new PowerPlantDispatchPlan().persist();
			plan.setPowerPlant(plant);
			plan.setSegment(segment);
			plan.setTime(time);

		}
		plan.setSellingAgent(producer);
		plan.setBuyingAgentSpot(market);
		plan.setMarginalCostExclCO2(price);
		plan.setCapacitySpotMarket(capacity);
		plan.setCapacityLongTermContract(0d);
		return null;
	}

	@Transactional
	public void updateCapacityLongTermContract(PowerPlantDispatchPlan plan,
			double capacity) {
		plan.setCapacityLongTermContract(capacity);
//		if(plan.getCapacitySpotMarket() + capacity > plan.getPowerPlant().getTechnology().getCapacity()){
//			logger.warn("PROBLEM: Adding to much ltc capacity to dispatch plan: " + plan);
//		}
	}

	@Transactional
	public void updateCapacitySpotMarket(PowerPlantDispatchPlan plan,
			double capacity) {
		plan.setCapacitySpotMarket(capacity);
//		if(plan.getCapacityLongTermContract() + capacity > plan.getPowerPlant().getTechnology().getCapacity()){
//			logger.warn("PROBLEM: Adding to much spot capacity to dispatch plan: " + plan);
//		}
	}
}
