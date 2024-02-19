package smartfloor.domain.entities.rehabilitation;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import org.springframework.lang.Nullable;
import smartfloor.domain.entities.Wearable;

/**
 * The Embeddable annotation will allow us to use this entity inside the TestResult, without the need of a
 * separate table for this entity.
 */
@Embeddable
public class WearableWithSide {
    @Nullable
    @ManyToOne
    @JoinColumn(name = "wearable_id")
    @JsonUnwrapped
    private Wearable wearable;

    @Nullable
    @Column(name = "wearable_side")
    private Wearable.Side side;


    private WearableWithSide() {
    }

    public WearableWithSide(Wearable wearable, Wearable.Side side) {
        this.wearable = wearable;
        this.side = side;
    }

    public Wearable getWearable() {
        return wearable;
    }

    public void setWearable(Wearable wearable) {
        this.wearable = wearable;
    }

    public Wearable.Side getSide() {
        return side;
    }

    public void setSide(Wearable.Side side) {
        this.side = side;
    }
}
