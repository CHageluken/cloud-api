package smartfloor.domain.dto;

/**
 * A DTO object that allows one to provide a wearable id of a wearable for which an active user-wearable link
 * should be completed (ended). This effectively "unlinks" the wearable, i.e. it has no active links anymore.
 */
public class UnlinkActiveWearableForm {

    private String wearableId;

    public UnlinkActiveWearableForm() {
    }

    public UnlinkActiveWearableForm(String wearableId) {
        this.wearableId = wearableId;
    }

    public String getWearableId() {
        return wearableId;
    }

    public void setWearableId(String wearableId) {
        this.wearableId = wearableId;
    }

    @Override
    public String toString() {
        return "UnlinkActiveWearableForm{" +
                "wearableId='" + wearableId + '\'' +
                '}';
    }
}
