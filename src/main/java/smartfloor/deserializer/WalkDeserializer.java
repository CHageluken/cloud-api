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
import smartfloor.domain.tests.Trial;
import smartfloor.domain.tests.Walk;

public class WalkDeserializer extends StdDeserializer<Walk> {
    ObjectMapper mapper;

    public WalkDeserializer() {
        this(null);
        mapper = new ObjectMapper();
    }

    protected WalkDeserializer(Class<?> vc) {
        super(vc);
        mapper = new ObjectMapper();
    }


    @Override
    public Walk deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
            throws IOException {
        JsonNode walkNode = jsonParser.getCodec().readTree(jsonParser);
        JsonNode trialsNode = walkNode.get("trials");
        JsonNode indicatorsNode = walkNode.get("indicators");
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
            trials.add(new Walk.Trial(
                    trialIndicators, beginTime, endTime
            ));
        }

        return new Walk(trials, indicators);
    }
}
