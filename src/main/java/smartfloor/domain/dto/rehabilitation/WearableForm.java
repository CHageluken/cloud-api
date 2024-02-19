package smartfloor.domain.dto.rehabilitation;

import smartfloor.domain.entities.Wearable;

/**
 * Part of the TestResult form.
 */
public class WearableForm {
    private String id;
    private Wearable.Side side;

    private WearableForm() {
    }

    public WearableForm(String wearableId, Wearable.Side side) {
        this.id = wearableId;
        this.side = side;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Wearable.Side getSide() {
        return side;
    }

    public void setSide(Wearable.Side side) {
        this.side = side;
    }

    @Override
    public String toString() {
        return "WearableForm{" +
                "id='" + id + '\'' +
                ", side=" + side +
                '}';
    }
}
