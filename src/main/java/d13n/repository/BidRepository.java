package d13n.repository;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.tinkerpop.blueprints.pgm.Vertex;
import com.tinkerpop.gremlin.pipes.filter.PropertyFilterPipe;
import com.tinkerpop.pipes.Pipe;
import com.tinkerpop.pipes.filter.FilterPipe;
import com.tinkerpop.pipes.util.Pipeline;

import d13n.domain.Rels;
import d13n.domain.agent.DecarbonizationAgent;
import d13n.domain.market.Bid;
import d13n.domain.market.DecarbonizationMarket;


/**
 * Repository for bids
 * @author ejlchappin
 *
 */
@Repository
public class BidRepository extends AbstractRepository<Bid> {

    static Logger logger = LoggerFactory.getLogger(BidRepository.class);

    /**
     * Finds all demand bids for a market for a time
     * @param market 
     * @param time
     * @return the found bids
     */
    public Iterable<Bid> findBidsForMarketForTime(DecarbonizationMarket market, long time) {
        return getBidsForMarketForTime(market, time, false);
    }

    /**
     *  Finds all supply bids for a market for a time
     * @param market
     * @param time
     * @return
     */
    public Iterable<Bid> findOffersForMarketForTime(DecarbonizationMarket market, long time) {
        return getBidsForMarketForTime(market, time, true);
    }
    
    /**
     * Find bids for a market for a time
     * @param market
     * @param time
     * @param isSupply supply or demand bids
     * @return the bids
     */
    private Iterable<Bid> getBidsForMarketForTime(DecarbonizationMarket market, long time, boolean isSupply) {
        Pipe<Vertex, Vertex> bids = new LabeledEdgePipe(Rels.MARKET, LabeledEdgePipe.Step.IN_OUT);
        // filter by time
        Pipe<Vertex, Vertex> timeFilter = new PropertyFilterPipe<Vertex, Long>("time", time, FilterPipe.Filter.EQUAL);
        // create pipeline
        Pipe<Vertex, Vertex> pipeline = new Pipeline<Vertex, Vertex>(bids, timeFilter);
        
        //TODO make this a pipe
        List<Bid> list = new ArrayList<Bid>();
        for (Bid bid : this.findAllByPipe(market, pipeline)) {
            if (bid.isSupplyBid() == isSupply) {
                list.add(bid);
            }
        }
        return list;
    }

    /**
     * Submit bids to a market. 
     * Note: this is not transactional, so when called, it should be transactional there!
     * @param market
     * @param agent 
     * @param time
     * @param isSupply
     * @param price
     * @param amount
     * @return the submitted bid
     */
    public Bid submitBidToMarket(DecarbonizationMarket market, DecarbonizationAgent agent, long time, boolean isSupply, double price,
            double amount) {

        Bid bid = new Bid().persist();
        bid.setMarket(market);
        bid.setAgent(agent);
        bid.setSupplyBid(isSupply);
        bid.setTime(time);
        bid.setPrice(price);
        bid.setAmount(amount);
        bid.setStatus(Bid.SUBMITTED);
        return bid;
    }

    /**
     * Changes the status of a bid
     * @param bid the bid to change
     * @param status the new status
     */
    @Transactional
    public void setBidStatus(Bid bid, int status) {
        bid.setStatus(status);
    }

    /**
     * Changes the amount of a bid
     * @param bid the bid to change
     * @param amount the new amount
     */
    @Transactional
    public void setBidAmount(Bid bid, double amount) {
        bid.setAmount(amount);
    }

}
