package smartfloor.converters;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import smartfloor.domain.UserMeasurementType;

@Converter
public class UserMeasurementTypeAttributeConverter implements AttributeConverter<UserMeasurementType, Integer> {

    @Override
    public Integer convertToDatabaseColumn(UserMeasurementType type) {
        if (type == null) return null;
        switch (type) {
            case FALL_INCIDENT:
                return 2;
            case POMA:
                return 1;
            default:
                throw new IllegalArgumentException(type + " not supported.");
        }
    }

    @Override
    public UserMeasurementType convertToEntityAttribute(Integer integer) {
        if (integer == null) return null;
        switch (integer) {
            case 2:
                return UserMeasurementType.FALL_INCIDENT;
            case 1:
                return UserMeasurementType.POMA;
            default:
                throw new IllegalArgumentException(integer + " not supported.");
        }
    }
}
