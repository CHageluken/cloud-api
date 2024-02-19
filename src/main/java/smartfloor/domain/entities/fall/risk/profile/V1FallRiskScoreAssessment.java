package smartfloor.domain.entities.fall.risk.profile;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.time.LocalDateTime;
import java.util.List;
import smartfloor.deserializer.CustomLocalDateTimeDeserializer;
import smartfloor.domain.entities.Floor;
import smartfloor.domain.entities.User;
import smartfloor.domain.indicators.Indicator;
import smartfloor.domain.indicators.fall.risk.FallRiskScore;
import smartfloor.domain.indicators.footstep.AverageSpeed;
import smartfloor.domain.indicators.footstep.AverageStrideFrequency;
import smartfloor.domain.indicators.footstep.AverageStrideLength;
import smartfloor.serializer.CustomLocalDateTimeSerializer;

/**
 * <p>A wrapper object for representing fall risk statistics when calling endpoints defined under
 * V1FallRiskAnalysisController.</p>
 * <p>The plan for this V1 class is to take over the usage of FallRiskScoreAssessment by constructing a response
 * body that is similar to that of Rehabilitation analysis V1. For now, we name this class the same way but with a V1
 * prefix, so that we can keep both classes available until all Fall risk analysis endpoints are adapted to utilize
 * this class.</p>
 * For implementation details, see FallRiskScoreAssessment.
 */
public class V1FallRiskScoreAssessment {
    private FallRiskProfile fallRiskProfile;
    private User user;

    // Derived:
    private List<Indicator> indicators;
    private Floor floor;
    private LocalDateTime beginTime;
    private LocalDateTime endTime;

    private V1FallRiskScoreAssessment() {

    }

    /**
     * TODO.
     */
    public V1FallRiskScoreAssessment(User user, FallRiskProfile fallRiskProfile) {
        this.fallRiskProfile = fallRiskProfile;
        this.user = user;
    }

    /**
     * TODO.
     */
    public V1FallRiskScoreAssessment(
            User user,
            List<Indicator> indicators,
            Floor floor,
            LocalDateTime beginTime,
            LocalDateTime endTime
    ) {
        this.user = user;
        this.indicators = indicators;
        this.floor = floor;
        this.beginTime = beginTime;
        this.endTime = endTime;
    }

    /**
     * TODO.
     */
    @JsonProperty("beginTime")
    @JsonSerialize(using = CustomLocalDateTimeSerializer.class)
    @JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
    public LocalDateTime getBeginTime() {
        if (fallRiskProfile != null) {
            return fallRiskProfile.getBeginTime();
        }
        return beginTime;
    }

    /**
     * TODO.
     */
    @JsonProperty("endTime")
    @JsonSerialize(using = CustomLocalDateTimeSerializer.class)
    @JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
    public LocalDateTime getEndTime() {
        if (fallRiskProfile != null) {
            return fallRiskProfile.getEndTime();
        }
        return endTime;
    }

    /**
     * TODO.
     */
    @JsonProperty("floor")
    @JsonIgnoreProperties({"maxX", "maxY", "orientationNorth", "rotation", "viewers"})
    public Floor getFloor() {
        if (fallRiskProfile != null) {
            return fallRiskProfile.getFloor();
        }
        return floor;
    }

    @JsonIgnoreProperties({"authId", "avatar", "archivedAt", "info", "isArchived"})
    public User getUser() {
        return user;
    }

    /**
     * TODO.
     */
    @JsonProperty("indicators")
    public List<Indicator> getIndicators() {
        if (fallRiskProfile != null) {
            FallRiskScore frs = FallRiskScore.ofFallRiskProfile(this.fallRiskProfile);
            AverageSpeed speed = AverageSpeed.of(fallRiskProfile.getWalkingSpeed());
            AverageStrideLength strideLength = AverageStrideLength.of(fallRiskProfile.getStepLength());
            AverageStrideFrequency strideFrequency = AverageStrideFrequency.of(fallRiskProfile.getStepFrequency());
            return List.of(frs, speed, strideLength, strideFrequency);
        }
        return indicators;
    }
}
