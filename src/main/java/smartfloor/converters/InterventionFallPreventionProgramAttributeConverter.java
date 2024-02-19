package smartfloor.converters;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import smartfloor.domain.entities.interventions.FallPreventionProgram;

@Converter
public class InterventionFallPreventionProgramAttributeConverter
        implements AttributeConverter<FallPreventionProgram, Integer> {

    @Override
    public Integer convertToDatabaseColumn(FallPreventionProgram fallPreventionProgram) {
        if (fallPreventionProgram == null) return null;
        switch (fallPreventionProgram) {
            case VALLEN_VERLEDEN_TIJD:
                return 1;
            case IN_BALANS:
                return 2;
            case OTAGO:
                return 3;
            case THUIS_ONBEZORGD_MOBIEL:
                return 4;
            case STEVIG_STAAN:
                return 5;
            case MINDER_FALLEN_DOOR_MEER_BEWEGEN:
                return 6;
            case ZICHT_OP_EVENWICHT:
                return 7;
            default:
                throw new IllegalArgumentException(fallPreventionProgram + " not supported.");
        }
    }

    @Override
    public FallPreventionProgram convertToEntityAttribute(Integer integer) {
        if (integer == null) return null;
        switch (integer) {
            case 1:
                return FallPreventionProgram.VALLEN_VERLEDEN_TIJD;
            case 2:
                return FallPreventionProgram.IN_BALANS;
            case 3:
                return FallPreventionProgram.OTAGO;
            case 4:
                return FallPreventionProgram.THUIS_ONBEZORGD_MOBIEL;
            case 5:
                return FallPreventionProgram.STEVIG_STAAN;
            case 6:
                return FallPreventionProgram.MINDER_FALLEN_DOOR_MEER_BEWEGEN;
            case 7:
                return FallPreventionProgram.ZICHT_OP_EVENWICHT;
            default:
                throw new IllegalArgumentException(integer + " not supported.");
        }
    }
}
