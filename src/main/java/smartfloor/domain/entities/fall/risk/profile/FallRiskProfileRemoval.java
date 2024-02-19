package smartfloor.domain.entities.fall.risk.profile;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;
import smartfloor.converters.FRPRemovalReasonAttributeConverter;
import smartfloor.deserializer.CustomLocalDateTimeDeserializer;
import smartfloor.domain.entities.User;
import smartfloor.serializer.CustomLocalDateTimeSerializer;

/**
 * This entity provides details on the soft-deletion of an FRP.
 */
@Entity
@Table(name = "fall_risk_profile_removals")
public class FallRiskProfileRemoval {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    /**
     * When an FRP is removed, we take the timestamp of removal and make it available as the removal time property.
     */
    @CreationTimestamp()
    @Column(name = "deleted_at")
    @JsonSerialize(using = CustomLocalDateTimeSerializer.class)
    @JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
    private LocalDateTime deletedAt;

    @Column(name = "reason_for_removal", nullable = false)
    @Convert(converter = FRPRemovalReasonAttributeConverter.class)
    private FallRiskProfileRemovalReason reasonForRemoval;

    /**
     * The specification field is a field for providing detail when the `reasonForRemoval` is `other`.
     */
    @Column(name = "specification_other")
    private String specificationOther;

    /**
     * References the user that is logged-in during this session.
     */
    @ManyToOne
    @JoinColumn(name = "deleted_by", nullable = false)
    private User deletedBy;

    @OneToOne
    @JoinColumn(name = "fall_risk_profile_id")
    private FallRiskProfile fallRiskProfile;

    protected FallRiskProfileRemoval() {
    }

    /**
     * This entity provides details on the soft-deletion of an FRP.
     *
     * @param reasonForRemoval The reason why the FRP is being soft-deleted.
     * @param specificationOther In case the reason is "other", this field can provide further details.
     * @param deletedBy The authenticated user.
     * @param deletedAt The time of the soft-deletion.
     */
    @Builder
    public FallRiskProfileRemoval(
            FallRiskProfile fallRiskProfile,
            FallRiskProfileRemovalReason reasonForRemoval,
            String specificationOther,
            User deletedBy,
            LocalDateTime deletedAt
    ) {
        this.fallRiskProfile = fallRiskProfile;
        this.reasonForRemoval = reasonForRemoval;
        this.specificationOther = specificationOther;
        this.deletedBy = deletedBy;
        this.deletedAt = deletedAt;
    }

    public FallRiskProfile getFallRiskProfile() {
        return fallRiskProfile;
    }

    public void setFallRiskProfile(FallRiskProfile fallRiskProfile) {
        this.fallRiskProfile = fallRiskProfile;
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public FallRiskProfileRemovalReason getReasonForRemoval() {
        return reasonForRemoval;
    }

    public String getSpecificationOther() {
        return specificationOther;
    }

    public User getDeletedBy() {
        return deletedBy;
    }

    public void setDeletedBy(User deletedBy) {
        this.deletedBy = deletedBy;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FallRiskProfileRemoval that = (FallRiskProfileRemoval) o;
        /* Null check on id's handles the case where an FRP is instantiated
        from a list of footsteps, then id is null. */
        return ((id == null || that.id == null) || id.equals(that.id)) &&
                fallRiskProfile.equals(that.fallRiskProfile) &&
                deletedAt.equals(that.deletedAt) &&
                reasonForRemoval == that.reasonForRemoval &&
                specificationOther.equals(that.specificationOther) &&
                deletedBy.equals(that.deletedBy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, deletedAt, reasonForRemoval, specificationOther);
    }

    @Override
    public String toString() {
        return "FallRiskProfileRemoval{" +
                "id=" + id +
                ", deletedAt=" + deletedAt +
                ", reasonForRemoval=" + reasonForRemoval +
                ", specificationOther='" + specificationOther + '\'' +
                ", deletedBy=" + deletedBy +
                ", fallRiskProfile=" + fallRiskProfile +
                '}';
    }
}
