package smartfloor.domain.dto.rehabilitation;

import java.util.Map;
import lombok.Builder;
import smartfloor.domain.entities.rehabilitation.TestSurveyType;

public class TestSurveyForm {

    private TestSurveyType type;

    private Map<String, Object> content;

    public TestSurveyForm() {
    }

    @Builder
    public TestSurveyForm(TestSurveyType type, Map<String, Object> content) {
        this.type = type;
        this.content = content;
    }

    public TestSurveyType getType() {
        return type;
    }

    void setType(TestSurveyType type) {
        this.type = type;
    }

    public Map<String, Object> getContent() {
        return content;
    }

    void setContent(Map<String, Object> content) {
        this.content = content;
    }
}
