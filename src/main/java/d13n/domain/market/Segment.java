package d13n.domain.market;

import org.springframework.data.neo4j.annotation.NodeEntity;

@NodeEntity
public class Segment {

    private double hours;
    
    private int segmentID;

    public void setHours(double hours) {
        this.hours = hours;
    }

    public double getHours() {
        return hours;
    }

    @Override
    public String toString() {
    	return "hours: " + getHours();
    }

	public int getSegmentID() {
		return segmentID;
	}

	public void setSegmentID(int segmentID) {
		this.segmentID = segmentID;
	}

}
