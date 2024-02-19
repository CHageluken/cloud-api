package smartfloor.domain;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import smartfloor.TenantTestBase;
import smartfloor.domain.entities.Wearable;
import smartfloor.domain.entities.fall.risk.profile.FallRiskProfile;
import smartfloor.domain.entities.fall.risk.profile.FallRiskScoreAssessment;

@Tag("UnitTest")
class FallRiskScoreAssessmentTest extends TenantTestBase {

    /**
     * The LR model-obtained coefficients.
     *
     * @see FallRiskScoreAssessment for more information.
     */
    private static final double WALKING_SPEED_COEFFICIENT = 29.940;
    private static final double STRIDE_LENGTH_COEFFICIENT = -10.845;
    private static final double STRIDE_FREQUENCY_COEFFICIENT = -7.402;
    private static final double Y_INTERCEPT = 17.133;

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

    /**
     * This test determines whether we are returned a total (FRP) score based on the three indicators:
     * walking speed, stride length and stride frequency.
     */
    @Test
    void testFallRiskScoreAssessment() {
        // given
        LocalDateTime currentTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);

        Wearable wearable = Wearable.builder().id("test_heelable").build();
        LocalDateTime beginTime = currentTime.minusMinutes(2);
        LocalDateTime endTime = currentTime;
        double walkingSpeed = 400.0; // in mm(/s), the unit in which the indicator is saved after the FRP is computed.
        double strideLength = 500.0; // in mm, the unit in which the indicator is saved after the FRP is computed.
        double strideFrequency = 1.33; // in strides/second
        FallRiskProfile fallRiskProfile = FallRiskProfile.builder()
                .wearable(wearable)
                .beginTime(beginTime)
                .endTime(endTime)
                .walkingSpeed(walkingSpeed)
                .stepLength(strideLength)
                .stepFrequency(strideFrequency)
                .build();
        // when
        FallRiskScoreAssessment fallRiskScoreAssessment = new FallRiskScoreAssessment(fallRiskProfile);
        // then
        assertNotNull(fallRiskScoreAssessment.getFallRiskProfile());
        assertNotEquals(0.0, fallRiskScoreAssessment.getTotal());
    }

    /**
     * This test determines whether the computed total score is within the proper Tinetti score range [0, 28].
     */
    @Test
    void testTotalScoreInCorrectRange() {
        // given
        LocalDateTime currentTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);

        Wearable wearable = Wearable.builder().id("test_heelable").build();
        LocalDateTime beginTime = currentTime.minusMinutes(2);
        LocalDateTime endTime = currentTime;
        double walkingSpeed = 400.0; // in mm(/s), the unit in which the indicator is saved after the FRP is computed.
        double strideLength = 500.0; // in mm, the unit in which the indicator is saved after the FRP is computed.
        double strideFrequency = 1.33; // in strides/second
        FallRiskProfile fallRiskProfile = FallRiskProfile.builder()
                .wearable(wearable)
                .beginTime(beginTime)
                .endTime(endTime)
                .walkingSpeed(walkingSpeed)
                .stepLength(strideLength)
                .stepFrequency(strideFrequency)
                .build();
        // when
        FallRiskScoreAssessment fallRiskScoreAssessment = new FallRiskScoreAssessment(fallRiskProfile);
        // then
        assertTrue(
                fallRiskScoreAssessment.getTotal() >= 0 && fallRiskScoreAssessment.getTotal() <= 28,
                () -> "Total fall risk score is outside correct (Tinetti) score range."
        );
    }

    /**
     * This test is really just there to prevent regressions (the test kind, not the statistical model kind, hehe)
     * from occurring when somebody changes the coefficients themselves. If that person knows what he/she is doing,
     * they can also safely change them in this "safeguard" test. A valid case for changing them could be made when
     * they have been improved (i.e. based on more clients, making the LR model more representative).
     */
    @Test
    void testTotalScoreCorrectness() {
        // given
        LocalDateTime currentTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);

        Wearable wearable = Wearable.builder().id("test_heelable").build();
        LocalDateTime beginTime = currentTime.minusMinutes(2);
        LocalDateTime endTime = currentTime;
        double walkingSpeed = 400.0; // in mm(/s), the unit in which the indicator is saved after the FRP is computed.
        double strideLength = 500.0; // in mm, the unit in which the indicator is saved after the FRP is computed.
        double strideFrequency = 1.33; // in strides/second
        double correctTotalScore = WALKING_SPEED_COEFFICIENT * convertToMeters(walkingSpeed) +
                STRIDE_LENGTH_COEFFICIENT * convertToMeters(strideLength) +
                STRIDE_FREQUENCY_COEFFICIENT * strideFrequency + Y_INTERCEPT;
        FallRiskProfile fallRiskProfile = FallRiskProfile.builder()
                .wearable(wearable)
                .beginTime(beginTime)
                .endTime(endTime)
                .walkingSpeed(walkingSpeed)
                .stepLength(strideLength)
                .stepFrequency(strideFrequency)
                .build();
        // when
        FallRiskScoreAssessment fallRiskScoreAssessment = new FallRiskScoreAssessment(fallRiskProfile);
        // then
        assertEquals(correctTotalScore, fallRiskScoreAssessment.getTotal(), 0.05,
                () -> "Total fall risk score is calculated incorrectly according to total (Tinetti) score coefficients."
        );
    }
}
