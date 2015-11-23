package d13n.domain.agent;


import org.springframework.data.neo4j.annotation.NodeEntity;

import agentspring.agent.Agent;

@NodeEntity
public class PowerPlantMaintainer extends DecarbonizationAgent implements Agent {

}