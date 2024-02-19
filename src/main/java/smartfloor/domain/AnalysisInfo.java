package smartfloor.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.ArrayList;
import java.util.List;
import smartfloor.deserializer.AnalysisInfoDeserializer;

@JsonDeserialize(using = AnalysisInfoDeserializer.class)
public class AnalysisInfo {
    private String name;
    private AnalysisCategory category;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private List<Object> parameters = new ArrayList<>();
    @JsonIgnore
    private List<JsonNode> jsonParameterNodes = new ArrayList<>();

    public AnalysisInfo() {
    }

    /**
     * TODO.
     */
    public AnalysisInfo(String name, List<JsonNode> parametersAsJson) {
        this.name = name;
        this.jsonParameterNodes = parametersAsJson;
    }

    public AnalysisInfo(String name, AnalysisCategory category) {
        this.name = name;
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AnalysisCategory getCategory() {
        return category;
    }

    public void setCategory(AnalysisCategory category) {
        this.category = category;
    }

    public List<Object> getParameters() {
        return parameters;
    }

    public void addParameter(Object parameter) {
        parameters.add(parameter);
    }

    public void setParameters(List<Object> parameters) {
        this.parameters = parameters;
    }

    public List<JsonNode> getJsonParameterNodes() {
        return jsonParameterNodes;
    }

    public void addJsonNode(JsonNode jsonNode) {
        this.jsonParameterNodes.add(jsonNode);
    }

    public void setJsonParameterNodes(List<JsonNode> jsonParameterNodes) {
        this.jsonParameterNodes = jsonParameterNodes;
    }
}
