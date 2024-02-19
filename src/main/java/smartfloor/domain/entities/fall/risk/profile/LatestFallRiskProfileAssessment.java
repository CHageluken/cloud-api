package smartfloor.domain.entities.fall.risk.profile;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.time.LocalDateTime;
import java.util.Objects;
import smartfloor.deserializer.CustomLocalDateTimeDeserializer;
import smartfloor.domain.entities.User;
import smartfloor.serializer.CustomLocalDateTimeSerializer;

/**
 * The latest fall risk assessment is an object that represents a minimal set of relevant information about the
 * latest fall risk assessment of a user. It is used by the FRP part of the Vitality application.
 */
public class LatestFallRiskProfileAssessment {

    private User user;

    @JsonSerialize(using = CustomLocalDateTimeSerializer.class)
    @JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
    private LocalDateTime time;

    private final double total;

    private LatestFallRiskProfileAssessment() {
        this.total = 0.0;
    }

    /**
     * TODO.
     */
    public LatestFallRiskProfileAssessment(User user, FallRiskAssessmentModel assessment) {
        this.user = user;
        this.time = assessment.getFallRiskProfile().getEndTime();
        this.total = assessment.getTotal();
    }

    public User getUser() {
        return user;
    }

    public double getTotal() {
        return total;
    }

    public LocalDateTime getTime() {
        return time;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LatestFallRiskProfileAssessment that = (LatestFallRiskProfileAssessment) o;
        return Double.compare(that.total, total) == 0 && user.equals(that.user) && time.equals(that.time);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, time, total);
    }
}
