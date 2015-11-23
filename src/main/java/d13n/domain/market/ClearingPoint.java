package d13n.domain.market;

import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;
import org.neo4j.graphdb.Direction;

/**
 * Spot market clearing point
 * 
 * @author alfredas&emile
 * 
 */
@NodeEntity
public class ClearingPoint {

    @RelatedTo(type = "MARKET_POINT", elementClass = DecarbonizationMarket.class, direction = Direction.OUTGOING)
    DecarbonizationMarket abstractMarket;

    private double price;
    private double volume;
    private long time;
    private int iteration;

    public int getIteration() {
        return iteration;
    }

    public void setIteration(int iteration) {
        this.iteration = iteration;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getVolume() {
        return volume;
    }

    public void setVolume(double volume) {
        this.volume = volume;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public DecarbonizationMarket getAbstractMarket() {
        return abstractMarket;
    }

    public void setAbstractMarket(DecarbonizationMarket abstractMarket) {
        this.abstractMarket = abstractMarket;
    }

    public String toString() {
        return " market: " + abstractMarket + ", price " + price + ", volume " + volume + ", time " + time + ", iteration " + iteration;
    }

}
