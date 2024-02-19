package smartfloor.converters;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import smartfloor.domain.entities.rehabilitation.TestSurveyType;

@Converter
public class TestSurveyTypeAttributeConverter implements AttributeConverter<TestSurveyType, Integer> {

    @Override
    public Integer convertToDatabaseColumn(TestSurveyType testSurveyType) {
        if (testSurveyType == null) return null;
        switch (testSurveyType) {
            case BORG_RPE:
                return 1;
            case FAC:
                return 2;
            case FES_I:
                return 3;
            case NPRS:
                return 4;
            case NRS:
                return 5;
            default:
                throw new IllegalArgumentException(testSurveyType + " not supported.");
        }
    }

    @Override
    public TestSurveyType convertToEntityAttribute(Integer integer) {
        if (integer == null) return null;
        switch (integer) {
            case 1:
                return TestSurveyType.BORG_RPE;
            case 2:
                return TestSurveyType.FAC;
            case 3:
                return TestSurveyType.FES_I;
            case 4:
                return TestSurveyType.NPRS;
            case 5:
                return TestSurveyType.NRS;
            default:
                throw new IllegalArgumentException(integer + " not supported.");
        }
    }
}
