package smartfloor.domain.entities.fall.risk.profile;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Wrapper object for representing the fall risk statistics that will be communicated back to the user when viewing
 * a sessions fall risk. This particular fall risk assessment is based on the "threshold" model.
 * Here, we consider a number of fall risk groups (LOW, MODERATE, HIGH) in accordance with the Tinetti gait assessment
 * test. Every fall risk feature/indicator (walking speed, step length, step frequency) gets a risk group categorization
 * based on whether they fall below a certain threshold value for a risk group.
 * The threshold values have been determined from earlier analyses conducted with people that have been identified as
 * belonging to either of the three possible fall risk groups.
 */
public class FallRiskThresholdAssessment implements FallRiskAssessmentModel {
    /*
    WALKING SPEED (M/S)
    */
    public static final double HIGH_RISK_MAX_WALKING_SPEED = 0.55;
    public static final double MODERATE_RISK_MAX_WALKING_SPEED = 0.65;
    /*
    STEP FREQUENCY (STEP/S)
    TODO: Rename. Halved thresholds => this is actually stride frequency.
    */
    public static final double HIGH_RISK_MAX_STEP_FREQUENCY = 0.55;
    public static final double MODERATE_RISK_MAX_STEP_FREQUENCY = 0.75;
    /*
    STEP LENGTH (MM)
    TODO: Rename. Doubled thresholds => this is actually stride length.
    */
    public static final int HIGH_RISK_MAX_STEP_LENGTH = 720;
    public static final int MODERATE_RISK_MAX_STEP_LENGTH = 820;
    /*
    RMS OF VERTICAL ACCELERATION (M/S^2)
    */
    public static final double HIGH_RISK_MAX_RMS_VERTICAL_ACCELERATION = 10.2;
    public static final double MODERATE_RISK_MAX_RMS_VERTICAL_ACCELERATION = 10.44;

    private FallRiskGroup total; // combined FRP
    private FallRiskGroup walkingSpeed;
    private FallRiskGroup stepFrequency;
    private FallRiskGroup stepLength;

    private FallRiskGroup rmsVerticalAcceleration;

    @JsonIgnore // TODO: to be implemented
    private FallRiskGroup stepContinuity;
    @JsonIgnore // TODO: to be implemented
    private FallRiskGroup standingBalance;
    @JsonIgnore // TODO: to be implemented
    private FallRiskGroup stepSymmetry;

    /**
     * The fall risk profile contains the "source" (fall risk) parameter values that lead to the assessment.
     */
    private FallRiskProfile fallRiskProfile;

    /**
     * For now, a flag determines whether we use RMS instead of walking speed.
     */
    private static final boolean USE_RMS_INSTEAD_OF_WALKING_SPEED = false;

    public FallRiskThresholdAssessment() {
    }

    /**
     * TODO.
     */
    public FallRiskThresholdAssessment(
            FallRiskGroup total,
            FallRiskGroup walkingSpeed,
            FallRiskGroup stepFrequency,
            FallRiskGroup stepLength
    ) {
        this.total = total;
        this.walkingSpeed = walkingSpeed;
        this.stepFrequency = stepFrequency;
        this.stepLength = stepLength;
    }

    /**
     * TODO.
     */
    public FallRiskThresholdAssessment(
            FallRiskGroup total,
            FallRiskGroup walkingSpeed,
            FallRiskGroup stepFrequency,
            FallRiskGroup stepLength,
            FallRiskGroup rmsVerticalAcceleration
    ) {
        this.total = total;
        this.walkingSpeed = walkingSpeed;
        this.stepFrequency = stepFrequency;
        this.stepLength = stepLength;
        this.rmsVerticalAcceleration = rmsVerticalAcceleration;
    }

    /**
     * Set the fall risk profile for this fall risk assessment and determine the assessment by calling compute().
     */
    public FallRiskThresholdAssessment(FallRiskProfile frp) {
        this.fallRiskProfile = frp;
        compute();
    }

    private void checkWalkingSpeed() {
        this.walkingSpeed = FallRiskGroup.LOW;
        if (fallRiskProfile.getWalkingSpeed() <= MODERATE_RISK_MAX_WALKING_SPEED) {
            this.walkingSpeed = FallRiskGroup.MODERATE;
            if (fallRiskProfile.getWalkingSpeed() <= HIGH_RISK_MAX_WALKING_SPEED) {
                this.walkingSpeed = FallRiskGroup.HIGH;
            }
        }
    }

