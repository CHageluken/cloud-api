package smartfloor.domain.dto.interventions;

import java.util.List;

/**
 * A form that accepts a list of `InterventionForm`s. That list is used for the creation of one or more Interventions in
 * one go.
 */
public class CreateInterventionsForm {
    private List<InterventionForm> interventions;
    private Long userId;

    public CreateInterventionsForm(List<InterventionForm> interventions, Long userId) {
        this.interventions = interventions;
        this.userId = userId;
    }

    public CreateInterventionsForm() {

    }

    public List<InterventionForm> getInterventions() {
        return interventions;
    }

    public void setInterventions(List<InterventionForm> interventions) {
        this.interventions = interventions;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
