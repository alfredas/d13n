package d13n.domain.market;

import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import d13n.trend.TriangularTrend;

@NodeEntity
public class ElectricitySpotMarket extends DecarbonizationMarket {

    @RelatedTo(type = "SEGMENT_LOAD", elementClass = SegmentLoad.class, direction = Direction.OUTGOING)
    private Set<SegmentLoad> loadDurationCurve;

    @RelatedTo(type = "DEMANDGROWTH_TREND", elementClass = TriangularTrend.class, direction = Direction.OUTGOING)
    private TriangularTrend demandGrowthTrend;

    private double valueOfLostLoad;

    public Set<SegmentLoad> getLoadDurationCurve() {
        return loadDurationCurve;
    }

    public void setLoadDurationCurve(Set<SegmentLoad> loadDurationCurve) {
        this.loadDurationCurve = loadDurationCurve;
    }

    public double getValueOfLostLoad() {
        return valueOfLostLoad;
    }

    public void setValueOfLostLoad(double valueOfLostLoad) {
        this.valueOfLostLoad = valueOfLostLoad;
    }

    public TriangularTrend getDemandGrowthTrend() {
        return demandGrowthTrend;
    }

    public void setDemandGrowthTrend(TriangularTrend demandGrowthTrend) {
        this.demandGrowthTrend = demandGrowthTrend;
    }

}
