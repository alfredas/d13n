package d13n.util;

import java.util.Comparator;

import d13n.domain.market.Bid;


public class BidPriceReverseComparator implements Comparator<Bid> {

    public int compare(Bid bidone, Bid bidtwo) {
        int compare = -1;
        if (bidone.getPrice() < bidtwo.getPrice()) {
            compare = 1;
        } else if (bidone.getPrice() == bidtwo.getPrice()) {
            compare = 0;
        }
        return compare;
    }
}
