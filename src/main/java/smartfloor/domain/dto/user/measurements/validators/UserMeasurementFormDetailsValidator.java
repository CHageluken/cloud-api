package smartfloor.domain.dto.user.measurements.validators;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import smartfloor.domain.UserMeasurementType;
import smartfloor.domain.dto.user.measurements.FallIncidentDetails;
import smartfloor.domain.dto.user.measurements.PomaMeasurementDetails;
import smartfloor.domain.dto.user.measurements.UserMeasurementForm;

@Component
public class UserMeasurementFormDetailsValidator implements Validator {

    private static final String DETAILS = "details";
    private final UserMeasurementDetailsValidator userMeasurementDetailsValidator;

    @Autowired
    public UserMeasurementFormDetailsValidator(UserMeasurementDetailsValidator userMeasurementDetailsValidator) {
        this.userMeasurementDetailsValidator = userMeasurementDetailsValidator;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return UserMeasurementForm.class.equals(clazz);
    }

    private void pomatype(UserMeasurementForm form, ObjectMapper mapper, Double value, Errors errors) {
        if (form.getDetails() != null) { // if there's actually anything to validate
            PomaMeasurementDetails pomaMeasurementDetails = mapper.convertValue(
                    form.getDetails(),
                    PomaMeasurementDetails.class
            );

            userMeasurementDetailsValidator.validate(pomaMeasurementDetails, errors);

            if (pomaMeasurementDetails.getBalanceTotal() != null &&
                    pomaMeasurementDetails.getMobilityTotal() != null &&
                    (Integer.sum(
                            pomaMeasurementDetails.getBalanceTotal(),
                            pomaMeasurementDetails.getMobilityTotal()
                    ) != value)) {
                errors.rejectValue(
                        "value",
                        "value.sum.invalid",
                        "Invalid POMA measurement value provided: value should be the sum of balance" +
                                " and mobility total."
                );
            }
        }
    }

    private void fallincidentype(UserMeasurementForm form, ObjectMapper mapper, Errors errors) {
        double valueThatRequiresFormDetails = 0;
        if (form.getValue() > valueThatRequiresFormDetails) {
            if (form.getDetails() != null && !form.getDetails().isEmpty()) {
                FallIncidentDetails fallIncidentDetails = mapper.convertValue(
                        form.getDetails(),
                        FallIncidentDetails.class
                );

                userMeasurementDetailsValidator.validate(fallIncidentDetails, errors);
            } else {
                errors.rejectValue(
                        DETAILS,
                        "details.empty",
                        "No Fall Incident details provided."
                );
            }
        } else if (form.getDetails() != null) {
            errors.rejectValue(
                    DETAILS,
                    "details.forbidden",
                    "Fall Incident details are forbidden."
            );
        }
    }

    @Override
    public void validate(Object target, Errors errors) {
        UserMeasurementForm form = (UserMeasurementForm) target;
        ObjectMapper mapper = new ObjectMapper();
        Double value = form.getValue();

        if (form.getType().equals(UserMeasurementType.POMA)) {
            try {
                pomatype(form, mapper, value, errors);
            } catch (IllegalArgumentException e) {
                errors.rejectValue(
                        DETAILS,
                        "details.invalid",
                        "Invalid POMA measurement details (fields) provided."
                );
            }
        } else if (form.getType().equals(UserMeasurementType.FALL_INCIDENT)) {
            try {

                fallincidentype(form, mapper, errors);
            } catch (IllegalArgumentException e) {
                errors.rejectValue(
                        DETAILS,
                        "details.invalid",
                        "Invalid Fall Incident details provided."
                );
            }
        }
    }
}
