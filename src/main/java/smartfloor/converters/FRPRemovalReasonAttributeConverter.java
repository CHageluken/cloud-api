package smartfloor.converters;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import smartfloor.domain.entities.fall.risk.profile.FallRiskProfileRemovalReason;

@Converter
public class FRPRemovalReasonAttributeConverter implements AttributeConverter<FallRiskProfileRemovalReason, Integer> {

    @Override
    public Integer convertToDatabaseColumn(FallRiskProfileRemovalReason fallRiskProfileRemovalReason) {
        if (fallRiskProfileRemovalReason == null) return null;
        switch (fallRiskProfileRemovalReason) {
            case PROTOCOL:
                return 1;
            case DUPLICATE:
                return 2;
            case SENSOR:
                return 3;
            case APPLICATION:
                return 4;
            case OTHER:
                return 5;
            default:
                throw new IllegalArgumentException(fallRiskProfileRemovalReason + " not supported.");
        }
    }

    @Override
    public FallRiskProfileRemovalReason convertToEntityAttribute(Integer integer) {
        if (integer == null) return null;
        switch (integer) {
            case 1:
                return FallRiskProfileRemovalReason.PROTOCOL;
            case 2:
                return FallRiskProfileRemovalReason.DUPLICATE;
            case 3:
                return FallRiskProfileRemovalReason.SENSOR;
            case 4:
                return FallRiskProfileRemovalReason.APPLICATION;
            case 5:
                return FallRiskProfileRemovalReason.OTHER;
            default:
                throw new IllegalArgumentException(integer + " not supported.");
        }
    }
}
