package smartfloor.domain.dto.user.measurements;

import java.util.List;
import lombok.Builder;
import org.springframework.validation.annotation.Validated;

/**
 * Corresponds to the `details` field of a Fall incident. This field is used for tracking the possible causes for a
 * fall. A Fall incident might be created with a value of 0, which indicates a "no-fall" record. Such a record is used
 * only for internal analysis. In case of a no-fall measurement, no causes for a fall need to be indicated. To ensure
 * that the details field is not exploited in any way, we forbid adding fall causes to a no-fall measurement. We also
 * validate the provided fall causes in case of an actual fall incident, which has a severity (`value` field) greater
 * than 0 (see how in `UserMeasurementFormDetailsValidator` and `UserMeasurementDetailsValidator`).
 */
@Validated
public final class FallIncidentDetails {
    private List<String> causes;

    public FallIncidentDetails() {
    }

    /**
     * Details for a Fall incident measurement.
     */
    @Builder
    public FallIncidentDetails(List<String> causes) {
        this.causes = causes;
    }

    public List<String> getCauses() {
        return causes;
    }
}
