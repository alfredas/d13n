package d13n.domain.agent;

import org.springframework.data.neo4j.annotation.NodeEntity;

import agentspring.agent.Agent;
import agentspring.simulation.SimulationParameter;

@NodeEntity
public class EnergyProducer extends DecarbonizationAgent implements Agent {

    private double riskAversion;

    @SimulationParameter(label = "Long-term contract margin", from = 0, to = 1)
    private double longTermContractMargin;

    @SimulationParameter(label = "Long-term contract horizon", from = 0, to = 10)
    private double longTermContractPastTimeHorizon;

    // Investment
    private double investmentCapacityMargin;

    @SimulationParameter(label = "Investment horizon", from = 0, to = 15)
    private int investmentFutureTimeHorizon;
    @SimulationParameter(label = "Equity Interest Rate", from = 0, to = 1)
    private double equityInterestRate;
    private int investmentAlgorithm;
    private double investmentCapitalCostFactor;
    private double downpaymentFractionOfCash;
    @SimulationParameter(label = "Debt ratio in investments", from = 0, to = 1)
    private double debtRatioOfInvestments;

    @SimulationParameter(label = "Discount period", from = 0, to = 30)
    private int discountPeriod;

    private boolean willingToInvest;

    // Loan
    private int loanPreferedType;
    private int loanPreferedNumberOfPlayments;
    @SimulationParameter(label = "Loan Interest Rate", from = 0, to = 1)
    private double loanInterestRate;

    // Dismantling
    private int dismantlingProlongingYearsAfterTechnicalLifetime;
    private double dismantlingRequiredOperatingProfit;
    private long pastTimeHorizon;

    public boolean isWillingToInvest() {
        return willingToInvest;
    }

    public void setWillingToInvest(boolean willingToInvest) {
        this.willingToInvest = willingToInvest;
    }

    public int getDiscountPeriod() {
        return discountPeriod;
    }

    public void setDiscountPeriod(int discountPeriod) {
        this.discountPeriod = discountPeriod;
    }

    public double getDownpaymentFractionOfCash() {
        return downpaymentFractionOfCash;
    }

    public void setDownpaymentFractionOfCash(double downpaymentFractionOfCash) {
        this.downpaymentFractionOfCash = downpaymentFractionOfCash;
    }

    public double getLoanInterestRate() {
        return loanInterestRate;
    }

    public void setLoanInterestRate(double loanInterestRate) {
        this.loanInterestRate = loanInterestRate;
    }

    public double getRiskAversion() {
        return riskAversion;
    }

    public void setRiskAversion(double riskAversion) {
        this.riskAversion = riskAversion;
    }

    public long getPastTimeHorizon() {
        return pastTimeHorizon;
    }

    public void setPastTimeHorizon(long pastTimeHorizon) {
        this.pastTimeHorizon = pastTimeHorizon;
    }

    public int getLoanPreferedType() {
        return loanPreferedType;
    }

    public void setLoanPreferedType(int loanPreferedType) {
        this.loanPreferedType = loanPreferedType;
    }

    public int getLoanPreferedNumberOfPlayments() {
        return loanPreferedNumberOfPlayments;
    }

    public void setLoanPreferedNumberOfPlayments(int loanPreferedNumberOfPlayments) {
        this.loanPreferedNumberOfPlayments = loanPreferedNumberOfPlayments;
    }

    public int getDismantlingProlongingYearsAfterTechnicalLifetime() {
        return dismantlingProlongingYearsAfterTechnicalLifetime;
    }

    public void setDismantlingProlongingYearsAfterTechnicalLifetime(int dismantlingProlongingYearsAfterTechnicalLifetime) {
        this.dismantlingProlongingYearsAfterTechnicalLifetime = dismantlingProlongingYearsAfterTechnicalLifetime;
    }

    public double getDismantlingRequiredOperatingProfit() {
        return dismantlingRequiredOperatingProfit;
    }

    public void setDismantlingRequiredOperatingProfit(double dismantlingRequiredOperatingProfit) {
        this.dismantlingRequiredOperatingProfit = dismantlingRequiredOperatingProfit;
    }

    public double getInvestmentCapacityMargin() {
        return investmentCapacityMargin;
    }

    public void setInvestmentCapacityMargin(double investmentCapacityMargin) {
        this.investmentCapacityMargin = investmentCapacityMargin;
    }

    public int getInvestmentFutureTimeHorizon() {
        return investmentFutureTimeHorizon;
    }

    public void setInvestmentFutureTimeHorizon(int investmentFutureTimeHorizon) {
        this.investmentFutureTimeHorizon = investmentFutureTimeHorizon;
    }

    public int getInvestmentAlgorithm() {
        return investmentAlgorithm;
    }

    public void setInvestmentAlgorithm(int investmentAlgorithm) {
        this.investmentAlgorithm = investmentAlgorithm;
    }

    public double getInvestmentCapitalCostFactor() {
        return investmentCapitalCostFactor;
    }

    public void setInvestmentCapitalCostFactor(double investmentCapitalCostFactor) {
        this.investmentCapitalCostFactor = investmentCapitalCostFactor;
    }

    public double getEquityInterestRate() {
        return equityInterestRate;
    }

    public void setEquityInterestRate(double investmentDiscountRate) {
        this.equityInterestRate = investmentDiscountRate;
    }

    public double getLongTermContractMargin() {
        return longTermContractMargin;
    }

    public void setLongTermContractMargin(double longTermContractMargin) {
        this.longTermContractMargin = longTermContractMargin;
    }

    public double getLongTermContractPastTimeHorizon() {
        return longTermContractPastTimeHorizon;
    }

    public void setLongTermContractPastTimeHorizon(double longTermContractPastTimeHorizon) {
        this.longTermContractPastTimeHorizon = longTermContractPastTimeHorizon;
    }

    public double getDebtRatioOfInvestments() {
        return debtRatioOfInvestments;
    }

    public void setDebtRatioOfInvestments(double debtRatioOfInvestments) {
        this.debtRatioOfInvestments = debtRatioOfInvestments;
    }
}
