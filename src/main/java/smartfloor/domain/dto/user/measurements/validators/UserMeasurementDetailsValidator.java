package smartfloor.domain.dto.user.measurements.validators;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import smartfloor.domain.dto.user.measurements.FallIncidentDetails;
import smartfloor.domain.dto.user.measurements.PomaMeasurementDetails;

@Component
public class UserMeasurementDetailsValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        if (PomaMeasurementDetails.class.isAssignableFrom(clazz)) {
            return true;
        } else {
            return FallIncidentDetails.class.isAssignableFrom(clazz);
        }
    }

    /**
     * Sums all the balance fields that are provided (not null).
     *
     * @param details the POMA measurement details
     * @return the sum of the non-null balance fields
     */
    private Integer sumOfBalanceFields(PomaMeasurementDetails details) {
        return Stream.of(
                        details.getSittingBalance(),
                        details.getArises(),
                        details.getAttemptsToArise(),
                        details.getImmediateStandingBalance(),
                        details.getStandingBalance(),
                        details.getNudged(),
                        details.getEyesClosed(),
                        details.getTurning360DegreesSteps(),
                        details.getTurning360DegreesSteadiness(),
                        details.getSittingDown()
                )
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
    }

    /**
     * Sums all the mobility fields that are provided (not null).
     *
     * @param details the POMA measurement details
     * @return the sum of the non-null mobility fields
     */
    private Integer sumOfMobilityFields(PomaMeasurementDetails details) {
        return Stream.of(
                        details.getInitiationOfGait(),
                        details.getStepLengthHeightRightPassesLeft(),
                        details.getStepLengthHeightRightClearsFloor(),
                        details.getStepLengthHeightLeftPassesRight(),
                        details.getStepLengthHeightLeftClearsFloor(),
                        details.getStepSymmetry(),
                        details.getStepContinuity(),
                        details.getPath(),
                        details.getTrunk(),
                        details.getWalkingStance()
                )
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
    }

    private boolean onlyBalanceTotalProvided(PomaMeasurementDetails details) {
        return details.getBalanceTotal() != null && Stream.of(
                details.getSittingBalance(),
                details.getArises(),
                details.getAttemptsToArise(),
                details.getImmediateStandingBalance(),
                details.getStandingBalance(),
                details.getNudged(),
                details.getEyesClosed(),
                details.getTurning360DegreesSteps(),
                details.getTurning360DegreesSteadiness(),
                details.getSittingDown()
        ).noneMatch(Objects::nonNull);
    }

    private boolean onlyMobilityTotalProvided(PomaMeasurementDetails details) {
        return details.getMobilityTotal() != null && Stream.of(
                details.getInitiationOfGait(),
                details.getStepLengthHeightRightPassesLeft(),
                details.getStepLengthHeightRightClearsFloor(),
                details.getStepLengthHeightLeftPassesRight(),
                details.getStepLengthHeightLeftClearsFloor(),
                details.getStepSymmetry(),
                details.getStepContinuity(),
                details.getPath(),
                details.getTrunk(),
                details.getWalkingStance()
        ).noneMatch(Objects::nonNull);
    }

    private boolean providedCausesAreValid(FallIncidentDetails details) {
        List<String> causes = details.getCauses();
        if (causes == null) return false;

        List<String> allowedFallCauses = List.of(
                "Reduced muscle strength",
                "Impaired balance",
                "Bad eyesight",
                "Fear of falling",
                "External cause (e.g., slippery floor, crooked tile, poor lighting, branch on the street, etc.)"
        );
        return allowedFallCauses.containsAll(causes);
    }

    /**
     * Provide additional validation for the POMA measurement details.
     *
     * @param target the POMA measurement details
     * @param errors the validation errors
     */
    @Override
    public void validate(Object target, Errors errors) {
        if (target instanceof PomaMeasurementDetails) {
            PomaMeasurementDetails details = (PomaMeasurementDetails) target;

            Errors pomaErrors = new BeanPropertyBindingResult(details, errors.getObjectName());

            boolean balanceFieldsSumUpToTotal = onlyBalanceTotalProvided(details) ||
                    sumOfBalanceFields(details).equals(details.getBalanceTotal());
            if (!balanceFieldsSumUpToTotal) {
                pomaErrors.rejectValue(
                        "balanceTotal",
                        "poma.balanceTotal.sum.invalid",
                        "The sum of the balance fields must equal the balance total."
                );
            }

            boolean mobilityFieldsSumUpToTotal = onlyMobilityTotalProvided(details) ||
                    sumOfMobilityFields(details).equals(details.getMobilityTotal());
            if (!mobilityFieldsSumUpToTotal) {
                pomaErrors.rejectValue(
                        "mobilityTotal",
                        "poma.mobilityTotal.sum.invalid",
                        "The sum of the mobility fields must equal the mobility total."
                );
            }

            errors.addAllErrors(pomaErrors);
        } else if (target instanceof FallIncidentDetails) {
            FallIncidentDetails details = (FallIncidentDetails) target;
            boolean detailsAreValid = providedCausesAreValid(details);
            Errors fallIncidentErrors = new BeanPropertyBindingResult(details, errors.getObjectName());
            if (!detailsAreValid) {
                fallIncidentErrors.rejectValue(
                        "causes",
                        "fallIncident.causes.invalid",
                        "One or more fall causes are not valid."
                );
            }

            errors.addAllErrors(fallIncidentErrors);
        }
    }
}
