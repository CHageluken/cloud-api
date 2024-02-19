package smartfloor.domain.entities.fall.risk.profile;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

/**
 * <p>Wrapper object for representing the fall risk statistics that will be communicated back to the user when viewing
 * a sessions fall risk. This particular fall risk assessment is based on a linear regression model.
 * In this model, we try to "predict" (y) the Tinetti score (see Tinetti gait assessment test) given the values (x) for
 * the relevant features/indicators walking speed, step length and step frequency.</p>
 * TODO: Currently only supports walking speed (instead of using RMS of the vertical acceleration signal as a proxy val)
 * We need to decide on when to incorporate RMS since currently it only makes sense in a situation where we would have
 * walking speed anyway.
 */
public class FallRiskScoreAssessment implements FallRiskAssessmentModel {

    /**
     * <p>Coefficients for the fall risk parameters (indicators) obtained from the LR model.</p>
     * Note: the coefficients for walking speed and stride length are assuming meters/second (m/s) while the
     * distance on the floor (and hence between steps/strides) is measured in millimeters (mm). Therefore, we
     * do a unit conversion on these indicators (walking speed, stride length) before multiplying with these
     * coefficients.
     * We could also have converted the coefficients itself but the small floating-point values we then get seem less
     * intuitive/interpretable.
     */
    private static final double WALKING_SPEED_COEFFICIENT = 29.940;
    private static final double STRIDE_LENGTH_COEFFICIENT = -10.845;
    private static final double STRIDE_FREQUENCY_COEFFICIENT = -7.402;

    /**
     * Y-intercept (y = "predicted" Tinetti score) obtained from the LR model.
     */
    private static final double Y_INTERCEPT = 17.133;

    private FallRiskProfile fallRiskProfile;

    /**
     * In this model, the total FRP metric consists of a "predicted" Tinetti score in the range [0,28].
     */
    private double total;

    @JsonCreator
    private FallRiskScoreAssessment(
            @JsonProperty("fallRiskProfile") FallRiskProfile fallRiskProfile,
            @JsonProperty("total") double total
    ) {
        this.fallRiskProfile = fallRiskProfile;
        this.total = total;
    }

    public FallRiskScoreAssessment(FallRiskProfile fallRiskProfile) {
        this.fallRiskProfile = fallRiskProfile;
        compute();
    }

    /**
     * For unit conversion of the relevant indicators (walking speed, stride length) before multiplying them with
     * their respective coefficients (weights). See also the documentation on the coefficients themselves.
     *
     * @param mm - the indicator value in millimeters
     * @return the indicator value in meters
     */
    private Double convertToMeters(Double mm) {
        return mm / 1000;
    }

    @Override
    public void compute() {
        Double walkingSpeed = fallRiskProfile.getWalkingSpeed();
        Double stepLength = fallRiskProfile.getStepLength();
        Double stepFrequency = fallRiskProfile.getStepFrequency();
        /* TODO: Added as part of #438 (meant to be #441).
            We should eventually, as part of #449, handle the case where an FRP does not have any footsteps
            associated with it. */
        if ((walkingSpeed == null) || (stepLength == null) || (stepFrequency == null)) {
            this.total = 0;
        } else {
            this.total = WALKING_SPEED_COEFFICIENT * convertToMeters(fallRiskProfile.getWalkingSpeed()) +
                    STRIDE_LENGTH_COEFFICIENT * convertToMeters(fallRiskProfile.getStepLength()) +
                    STRIDE_FREQUENCY_COEFFICIENT * fallRiskProfile.getStepFrequency() +
                    Y_INTERCEPT;
        }
    }

    @Override
    public void setFallRiskProfile(FallRiskProfile fallRiskProfile) {
        this.fallRiskProfile = fallRiskProfile;
        compute();
    }

    @Override
    public FallRiskProfile getFallRiskProfile() {
        return fallRiskProfile;
    }

    @Override
    public double getTotal() {
        return total;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FallRiskScoreAssessment that = (FallRiskScoreAssessment) o;
        return Double.compare(that.total, total) == 0 &&
                fallRiskProfile.equals(that.fallRiskProfile);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fallRiskProfile, total);
    }

    @Override
    public String toString() {
        return "FallRiskScoreAssessment{" +
                "fallRiskProfile=" + fallRiskProfile +
                ", total=" + total +
                '}';
    }
}
