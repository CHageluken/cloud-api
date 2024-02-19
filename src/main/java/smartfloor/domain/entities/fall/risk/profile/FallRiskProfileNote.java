package smartfloor.domain.entities.fall.risk.profile;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import lombok.Builder;
import smartfloor.deserializer.CustomLocalDateTimeDeserializer;
import smartfloor.domain.entities.User;
import smartfloor.serializer.CustomLocalDateTimeSerializer;

@Entity
@Table(name = "fall_risk_profile_notes")
public class FallRiskProfileNote implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @JsonIgnore
    public Long id;

    @OneToOne
    @JoinColumn(name = "fall_risk_profile_id")
    @JsonIgnore
    private FallRiskProfile fallRiskProfile;

    @Column
    private String value;

    @Column(name = "created_at")
    @JsonSerialize(using = CustomLocalDateTimeSerializer.class)
    @JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @JsonSerialize(using = CustomLocalDateTimeSerializer.class)
    @JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
    private LocalDateTime updatedAt;

    @ManyToOne
    @JoinColumn(name = "created_by", nullable = false)
    @JsonIgnoreProperties(value = {"archivedAt", "compositeUser", "info", "isArchived"})
    private User createdBy;

    /**
     * Constructor for a fall risk profile note.
     */
    @Builder
    public FallRiskProfileNote(
            String value,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            User createdBy,
            FallRiskProfile fallRiskProfile
    ) {
        this.value = value;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.fallRiskProfile = fallRiskProfile;
    }

    public FallRiskProfileNote() {

    }

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public FallRiskProfile getFallRiskProfile() {
        return fallRiskProfile;
    }

    public void setFallRiskProfile(FallRiskProfile fallRiskProfile) {
        this.fallRiskProfile = fallRiskProfile;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FallRiskProfileNote that = (FallRiskProfileNote) o;
        return Objects.equals(value, that.value) && Objects.equals(createdAt, that.createdAt) &&
                Objects.equals(createdBy, that.createdBy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, createdAt, updatedAt, createdBy);
    }

    @Override
    public String toString() {
        return "FallRiskProfileNote{" +
                "value='" + value + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", createdBy=" + createdBy +
                '}';
    }
}
