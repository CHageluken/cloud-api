package smartfloor.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import smartfloor.domain.AnalysisInfo;

public class AnalysisInfoDeserializer extends StdDeserializer<AnalysisInfo> {

    public AnalysisInfoDeserializer() {
        this(null);
    }

    protected AnalysisInfoDeserializer(Class<AnalysisInfo> vc) {
        super(vc);
    }

    @Override
    public AnalysisInfo deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);
        List<JsonNode> parametersAsJson = new ArrayList<>();
        String name = node.get("name").asText();
        JsonNode parametersNode = node.get("parameters");
        for (JsonNode jsonNode : parametersNode) {
            for (Iterator<String> it = jsonNode.fieldNames(); it.hasNext(); ) {
                String fieldName = it.next();
                JsonNode jsonNodeTemp = jsonNode.get(fieldName);
                parametersAsJson.add(jsonNodeTemp);
            }
        }

        return new AnalysisInfo(name, parametersAsJson);
    }
}
