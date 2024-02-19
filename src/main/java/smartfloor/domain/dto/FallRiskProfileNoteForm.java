package smartfloor.domain.dto;

import lombok.Builder;

public class FallRiskProfileNoteForm {
    /**
     * The value of the FRP note.
     */
    private String noteValue;

    public FallRiskProfileNoteForm() {

    }

    @Builder
    public FallRiskProfileNoteForm(String noteValue) {
        this.noteValue = noteValue;
    }

    public String getNoteValue() {
        return noteValue;
    }

    public void setNoteValue(String noteValue) {
        this.noteValue = noteValue;
    }
}
