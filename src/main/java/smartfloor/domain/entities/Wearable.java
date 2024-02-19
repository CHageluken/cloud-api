package smartfloor.domain.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import lombok.Builder;
import smartfloor.domain.dto.CreateWearableForm;
import smartfloor.domain.entities.fall.risk.profile.FallRiskProfile;

@Table(name = "wearables")
@Entity
@Builder
@JsonIgnoreProperties(ignoreUnknown = true, value = {"hibernateLazyInitializer", "handler"})
public class Wearable implements Serializable {

    /**
     * Generated identifier for the wearable.
     */
    @Id
    @Column(name = "id")
    private String id;

    /**
     * References all links to users during sessions. A wearable can be worn by more than one user during various
     * sessions.
     */
    @OneToMany(
            cascade = CascadeType.PERSIST,
            fetch = FetchType.LAZY,
            mappedBy = "wearable"
    )
    @JsonIgnore
    private List<UserWearableLink> userWearableLinks;

    @ManyToMany(mappedBy = "wearables")
    @JsonIgnore
    private List<WearableGroup> groups;

    @OneToMany(
            fetch = FetchType.LAZY,
            mappedBy = "wearable"
    )
    @JsonIgnore
    private List<FallRiskProfile> fallRiskProfiles;

    public Wearable() {
    }

    public Wearable(List<UserWearableLink> userWearableLinks) {
        this.userWearableLinks = userWearableLinks;
    }

    /**
     * TODO.
     */
    @Builder
    public Wearable(
            String id,
            List<UserWearableLink> userWearableLinks,
            List<WearableGroup> groups,
            List<FallRiskProfile> fallRiskProfiles
    ) {
        this.id = id;
        this.userWearableLinks = userWearableLinks;
        this.groups = groups;
        this.fallRiskProfiles = fallRiskProfiles;
    }

    /**
     * Leave it up to the calling method to set the tenant.
     */
    public Wearable(CreateWearableForm createWearableForm) {
        this.id = createWearableForm.getId();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<UserWearableLink> getUserWearableLinks() {
        return userWearableLinks;
    }

    public List<WearableGroup> getGroups() {
        return groups;
    }

    public List<FallRiskProfile> getFallRiskProfiles() {
        return fallRiskProfiles;
    }

    public void setFallRiskProfiles(List<FallRiskProfile> fallRiskProfiles) {
        this.fallRiskProfiles = fallRiskProfiles;
    }

    /**
     * A wearable may be worn either on the left or right side of the ankle or heel of a user.
     */
    public enum Side {
        LEFT,
        RIGHT
    }

    @Override
    public String toString() {
        return "Wearable{" +
                "id='" + id + '\'' +
                ", userWearableLinks=" + userWearableLinks +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Wearable wearable = (Wearable) o;
        return id.equals(wearable.id) &&
                userWearableLinks.equals(wearable.userWearableLinks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userWearableLinks);
    }
}
