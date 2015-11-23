/**
 * 
 */
package d13n.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * @author JoernRichstein
 *
 */
@Repository
public class Reps {
	

    @Autowired
    public GenericRepository genericRepository;

    @Autowired
    public PowerPlantRepository powerPlantRepository;

    @Autowired
    public CashFlowRepository cashFlowRepository;

    @Autowired
    public MarketRepository marketRepository;

    @Autowired
    public ModelRepository modelRepository;

    @Autowired
    public BidRepository bidRepository;

    @Autowired
    public ClearingPointRepository clearingPointRepository;

    @Autowired
    public LoanRepository loanRepository;

    @Autowired
    public PowerPlantDispatchPlanRepository powerPlantDispatchPlanRepository;
    
    @Autowired
    public ContractRepository contractRepository;
    
    @Autowired
    public SegmentLoadRepository segmentLoadRepository;
    
    @Autowired
    public SegmentRepository segmentRepository;
    
    
   

}
