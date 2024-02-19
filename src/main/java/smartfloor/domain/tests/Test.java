package smartfloor.domain.tests;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.time.LocalDateTime;
import java.util.List;
import smartfloor.deserializer.CustomLocalDateTimeDeserializer;
import smartfloor.domain.indicators.Indicator;
import smartfloor.serializer.CustomLocalDateTimeSerializer;

public interface Test {

    default String getName() {
        return this.getClass().getSimpleName();
    }

    @JsonSerialize(using = CustomLocalDateTimeSerializer.class)
    @JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
    default LocalDateTime getBeginTime() {
        Trial firstTrial = getTrials().get(0);
        return firstTrial.getBeginTime();
    }

    @JsonSerialize(using = CustomLocalDateTimeSerializer.class)
    @JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
    default LocalDateTime getEndTime() {
        Trial lastTrial = getTrials().get(getTrials().size() - 1);
        return lastTrial.getEndTime();
    }

    List<Trial> getTrials();

    List<Indicator> getIndicators();
}

