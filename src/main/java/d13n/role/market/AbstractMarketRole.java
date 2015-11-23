package d13n.role.market;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import agentspring.role.AbstractRole;
import d13n.domain.market.Bid;
import d13n.domain.market.ClearingPoint;
import d13n.domain.market.DecarbonizationMarket;
import d13n.repository.Reps;
import d13n.util.BidPriceComparator;
import d13n.util.BidPriceReverseComparator;
import d13n.util.Utils;

/**
 * Calculates {@link ClearingPoint} for any {@link Market}.
 * 
 * @author <a href="mailto:E.J.L.Chappin@tudelft.nl">Emile Chappin</a>
 */
public abstract class AbstractMarketRole<T extends DecarbonizationMarket> extends AbstractRole<T> {

    public ClearingPoint calculateClearingPoint(DecarbonizationMarket market, Iterable<Bid> supplyBidsIterable,
            Iterable<Bid> demandBidsIterable, long time) {

        List<Bid> supplyBids = Utils.asList(supplyBidsIterable);
        List<Bid> demandBids = Utils.asList(demandBidsIterable);

        logger.info("Number of supply bids: " + supplyBids.size() + " and demand: " + demandBids.size());

        if (supplyBids.size() == 0 || demandBids.size() == 0) {
            logger.info("Either no supply bids or no demand bids - supply: {}; demand: {}", +supplyBids.size(), demandBids.size());
            return null;
        } else {
            logger.info("{} supply bids and {} demand bids present on " + market, supplyBids.size(), demandBids.size());
        }

        double totalSupply = 0d;
        for (Bid bid : supplyBids) {
            totalSupply += bid.getAmount();
        }
        double totalDemand = 0d;
        for (Bid bid : demandBids) {
            totalDemand += bid.getAmount();
        }
        logger.info("Total supply: {} -- total demand: {}", totalSupply, totalDemand);

        Collections.sort(supplyBids, new BidPriceComparator());
        Collections.sort(demandBids, new BidPriceReverseComparator());
        logger.info("Bids sorted on price");

        // TODO check whether there are negative amounts bid. Negative prices
        // may be ok, possibly warn. Negative amounts are not allowed.

        boolean settled = false;
        double price = 0d;
        double amount = 0d;

        double totalSupplyAccepted = 0d;
        double totalDemandAccepted = 0d;
        double lastSupplyPrice = 0d;
        double lastDemandPrice = 0d;

        int bidDemandIndex = 0;
        int bidSupplyIndex = 0;

        boolean done = false;

        // SUPPLY LOOP
        while (!done) {
            boolean supplyBidMet = false;

            if (supplyBids.size() <= bidSupplyIndex) {
                logger.info("No more supply bids");
                // no more supply bids.
                done = true;
                settled = true;

                amount = totalSupplyAccepted;
                if (market.isAuction()) {
                    price = lastDemandPrice;
                } else {
                    price = lastSupplyPrice;
                }

                logger.info("Accepted the last demand bid partly as a final bid");

                // Supply bid is accepted
                double partialAcceptance = totalSupplyAccepted - totalDemandAccepted;

                calculateAndDetermineSharedAcceptance(demandBids, demandBids.get(bidDemandIndex), partialAcceptance);

                totalDemandAccepted += partialAcceptance;

                logger.info("Cleared: partial demand bid, no more supply bids.");

            } else {
                double supplyAmount = supplyBids.get(bidSupplyIndex).getAmount();
                double supplyPrice = supplyBids.get(bidSupplyIndex).getPrice();

                // DEMAND LOOP
                while (!supplyBidMet && !done) {

                    if (demandBids.size() <= bidDemandIndex) {
                        // no more demand bids;
                        logger.info("No more demand bids, settle with accepted demand so far. Maybe a partial supply bid.");
                        done = true;

                        if ((totalDemandAccepted) > (totalSupplyAccepted)) {

                            settled = true;
                            logger.info("Accepted a partial supply bid as a final bid");
                            logger.info("Accepted a demand bid as a final bid");

                            getReps().bidRepository.setBidStatus(demandBids.get(bidDemandIndex - 1), Bid.ACCEPTED);

                            double partialAcceptance = totalDemandAccepted - totalSupplyAccepted;

                            calculateAndDetermineSharedAcceptance(supplyBids, supplyBids.get(bidSupplyIndex), partialAcceptance);

                            logger.info("Supply bid part accepted is " + partialAcceptance);
                            totalSupplyAccepted += partialAcceptance;
                            price = supplyPrice;
                            amount = totalSupplyAccepted;
                            logger.info("Done case 2: partial supply bid.");
                        }

                    } else {
                        double demandAmount = demandBids.get(bidDemandIndex).getAmount();
                        double demandPrice = demandBids.get(bidDemandIndex).getPrice();
                        lastDemandPrice = demandPrice;

                        // logger.info("Demand price: " + demandPrice);

                        // Should this demand bid be accepted? If true, the
                        // demand bid is still above the supply bid on the bid
                        // ladder.

                        if (supplyPrice <= demandPrice) {

                            // Is this demand bid is smaller in amount than the
                            // supply bid including the current one?
                            if ((totalDemandAccepted + demandAmount) < (totalSupplyAccepted + supplyAmount)) {

                                logger.info("Accepted a demand bid");
                                // Demand bid is accepted
                                getReps().bidRepository.setBidStatus(demandBids.get(bidDemandIndex), Bid.ACCEPTED);
                                // lastDemandPrice = demandPrice;
                                totalDemandAccepted += demandAmount;
                                bidDemandIndex++;

                            } else if ((totalDemandAccepted + demandAmount) == (totalSupplyAccepted + supplyAmount)) {

                                logger.info("Accepted both a demand and a supply bid");
                                // Demand and supply bids are accepted
                                logger.info("Bid repository {}", getReps().bidRepository);
                                getReps().bidRepository.setBidStatus(demandBids.get(bidDemandIndex), Bid.ACCEPTED);
                                getReps().bidRepository.setBidStatus(supplyBids.get(bidSupplyIndex), Bid.ACCEPTED);
                                // lastDemandPrice = demandPrice;
                                lastSupplyPrice = supplyPrice;
                                totalDemandAccepted += demandAmount;
                                totalSupplyAccepted += supplyAmount;
                                bidDemandIndex++;
                                bidSupplyIndex++;

                            } else {

                                // this demand bid is larger then the supply
                                // bid, we should check the next supply bid and
                                // leave the demand bid as it is
                                logger.info("Accepted a supply bid");
                                // Supply bid is accepted
                                getReps().bidRepository.setBidStatus(supplyBids.get(bidSupplyIndex), Bid.ACCEPTED);
                                supplyBidMet = true; // We go out the demand
                                                     // loop
                                bidSupplyIndex++;
                                lastSupplyPrice = supplyPrice;
                                totalSupplyAccepted += supplyAmount;
                            }
                        } else {
                            logger.info("Demand curve now below supply curve, so the clearing has been passed");
                            done = true;

                            if (totalDemandAccepted == totalSupplyAccepted) {
                                // We have not settled yet!
                                logger.info("We are in balance. Settle later.");
                            } else {
                                if ((totalDemandAccepted + demandAmount) > (totalSupplyAccepted)) {

                                    settled = true;
                                    // this demand bid is larger then the supply
                                    // bid, we should check the next supply bid
                                    // and leave the demand bid as it is
                                    logger.info("Accepted a partial demand bid as a final bid");
                                    // Supply bid is accepted

                                    getReps().bidRepository.setBidStatus(supplyBids.get(bidSupplyIndex), Bid.FAILED);
                                    // lastDemandPrice = demandPrice;
                                    double partialAcceptance = totalSupplyAccepted - totalDemandAccepted;

                                    calculateAndDetermineSharedAcceptance(demandBids, demandBids.get(bidDemandIndex), partialAcceptance);

                                    logger.info("Demand bid part accepted is " + partialAcceptance);
                                    totalDemandAccepted += partialAcceptance;
                                    price = demandPrice;
                                    amount = totalDemandAccepted;
                                    logger.info("Done case 3: partial demand bid.");
                                }
                            }

                        }

                    }
                    if (done && !settled) {

                        // supply is now larger then demand. We're done
                        // somewhere in
                        // this area. With, without this bid, or something.

                        // Found a price and a demand
                        // logger.info("Done but not yet settled");
                        // logger.info("Total supply: " + totalSupplyAccepted);
                        // logger.info("Total demand: " + totalDemandAccepted);

                        // Case 1. We're exactly on the crossing
                        if (totalDemandAccepted == totalSupplyAccepted) {
                            logger.info("Done case 4: even bid.");
                            amount = totalSupplyAccepted;
                            price = lastSupplyPrice;
                            supplyBidMet = true;
                        }
                    }
                }

            }
        }
        logger.info("Cleared with amount: " + amount + " and price: " + price);

        int iteration = getReps().modelRepository.findModel().getIteration();
        logger.info("Current iteration is: {}", iteration);

        ClearingPoint clearingPoint = getReps().clearingPointRepository.createOrUpdateClearingPoint(market, price, amount, time, iteration);

        // Setting all bids that are still Submitted to Failed
        for (Bid bid : demandBids) {
            if (bid.getStatus() == Bid.SUBMITTED) {
                getReps().bidRepository.setBidStatus(bid, Bid.FAILED);
            }
        }
        for (Bid bid : supplyBids) {
            if (bid.getStatus() == Bid.SUBMITTED) {
                getReps().bidRepository.setBidStatus(bid, Bid.FAILED);
            }
        }
        logger.info("Set other bids to failed");
        return clearingPoint;
    }

