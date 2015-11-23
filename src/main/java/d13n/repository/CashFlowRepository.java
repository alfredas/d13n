package d13n.repository;


import org.springframework.stereotype.Repository;

import d13n.domain.agent.DecarbonizationAgent;
import d13n.domain.contract.CashFlow;
import d13n.domain.technology.PowerPlant;

/**
 * Repository for cash flows
 * @author ejlchappin
 *
 */
@Repository
public class CashFlowRepository extends AbstractRepository<CashFlow> {

	/**
	 * Creates cash flow.
	 * Note: this is not transactional, so when called, it should be transactional there!
	 * @param from 
	 * @param to
	 * @param amount the money transfered
	 * @param type what the cashflow is about
	 * @param time the time
	 * @param plant the power plant related to this cash flow
	 * @return the cash flow
	 */
	public CashFlow createCashFlow(DecarbonizationAgent from,
			DecarbonizationAgent to, double amount, int type, long time,
			PowerPlant plant) {
		CashFlow cashFlow = new CashFlow().persist();
		cashFlow.setFrom(from);
		cashFlow.setTo(to);
		cashFlow.setMoney(amount);
		cashFlow.setType(type);
		cashFlow.setTime(time);
		cashFlow.setRegardingPowerPlant(plant);
		from.setCash(from.getCash() - amount);
		if (to != null) {
			to.setCash(to.getCash() + amount);
		}

		return cashFlow;
	}

}
