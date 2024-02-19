package smartfloor.domain.entities;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import smartfloor.converters.UserMeasurementTypeAttributeConverter;
import smartfloor.deserializer.CustomLocalDateTimeDeserializer;
import smartfloor.domain.UserMeasurementType;
import smartfloor.domain.dto.user.measurements.UserMeasurementForm;
import smartfloor.serializer.CustomLocalDateTimeSerializer;

@Entity
@Table(name = "user_measurements")
public class UserMeasurement implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long id;

    @Column(name = "type_id", nullable = false)
    @Convert(converter = UserMeasurementTypeAttributeConverter.class)
    private UserMeasurementType type;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "recorded_at", nullable = false)
    @JsonSerialize(using = CustomLocalDateTimeSerializer.class)
    @JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
    private LocalDateTime recordedAt;

    @ManyToOne
    @JoinColumn(name = "recorded_by", nullable = false)
    private User recordedBy;

    @CreationTimestamp
    @Column(name = "created_at")
    @JsonSerialize(using = CustomLocalDateTimeSerializer.class)
    @JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private Double value;

    @Type(JsonBinaryType.class)
    @Column(name = "details")
    private Map<String, Object> details;

    @Column(name = "deleted")
    private boolean deleted;

    public UserMeasurement() {
    }

    /**
     * TODO.
     */
    @Builder
    private UserMeasurement(
            Long id,
            UserMeasurementType type,
            User user,
            LocalDateTime recordedAt,
            User recordedBy,
            LocalDateTime createdAt,
            Double value,
            Map<String, Object> details
    ) {
        this.id = id;
        this.type = type;
        this.user = user;
        this.recordedAt = recordedAt;
        this.recordedBy = recordedBy;
        this.createdAt = createdAt;
        this.value = value;
        this.details = details;
        this.deleted = false;
    }

    /**
     * TODO.
     */
    public static UserMeasurement fromDetails(UserMeasurementForm userMeasurementForm) throws IllegalArgumentException {
        return UserMeasurement.builder()
                .type(userMeasurementForm.getType())
                .recordedAt(userMeasurementForm.getRecordedAt())
                .value(userMeasurementForm.getValue())
                .details(userMeasurementForm.getDetails())
                .build();
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public UserMeasurementType getType() {
        return type;
    }

    public void setType(UserMeasurementType type) {
        this.type = type;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(LocalDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }

    public User getRecordedBy() {
        return recordedBy;
    }

    public void setRecordedBy(User recordedBy) {
        this.recordedBy = recordedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserMeasurement that = (UserMeasurement) o;
        return Objects.equals(id, that.id) && type == that.type && Objects.equals(user, that.user) &&
                Objects.equals(recordedAt, that.recordedAt) && Objects.equals(recordedBy, that.recordedBy) &&
                Objects.equals(createdAt, that.createdAt) && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type, user, recordedAt, recordedBy, createdAt, value);
    }

    @Override
    public String toString() {
        return "UserMeasurement{" +
                "id=" + id +
                ", type=" + type +
                ", user=" + user +
                ", recordedAt=" + recordedAt +
                ", recordedBy=" + recordedBy +
                ", createdAt=" + createdAt +
                ", value=" + value +
                '}';
    }
}
