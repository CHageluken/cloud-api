package smartfloor.domain.entities.rehabilitation;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import java.util.Map;
import lombok.Builder;
import org.hibernate.annotations.Type;
import smartfloor.converters.TestSurveyTypeAttributeConverter;
import smartfloor.domain.dto.rehabilitation.TestSurveyForm;

@Embeddable
public class TestSurvey {
    @Column(name = "type", nullable = false)
    @Convert(converter = TestSurveyTypeAttributeConverter.class)
    private TestSurveyType type;

    @Type(JsonBinaryType.class)
    @Column(name = "content")
    private Map<String, Object> content;

    public TestSurvey() {
    }

    public TestSurvey(TestSurveyForm testSurveyForm) {
        this.type = testSurveyForm.getType();
        this.content = testSurveyForm.getContent();
    }

    @Builder
    public TestSurvey(TestSurveyType type, Map<String, Object> content) {
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
