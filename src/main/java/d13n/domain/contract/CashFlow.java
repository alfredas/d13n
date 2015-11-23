package d13n.domain.contract;


import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;
import org.neo4j.graphdb.Direction;

import d13n.domain.agent.DecarbonizationAgent;
import d13n.domain.technology.PowerPlant;

@NodeEntity
public class CashFlow {

    public static int UNCLASSIFIED = 0;
    public static int ELECTRICITY_SPOT = 1;
    public static int ELECTRICITY_LONGTERM = 2;
    public static int FIXEDOMCOST = 3;
    public static int COMMODITY = 4;
    public static int CO2TAX = 5;
    public static int CO2AUCTION = 6;
    public static int LOAN = 7;
    public static int DOWNPAYMENT = 8;
    
    @RelatedTo(type = "FROM_AGENT", elementClass = DecarbonizationAgent.class, direction = Direction.OUTGOING)
    private DecarbonizationAgent from;

    @RelatedTo(type = "TO_AGENT", elementClass = DecarbonizationAgent.class, direction = Direction.OUTGOING)
    private DecarbonizationAgent to;

    @RelatedTo(type = "REGARDING_POWERPLANT", elementClass = PowerPlant.class, direction = Direction.OUTGOING)
    private PowerPlant regardingPowerPlant;

    private int type;
    private double money;
    private long time;

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
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

    public double getMoney() {
        return money;
    }

    public void setMoney(double money) {
        this.money = money;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String toString() {
        return "from " + getFrom() + " to " + getTo() + " type " + getType() + " amount " + getMoney();
    }

    public PowerPlant getRegardingPowerPlant() {
        return regardingPowerPlant;
    }

    public void setRegardingPowerPlant(PowerPlant regardingPowerPlant) {
        this.regardingPowerPlant = regardingPowerPlant;
    }

}
