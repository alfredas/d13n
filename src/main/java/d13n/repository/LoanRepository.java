package d13n.repository;


import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.tinkerpop.blueprints.pgm.Vertex;
import com.tinkerpop.pipes.Pipe;

import d13n.domain.Rels;
import d13n.domain.agent.DecarbonizationAgent;
import d13n.domain.contract.Loan;
import d13n.domain.technology.PowerPlant;


/**
 * Repository for loans
 * @author ejlchappin
 *
 */
@Repository
public class LoanRepository extends AbstractRepository<Loan> {

	/**
	 * Creates a loan
	 * @param from the seller of the loan
	 * @param to the buyer of the loan
	 * @param amount the total amount to be payed
	 * @param numberOfPayments the number of payments
	 * @param loanStartTime the time the loan starts
	 * @param plant the power plant the loan is connected to
	 * @return
	 */
    @Transactional
    public Loan createLoan(DecarbonizationAgent from, DecarbonizationAgent to, double amount, long numberOfPayments, long loanStartTime, PowerPlant plant) {
        Loan loan = new Loan().persist();
        loan.setFrom(from);
        loan.setTo(to);
        loan.setAmountPerPayment(amount);
        loan.setTotalNumberOfPayments(numberOfPayments);
        loan.setRegardingPowerPlant(plant);
        loan.setLoanStartTime(loanStartTime);
        loan.setNumberOfPaymentsDone(0);
        return loan;
    }

    /**
     * Finds all loans that the agent has been lend to by others.  
     * @param agent
     * @return the loans
     */
    public Iterable<Loan> findLoansFromAgent(DecarbonizationAgent agent) {
        Pipe<Vertex, Vertex> loansPipe = new LabeledEdgePipe(Rels.LEND_TO_AGENT, LabeledEdgePipe.Step.IN_OUT);
        return findAllByPipe(agent, loansPipe);
    }
    
    /**
     * Finds all loans that the agent has lend to others
     * @param agent 
     * @return the loans
     */
    public Iterable<Loan> findLoansToAgent(DecarbonizationAgent agent) {
        Pipe<Vertex, Vertex> loansPipe = new LabeledEdgePipe(Rels.LEND_BY_AGENT, LabeledEdgePipe.Step.IN_OUT);
        return findAllByPipe(agent, loansPipe);
    }

}
