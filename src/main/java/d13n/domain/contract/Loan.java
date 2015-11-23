package d13n.domain.contract;


import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;
import org.neo4j.graphdb.Direction;

import d13n.domain.agent.DecarbonizationAgent;
import d13n.domain.technology.PowerPlant;

@NodeEntity
public class Loan {

    @RelatedTo(type = "LEND_TO_AGENT", elementClass = DecarbonizationAgent.class, direction = Direction.OUTGOING)
    private DecarbonizationAgent from;

    @RelatedTo(type = "LEND_BY_AGENT", elementClass = DecarbonizationAgent.class, direction = Direction.OUTGOING)
    private DecarbonizationAgent to;

    @RelatedTo(type = "REGARDING_POWERPLANT", elementClass = PowerPlant.class, direction = Direction.OUTGOING)
    private PowerPlant regardingPowerPlant;

    private double amountPerPayment;
    private long totalNumberOfPayments;
    private long numberOfPaymentsDone;
    private long loanStartTime;

    public long getLoanStartTime() {
        return loanStartTime;
    }

    public void setLoanStartTime(long loanStartTime) {
        this.loanStartTime = loanStartTime;
    }

    public long getTotalNumberOfPayments() {
		return totalNumberOfPayments;
	}

	public double getAmountPerPayment() {
		return amountPerPayment;
	}

	public void setAmountPerPayment(double amountPerPayment) {
		this.amountPerPayment = amountPerPayment;
	}

	public void setTotalNumberOfPayments(long totalNumberOfPayments) {
		this.totalNumberOfPayments = totalNumberOfPayments;
	}

	public long getNumberOfPaymentsDone() {
		return numberOfPaymentsDone;
	}

	public void setNumberOfPaymentsDone(long numberOfPaymentsDone) {
		this.numberOfPaymentsDone = numberOfPaymentsDone;
	}

	public DecarbonizationAgent getFrom() {
        return from;
    }

    public void setFrom(DecarbonizationAgent from) {
        this.from = from;
    }

    public DecarbonizationAgent getTo() {
        return to;
    }

    public void setTo(DecarbonizationAgent to) {
        this.to = to;
    }

    public PowerPlant getRegardingPowerPlant() {
        return regardingPowerPlant;
    }

    public void setRegardingPowerPlant(PowerPlant regardingPowerPlant) {
        this.regardingPowerPlant = regardingPowerPlant;
    }
}