    private void calculateAndDetermineSharedAcceptance(List<Bid> bids, Bid bid, double partialAcceptance) {
        // check whether there are more demand bids that match the same
        // price level and make them all accept part
        ArrayList<Bid> bidsThatAreAllToBePartial = getBidsWithSamePrice(bids, bid);
        double ratio = partialAcceptance / getTotalVolumeOfBids(bidsThatAreAllToBePartial);
        for (Bid _bid : bidsThatAreAllToBePartial) {
            getReps().bidRepository.setBidStatus(_bid, Bid.PARTLY_ACCEPTED);
            overrideBidAmount(_bid, ratio);
        }

        logger.info("For " + bidsThatAreAllToBePartial.size() + " demand bid(s) they are partly accepted, total " + partialAcceptance
                + ", ratio: " + ratio);

    }

    private void overrideBidAmount(Bid bid, double ratio) {
        getReps().bidRepository.setBidAmount(bid, bid.getAmount() * ratio);
    }

    private ArrayList<Bid> getBidsWithSamePrice(List<Bid> bids, Bid bidToMatch) {
        logger.info("Finding bids in a list with the same price as some bid");
        ArrayList<Bid> bidsThatMatch = new ArrayList<Bid>();

        for (Bid bid : bids) {
            if (bid.getPrice() == bidToMatch.getPrice()) {
                bidsThatMatch.add(bid);
            }
        }
        return bidsThatMatch;
    }

