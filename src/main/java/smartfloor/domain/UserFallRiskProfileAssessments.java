package smartfloor.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import smartfloor.domain.entities.User;
import smartfloor.domain.entities.fall.risk.profile.FallRiskAssessmentModel;

public final class UserFallRiskProfileAssessments {

    @JsonIgnoreProperties({"authId", "archivedAt", "info", "isArchived"})
    private final User user;
    private final List<FallRiskAssessmentModel> assessments;

    public UserFallRiskProfileAssessments(User user, List<FallRiskAssessmentModel> assessments) {
        this.user = user;
        this.assessments = Collections.unmodifiableList(assessments);
    }

    /**
     * TODO.
     */
    public static List<UserFallRiskProfileAssessments> of(Map<User, List<FallRiskAssessmentModel>> map) {
        return map.entrySet().stream()
                .map(entry -> new UserFallRiskProfileAssessments(entry.getKey(), entry.getValue()))
                .toList();
    }

    public User getUser() {
        return user;
    }

    public List<FallRiskAssessmentModel> getAssessments() {
        return assessments;
    }
}
