package smartfloor.converters;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import smartfloor.domain.entities.interventions.InterventionType;

@Converter
public class InterventionTypeAttributeConverter implements AttributeConverter<InterventionType, Integer> {

    @Override
    public Integer convertToDatabaseColumn(InterventionType interventionType) {
        if (interventionType == null) return null;
        switch (interventionType) {
            case REGULAR_EXERCISE:
                return 1;
            case FALL_PREVENTION_PROGRAM:
                return 2;
            case INDIVIDUAL_PHYSIOTHERAPY:
                return 3;
            case OTHER:
                return 4;
            default:
                throw new IllegalArgumentException(interventionType + " not supported.");
        }
    }

    @Override
    public InterventionType convertToEntityAttribute(Integer integer) {
        if (integer == null) return null;
        switch (integer) {
            case 1:
                return InterventionType.REGULAR_EXERCISE;
            case 2:
                return InterventionType.FALL_PREVENTION_PROGRAM;
            case 3:
                return InterventionType.INDIVIDUAL_PHYSIOTHERAPY;
            case 4:
                return InterventionType.OTHER;
            default:
                throw new IllegalArgumentException(integer + " not supported.");
        }
    }
}
