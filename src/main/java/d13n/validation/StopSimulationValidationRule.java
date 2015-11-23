package d13n.validation;

import agentspring.validation.AbstractValidationRule;
import agentspring.validation.ValidationException;
import agentspring.validation.ValidationRule;

public class StopSimulationValidationRule extends AbstractValidationRule implements ValidationRule {

    @Override
    public void validate() {
        if (getCurrentTick() >= 50) {
            throw new ValidationException("Reached tick 50. Enough!");
        }
    }

}
