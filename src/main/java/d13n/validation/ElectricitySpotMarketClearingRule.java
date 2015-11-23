package d13n.validation;


import org.springframework.beans.factory.annotation.Autowired;

import d13n.domain.market.ClearingPoint;
import d13n.domain.market.ElectricitySpotMarket;
import d13n.domain.market.Segment;
import d13n.domain.market.SegmentLoad;
import d13n.repository.GenericRepository;
import agentspring.validation.AbstractValidationRule;
import agentspring.validation.ValidationException;
import agentspring.validation.ValidationRule;
import d13n.repository.ClearingPointRepository;

public class ElectricitySpotMarketClearingRule extends AbstractValidationRule implements ValidationRule {

    @Autowired
    GenericRepository genericRepository;

    @Autowired
    ClearingPointRepository clearingPointRepository;

    @Override
    public void validate() {
        for (ElectricitySpotMarket market : genericRepository.findAll(ElectricitySpotMarket.class)) {
            for (SegmentLoad segmentload : market.getLoadDurationCurve()) {
            	Segment segment = segmentload.getSegment();
                ClearingPoint point = clearingPointRepository
                        .findClearingPointForSegmentAndTime(segment, getCurrentTick());

                if (point == null) {
                    throw new ValidationException(market.toString() + " " + segment.toString() + " failed to clear");
                }
            }
        }
    }

}