    private void checkRmsVerticalAcceleration() {
        if (fallRiskProfile.getRmsVerticalAcceleration() != null) {
            this.rmsVerticalAcceleration = FallRiskGroup.LOW;
            if (fallRiskProfile.getRmsVerticalAcceleration() <= MODERATE_RISK_MAX_RMS_VERTICAL_ACCELERATION) {
                this.rmsVerticalAcceleration = FallRiskGroup.MODERATE;
                if (fallRiskProfile.getRmsVerticalAcceleration() <= HIGH_RISK_MAX_RMS_VERTICAL_ACCELERATION) {
                    this.rmsVerticalAcceleration = FallRiskGroup.HIGH;
                }
            }
        }
    }

    private void checkStepLength() {
        this.stepLength = FallRiskGroup.LOW;
        if (fallRiskProfile.getStepLength() <= MODERATE_RISK_MAX_STEP_LENGTH) {
            this.stepLength = FallRiskGroup.MODERATE;
            if (fallRiskProfile.getStepLength() <= HIGH_RISK_MAX_STEP_LENGTH) {
                this.stepLength = FallRiskGroup.HIGH;
            }
        }
    }

    private void checkStepFrequency() {
        this.stepFrequency = FallRiskGroup.LOW;
        if (fallRiskProfile.getStepFrequency() <= MODERATE_RISK_MAX_STEP_FREQUENCY) {
            this.stepFrequency = FallRiskGroup.MODERATE;
            if (fallRiskProfile.getStepFrequency() <= HIGH_RISK_MAX_STEP_FREQUENCY) {
                this.stepFrequency = FallRiskGroup.HIGH;
            }
        }
    }

    @Override
    public void compute() {
        ArrayList<Integer> determinedFallRiskGroups = new ArrayList<>();

        checkWalkingSpeed();
        checkRmsVerticalAcceleration();
        if (USE_RMS_INSTEAD_OF_WALKING_SPEED) {
            determinedFallRiskGroups.add(rmsVerticalAcceleration.level);
        } else {
            determinedFallRiskGroups.add(walkingSpeed.level);
        }

        checkStepLength();
        determinedFallRiskGroups.add(stepLength.level);


        checkStepFrequency();
        determinedFallRiskGroups.add(stepFrequency.level);

        /* NOTE: This approach will only work for odd numbers of indicators (like the 3 we have now). */
        List<Integer> frequencies = Arrays.stream(FallRiskGroup.values())
                .map(frg -> Collections.frequency(determinedFallRiskGroups, frg.level))
                .toList();
        int majorityRiskGroupIndex = frequencies.indexOf(Collections.max(frequencies));

        this.total = FallRiskGroup.values()[majorityRiskGroupIndex];
    }

    /**
     * Return the categorical value of the fall risk group in its numerical representation according to the contract
     * specified by the interface. Note: assumes the client knows what category the value translates to.
     *
     * @return the fall risk group (categorical value represented numerically) determined for the user
     */
    public double getTotal() {
        return total.level;
    }

    public void setTotal(FallRiskGroup total) {
        this.total = total;
    }

    public FallRiskGroup getWalkingSpeedAssessment() {
        return walkingSpeed;
    }

    public void setWalkingSpeed(FallRiskGroup walkingSpeed) {
        this.walkingSpeed = walkingSpeed;
    }

    public FallRiskGroup getStepFrequencyAssessment() {
        return stepFrequency;
    }

    public void setStepFrequency(FallRiskGroup stepFrequency) {
        this.stepFrequency = stepFrequency;
    }

    public FallRiskGroup getStepLengthAssessment() {
        return stepLength;
    }

    public void setStepLength(FallRiskGroup stepLength) {
        this.stepLength = stepLength;
    }

    public FallRiskGroup getRmsVerticalAccelerationAssessment() {
        return rmsVerticalAcceleration;
    }

    public void setRmsVerticalAcceleration(FallRiskGroup rmsVerticalAcceleration) {
        this.rmsVerticalAcceleration = rmsVerticalAcceleration;
    }

    public FallRiskGroup getStepContinuityAssessment() {
        return stepContinuity;
    }

    public void setStepContinuity(FallRiskGroup stepContinuity) {
        this.stepContinuity = stepContinuity;
    }

    public FallRiskGroup getStepSymmetryAssessment() {
        return stepSymmetry;
    }

    public void setStepSymmetry(FallRiskGroup stepSymmetry) {
        this.stepSymmetry = stepSymmetry;
    }

    public FallRiskGroup getStandingBalanceAssessment() {
        return standingBalance;
    }

    public void setStandingBalance(FallRiskGroup standingBalance) {
        this.standingBalance = standingBalance;
    }

    public FallRiskProfile getFallRiskProfile() {
        return fallRiskProfile;
    }

    public void setFallRiskProfile(FallRiskProfile fallRiskProfile) {
        this.fallRiskProfile = fallRiskProfile;
    }
}
