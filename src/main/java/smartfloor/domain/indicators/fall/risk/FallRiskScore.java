package smartfloor.domain.indicators.fall.risk;

import java.time.LocalDateTime;
import java.util.List;
import smartfloor.domain.TimeWindow;
import smartfloor.domain.entities.Footstep;
import smartfloor.domain.entities.fall.risk.profile.FallRiskProfile;
import smartfloor.domain.entities.fall.risk.profile.FallRiskScoreAssessment;
import smartfloor.domain.indicators.Indicator;

/**
 * The fall risk total is calculated on a 28 point scale, but converted to a 10 point scale in our client applications.
 * To avoid the conversion on the frontend, we do it here instead.
 */
public final class FallRiskScore implements Indicator {

    private static final double UPPER_BOUND_TINETTI_SCORE = 28;
    private static final double UPPER_BOUND_FRP_SCORE = 10;
    private static final double BOTTOM_BOUND_FRP_SCORE = 0;
    private static final double WALKING_SPEED_COEFFICIENT = 29.940;
    private static final double STRIDE_LENGTH_COEFFICIENT = -10.845;
    private static final double STRIDE_FREQUENCY_COEFFICIENT = -7.402;
    private static final double Y_INTERCEPT = 17.133;

    private Double value;
    private final String unit = "SF-VRP";

    private FallRiskScore() {
    }

    private FallRiskScore(List<Footstep> footsteps) {
        this.value = compute(footsteps);
    }

    private FallRiskScore(List<Footstep> footsteps, LocalDateTime beginTime, LocalDateTime endTime) {
        this.value = computeWithCustomTimeWindow(footsteps, beginTime, endTime);
    }

    private FallRiskScore(Double total) {
        this.value = convertTotalToTenPointScale(total);
    }

    public static FallRiskScore of(List<Footstep> footsteps) {
        return new FallRiskScore(footsteps);
    }

    public static FallRiskScore of(List<Footstep> footsteps, LocalDateTime beginTime, LocalDateTime endTime) {
        return new FallRiskScore(footsteps, beginTime, endTime);
    }

    /**
     * Constructs a FallRiskScore (FRS) using a FallRiskProfile's indicators to compute a total score.
     */
    public static FallRiskScore ofFallRiskProfile(FallRiskProfile fallRiskProfile) {
        Double walkingSpeed = fallRiskProfile.getWalkingSpeed();
        Double stepLength = fallRiskProfile.getStepLength();
        Double stepFrequency = fallRiskProfile.getStepFrequency();

        if ((walkingSpeed == null) || (stepLength == null) || (stepFrequency == null)) {
            return new FallRiskScore((double) 0);
        } else {
            return new FallRiskScore(WALKING_SPEED_COEFFICIENT * convertToMeters(fallRiskProfile.getWalkingSpeed()) +
                    STRIDE_LENGTH_COEFFICIENT * convertToMeters(fallRiskProfile.getStepLength()) +
                    STRIDE_FREQUENCY_COEFFICIENT * fallRiskProfile.getStepFrequency() +
                    Y_INTERCEPT);
        }
    }

    private static Double convertToMeters(Double mm) {
        return mm / 1000;
    }

    private Double compute(List<Footstep> footsteps) {
        Double frpTotal = new FallRiskScoreAssessment(FallRiskProfile.fromFootsteps(footsteps))
                .getTotal();
        return convertTotalToTenPointScale(frpTotal);
    }

    private Double computeWithCustomTimeWindow(
            List<Footstep> footsteps,
            LocalDateTime beginTime,
            LocalDateTime endTime
    ) {
        Double frpTotal = new FallRiskScoreAssessment(
                FallRiskProfile.fromFootstepsAndTimeWindow(
                        footsteps,
                        new TimeWindow(beginTime, endTime)
                )
        ).getTotal();
        return convertTotalToTenPointScale(frpTotal);
    }

    private Double convertTotalToTenPointScale(Double total) {
        if (total > UPPER_BOUND_TINETTI_SCORE) {
            return UPPER_BOUND_FRP_SCORE;
        }
        if (total <= BOTTOM_BOUND_FRP_SCORE) {
            return BOTTOM_BOUND_FRP_SCORE;
        }
        return (double) Math.round(((total / UPPER_BOUND_TINETTI_SCORE) * UPPER_BOUND_FRP_SCORE) * 100) / 100;
    }

    @Override
    public Number getValue() {
        return this.value;
    }

    @Override
    public String getUnit() {
        return unit;
    }
}
