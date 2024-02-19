package smartfloor.domain.entities.fall.risk.profile;

/**
 * A fall risk assessment model can be used to determine a fall risk assessment metric and any necessary other values
 * (such as a metric per feature/indicator of the fall risk profile).
 * Hence, every fall risk assessment model expects a certain fall risk profile (consisting of multiple indicators) from
 * which the assessment can be determined. The assessment is then determined through execution of the compute() method.
 * NOTE: In the domain terminology, the term "fall risk profile" (FRP) is actually used to refer to this fall risk
 * assessment.
 */
public interface FallRiskAssessmentModel {
    /**
     * Computes the total fall risk assessment metric (a continuous or categorical value representing the "overall" FRP)
     * from the fall risk profile provided to the model.
     */
    void compute();

    /**
     * The fall risk assessment is computed from a fall risk profile provided to it.
     *
     * @param fallRiskProfile a fall risk profile that will determine the fall risk assessment.
     */
    void setFallRiskProfile(FallRiskProfile fallRiskProfile);

    FallRiskProfile getFallRiskProfile();

    /**
     * The total fall risk assessment/FRP metric (a continuous or categorical value) representing the "overall"
     * FRP. The return type is a double suitable for a continuous value, i.e. a "score" or a categorical value
     * (represented numerically).
     *
     * @return total FRP metric.
     */
    double getTotal();
}
