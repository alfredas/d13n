package d13n.role.investment;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import agentspring.role.AbstractRole;
import agentspring.role.Role;
import agentspring.role.RoleComponent;
import d13n.domain.agent.EnergyProducer;
import d13n.domain.contract.CashFlow;
import d13n.domain.contract.Loan;
import d13n.domain.technology.PowerPlant;
import d13n.repository.Reps;

/**
 * {@link EnergyProducer}s repay their loans
 * 
 * @author alfredas
 * @author emile
 * 
 */
@RoleComponent
public class PayForLoansRole extends AbstractRole<EnergyProducer> implements Role<EnergyProducer> {


    @Autowired
    Reps reps;

    @Transactional
    public void act(EnergyProducer producer) {

        logger.info("Process accepted bids to cash flow now");

        // for (Loan loan : loanRepository.findLoansFromAgent(producer)) {
        for (PowerPlant plant : reps.powerPlantRepository.findPowerPlantsByOwner(producer)) {
            Loan loan = plant.getLoan();
            if (loan != null) {
                logger.info("Found a loan: {}", loan);
                if (loan.getNumberOfPaymentsDone() < loan.getTotalNumberOfPayments()) {

                    double payment = loan.getAmountPerPayment();
                    reps.cashFlowRepository.createCashFlow(producer, loan.getTo(), payment, CashFlow.LOAN, getCurrentTick(),
                            loan.getRegardingPowerPlant());

                    loan.setNumberOfPaymentsDone(loan.getNumberOfPaymentsDone() + 1);

                    logger.info("Paying {} (euro) for loan {}", payment, loan);
                    logger.info("Number of payments done {}, total needed: {}", loan.getNumberOfPaymentsDone(),
                            loan.getTotalNumberOfPayments());
                }
            }
        }

    }
}
