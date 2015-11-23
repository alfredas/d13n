package d13n.domain.market;


import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;
import org.neo4j.graphdb.Direction;

import d13n.domain.agent.DecarbonizationAgent;

@NodeEntity
public class Bid {

    public static int FAILED = -1;
    public static int NOT_SUBMITTED = 0;
    public static int SUBMITTED = 1;
    public static int PARTLY_ACCEPTED = 2;
    public static int ACCEPTED = 3;
    
    @RelatedTo(type = "BID", elementClass = DecarbonizationAgent.class, direction = Direction.INCOMING)
    private DecarbonizationAgent agent;

    @RelatedTo(type = "MARKET", elementClass = DecarbonizationMarket.class, direction = Direction.OUTGOING)
    private DecarbonizationMarket market;

    private double amount;
    private double price;
    private long time;
    private int status;
    private boolean supplyBid;

    public DecarbonizationAgent getAgent() {
        return agent;
    }

    public void setAgent(DecarbonizationAgent agent) {
        this.agent = agent;
    }

    public DecarbonizationMarket getMarket() {
        return market;
    }

    public void setMarket(DecarbonizationMarket market) {
        this.market = market;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public boolean isSupplyBid() {
        return supplyBid;
    }

    public void setSupplyBid(boolean supplyBid) {
        this.supplyBid = supplyBid;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "for " + getMarket() + " price: " + getPrice() + " amount: " + getAmount() + " isSupply: " + isSupplyBid();
    }
}