    private double getTotalVolumeOfBids(List<Bid> bids) {
        logger.info("Calculating volumes of a number of bids");
        double volume = 0d;
        for (Bid bid : bids) {
            volume += bid.getAmount();
        }
        return volume;
    }
//
//    public SegmentClearingPoint calculateSegmentClearingPoint(DecarbonizationMarket market, Iterable<ElectricitySpotBid> supplyBidsIterable,
//            Iterable<ElectricitySpotBid> demandBidsIterable, long time, Segment segment) {
//
//        List<Bid> demandBids = Utils.asDownCastedList(demandBidsIterable);
//        List<Bid> supplyBids = Utils.asDownCastedList(supplyBidsIterable);
//
//        ClearingPoint clearingPoint = calculateClearingPoint(market, supplyBids, demandBids, time);
//
//        if (clearingPoint != null) {
//            SegmentClearingPoint segmentClearingPoint = getClearingPointRepository().createOrUpdateSegmentClearingPoint(segment,
//                    clearingPoint.getAbstractMarket(), clearingPoint.getPrice(), clearingPoint.getVolume(), clearingPoint.getTime(),
//                    clearingPoint.getIteration());
//
//            return segmentClearingPoint;
//        } else {
//            return null;
//        }
//
//    }

    public abstract Reps getReps();


}
