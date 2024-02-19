package smartfloor.domain.dto;

import lombok.Builder;
import smartfloor.domain.entities.fall.risk.profile.FallRiskProfileRemovalReason;

/**
 * The fall risk profile removal (FRP removal) form contains the values for the features/indicators we deem relevant to
 * describe the removal of an FRP.
 */
public class FallRiskProfileRemovalForm {
    private FallRiskProfileRemovalReason reasonForRemoval;

    /**
     * A field for providing detail when the `reasonForRemoval` is `other`.
     */
    private String specificationOther = "";

    private FallRiskProfileRemovalForm() {
    }

    /**
     * DTO for soft-deleting an FRP.
     */
    @Builder
    public FallRiskProfileRemovalForm(
            FallRiskProfileRemovalReason reasonForRemoval,
            String specificationOther
    ) {
        this.reasonForRemoval = reasonForRemoval;
        this.specificationOther = specificationOther;
    }

    public FallRiskProfileRemovalReason getReasonForRemoval() {
        return reasonForRemoval;
    }

    public void setReasonForRemoval(FallRiskProfileRemovalReason reasonForRemoval) {
        this.reasonForRemoval = reasonForRemoval;
    }

    public String getSpecificationOther() {
        return specificationOther;
    }

    public void setSpecificationOther(String specificationOther) {
        this.specificationOther = specificationOther;
    }
}
