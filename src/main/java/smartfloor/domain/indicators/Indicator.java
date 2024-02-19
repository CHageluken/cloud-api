package smartfloor.domain.indicators;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import smartfloor.domain.indicators.fall.risk.FallRiskScore;
import smartfloor.domain.indicators.footstep.AverageSpeed;
import smartfloor.domain.indicators.footstep.AverageStrideFrequency;
import smartfloor.domain.indicators.footstep.AverageStrideLength;
import smartfloor.domain.indicators.footstep.CoveredDistance;
import smartfloor.domain.indicators.footstep.FirstToLastStepDistance;
import smartfloor.domain.indicators.footstep.WalkingTime;
import smartfloor.domain.indicators.rehabilitation.TargetDistance;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "name"
)
@JsonSubTypes(
        {
                @JsonSubTypes.Type(value = AverageSpeed.class, name = "AverageSpeed"),
                @JsonSubTypes.Type(value = AverageStrideFrequency.class, name = "AverageStrideFrequency"),
                @JsonSubTypes.Type(value = AverageStrideLength.class, name = "AverageStrideLength"),
                @JsonSubTypes.Type(value = CoveredDistance.class, name = "CoveredDistance"),
                @JsonSubTypes.Type(value = FirstToLastStepDistance.class, name = "FirstToLastStepDistance"),
                @JsonSubTypes.Type(value = WalkingTime.class, name = "WalkingTime"),
                @JsonSubTypes.Type(value = TargetDistance.class, name = "TargetDistance"),
                @JsonSubTypes.Type(value = FallRiskScore.class, name = "FallRiskScore")
        }
)
public interface Indicator {
    default String getName() {
        return this.getClass().getSimpleName();
    }

    Number getValue();

    String getUnit();
}