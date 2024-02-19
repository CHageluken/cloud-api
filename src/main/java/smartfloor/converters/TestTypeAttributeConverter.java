package smartfloor.converters;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import smartfloor.domain.entities.rehabilitation.TestType;

@Converter
public class TestTypeAttributeConverter implements AttributeConverter<TestType, Integer> {

    @Override
    public Integer convertToDatabaseColumn(TestType testType) {
        if (testType == null) return null;
        switch (testType) {
            case BERG_BALANCE_SCALE:
                return 1;
            case SIX_MINUTE_WALKING:
                return 2;
            case TEN_METER_WALKING:
                return 3;
            case TIMED_UP_N_GO:
                return 4;
            case POMA:
                return 5;
            case WALK:
                return 6;
            default:
                throw new IllegalArgumentException(testType + " not supported.");
        }
    }

    @Override
    public TestType convertToEntityAttribute(Integer integer) {
        if (integer == null) return null;
        switch (integer) {
            case 1:
                return TestType.BERG_BALANCE_SCALE;
            case 2:
                return TestType.SIX_MINUTE_WALKING;
            case 3:
                return TestType.TEN_METER_WALKING;
            case 4:
                return TestType.TIMED_UP_N_GO;
            case 5:
                return TestType.POMA;
            case 6:
                return TestType.WALK;
            default:
                throw new IllegalArgumentException(integer + " not supported.");
        }
    }
}
