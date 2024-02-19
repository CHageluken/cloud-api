package smartfloor.domain.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Builder;
import smartfloor.deserializer.CustomLocalDateTimeDeserializer;
import smartfloor.serializer.CustomLocalDateTimeSerializer;

/**
 * The footstep class describes a footstep as made by a user on a floor while wearing a wearable.
 * It contains a timestamp, a position and references to the floor the footstep was made on as well as the wearable that
 * measured the footstep.
 */
@Entity
@Table(name = "footsteps", uniqueConstraints = @UniqueConstraint(columnNames = {"wearable_id", "timestamp"}))
@SecondaryTable(
        name = "footstep_positions",
        pkJoinColumns = @PrimaryKeyJoinColumn(name = "footstep_id")
)
public class Footstep implements Comparable<Footstep>, Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private Long id;

    /**
     * The footstep's position is stored in a secondary table 'footstep_positions' and can be null (i.e. we found no
     * position for the footstep). A missing position will be indicated by a missing entry in the secondary table.
     * We override the attributes here to link to the secondary table.
     * Using the secondary table, we avoid writing null values whenever a position is not available for a footstep.
     */
    @Embedded
    @AttributeOverrides(
            {
                    @AttributeOverride(name = "x", column = @Column(name = "x", table = "footstep_positions")),
                    @AttributeOverride(name = "y", column = @Column(name = "y", table = "footstep_positions"))
            }
    )
    private Position position;

    @Column(name = "timestamp", nullable = false)
    @JsonSerialize(using = CustomLocalDateTimeSerializer.class)
    @JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
    private LocalDateTime time;

    /**
     * The floor the footstep was made on.
     * It can be null if the source footstep event it was based on did not contain any tags, and we did not set a floor
     * (or predict it in some other way).
     */
    @ManyToOne
    @JoinColumn(name = "floor_id")
    @JsonIgnore
    private Floor floor;

    /**
     * The wearable the footstep was registered by.
     */
    @ManyToOne
    @JoinColumn(name = "wearable_id", nullable = false)
    private Wearable wearable;

    public Footstep() {
    }

    /**
     * TODO.
     */
    @Builder
    public Footstep(Position position, LocalDateTime time, Floor floor, Wearable wearable) {
        this.position = position;
        this.time = time;
        this.floor = floor;
        this.wearable = wearable;
    }

    public Footstep(Footstep footstep) {
        this.position = footstep.getPosition();
        this.time = footstep.getTime();
    }

    public Long getId() {
        return id;
    }

    public boolean hasPosition() {
        return position != null;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }

    public Floor getFloor() {
        return floor;
    }

    @JsonProperty("floorId")
    private Long getFloorId() {
        if (floor != null) {
            return floor.getId();
        } else {
            return null;
        }
    }

    public void setFloor(Floor floor) {
        this.floor = floor;
    }

    public void setWearable(Wearable wearable) {
        this.wearable = wearable;
    }

    public Wearable getWearable() {
        return wearable;
    }

    /**
     * Footstep ordering is determined by their timestamp.
     */
    @Override
    public int compareTo(Footstep footstep) {
        return time.compareTo(footstep.getTime());
    }
}
