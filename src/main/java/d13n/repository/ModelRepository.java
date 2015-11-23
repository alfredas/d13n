package d13n.repository;


import org.springframework.stereotype.Repository;

import d13n.domain.agent.DecarbonizationModel;

/**
 * The repository for the model
 * @author ejlchappin
 *
 */
@Repository
public class ModelRepository extends AbstractRepository<DecarbonizationModel> {

	/**
	 * Finds the model
	 * @return the model
	 */
    public DecarbonizationModel findModel() {
        return super.findAll().iterator().next();
    }

 }
