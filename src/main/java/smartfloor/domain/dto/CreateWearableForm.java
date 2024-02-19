package smartfloor.domain.dto;

public class CreateWearableForm {

    /**
     * Every wearable has a (hardware) identifier.
     */
    private String id;

    public CreateWearableForm() {
    }

    public CreateWearableForm(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

}
