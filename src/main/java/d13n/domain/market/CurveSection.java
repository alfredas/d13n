package d13n.domain.market;

import org.springframework.data.neo4j.annotation.NodeEntity;

@NodeEntity
public class CurveSection {

    private double amount;
    private double price;

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

    @Override
    public String toString() {
        return "amount: " + getAmount() + " price: " + getPrice();
    }

}
