package smartfloor.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import smartfloor.domain.indicators.Indicator;
import smartfloor.domain.tests.TimedUpNGo;
import smartfloor.domain.tests.Trial;

public class TimedUpNGoDeserializer extends StdDeserializer<TimedUpNGo> {
    ObjectMapper mapper;

    public TimedUpNGoDeserializer() {
        this(null);
        mapper = new ObjectMapper();
    }

    protected TimedUpNGoDeserializer(Class<?> vc) {
        super(vc);
        mapper = new ObjectMapper();
    }


    @Override
    public TimedUpNGo deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
            throws IOException {
        JsonNode tugNode = jsonParser.getCodec().readTree(jsonParser);
        JsonNode trialsNode = tugNode.get("trials");
        JsonNode indicatorsNode = tugNode.get("indicators");
        String indicatorsContent = indicatorsNode.toString();
        List<Indicator> indicators = mapper.readValue(indicatorsContent, new TypeReference<>() {
        });

        List<Trial> trials = new ArrayList<>();
        for (JsonNode jsonNode : trialsNode) {
            LocalDateTime beginTime = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(jsonNode.get("beginTime").longValue()),
                    ZoneOffset.UTC
            );
            LocalDateTime endTime =
                    LocalDateTime.ofInstant(Instant.ofEpochMilli(jsonNode.get("endTime").longValue()), ZoneOffset.UTC);
            JsonNode trialIndicatorsNode = jsonNode.get("indicators");
            List<Indicator> trialIndicators = mapper.readValue(trialIndicatorsNode.toString(), new TypeReference<>() {
            });
            trials.add(new TimedUpNGo.Trial(
                    trialIndicators, beginTime, endTime
            ));
        }

        return new TimedUpNGo(trials, indicators);
    }
}
