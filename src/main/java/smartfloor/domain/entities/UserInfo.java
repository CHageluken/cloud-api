package smartfloor.domain.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import lombok.Builder;
import smartfloor.domain.entities.user.info.history.GenderConstants;

@Embeddable
public class UserInfo implements Serializable {

    @Column(name = "gender")
    private String gender;

    @Column(name = "admission_diagnosis")
    private String admissionDiagnosis;

    @Column(name = "secondary_diagnosis")
    private String secondaryDiagnosis;

    @Column(name = "relevant_medication")
    private String relevantMedication;

    @Column(name = "height")
    private Integer height;

    @Column(name = "weight")
    private Integer weight;

    @Column(name = "age")
    private Integer age;

    @Column(name = "orthosis")
    private String orthosis;

    @Column(name = "shoes")
    private String shoes;

    @Column(name = "walking_aid")
    private String walkingAid;

    @Column
    private String notes;

    public UserInfo() {
    }

    /**
     * TODO.
     */
    @Builder
    public UserInfo(
            String gender,
            String admissionDiagnosis,
            String secondaryDiagnosis,
            String relevantMedication,
            Integer height,
            Integer weight,
            Integer age,
            String orthosis,
            String shoes,
            String walkingAid,
            String notes
    ) {
        setGender(gender);
        this.admissionDiagnosis = admissionDiagnosis;
        this.secondaryDiagnosis = secondaryDiagnosis;
        this.relevantMedication = relevantMedication;
        this.height = height;
        this.weight = weight;
        this.age = age;
        this.orthosis = orthosis;
        this.shoes = shoes;
        this.walkingAid = walkingAid;
        this.notes = notes;
    }

    public String getGender() {
        return gender;
    }

    /**
     * TODO.
     */
    public void setGender(String gender) {
        if (gender != null && GenderConstants.SUPPORTED_GENDERS.contains(gender)) {
            this.gender = gender;
        } else {
            this.gender = "";
        }
    }

    public String getAdmissionDiagnosis() {
        return admissionDiagnosis;
    }

    public void setAdmissionDiagnosis(String admissionDiagnosis) {
        this.admissionDiagnosis = admissionDiagnosis;
    }

    public String getSecondaryDiagnosis() {
        return secondaryDiagnosis;
    }

    public void setSecondaryDiagnosis(String secondaryDiagnosis) {
        this.secondaryDiagnosis = secondaryDiagnosis;
    }

    public String getRelevantMedication() {
        return relevantMedication;
    }

    public void setRelevantMedication(String relevantMedication) {
        this.relevantMedication = relevantMedication;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getOrthosis() {
        return orthosis;
    }

    public void setOrthosis(String orthosis) {
        this.orthosis = orthosis;
    }

    public String getShoes() {
        return shoes;
    }

    public void setShoes(String shoes) {
        this.shoes = shoes;
    }

    public String getWalkingAid() {
        return walkingAid;
    }

    public void setWalkingAid(String walkingAid) {
        this.walkingAid = walkingAid;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserInfo userInfo = (UserInfo) o;
        return Objects.equals(height, userInfo.height) &&
                Objects.equals(weight, userInfo.weight) &&
                Objects.equals(age, userInfo.age) &&
                Objects.equals(gender, userInfo.gender) &&
                Objects.equals(admissionDiagnosis, userInfo.admissionDiagnosis) &&
                Objects.equals(secondaryDiagnosis, userInfo.secondaryDiagnosis) &&
                Objects.equals(relevantMedication, userInfo.relevantMedication) &&
                Objects.equals(orthosis, userInfo.orthosis) &&
                Objects.equals(shoes, userInfo.shoes) &&
                Objects.equals(walkingAid, userInfo.walkingAid) &&
                Objects.equals(notes, userInfo.notes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                gender,
                admissionDiagnosis,
                secondaryDiagnosis,
                relevantMedication,
                height,
                weight,
                age,
                orthosis,
                shoes,
                walkingAid,
                notes
        );
    }

    @Override
    public String toString() {
        return "UserInfo{" +
                "gender='" + gender + '\'' +
                ", admissionDiagnosis='" + admissionDiagnosis + '\'' +
                ", secondaryDiagnosis='" + secondaryDiagnosis + '\'' +
                ", relevantMedication='" + relevantMedication + '\'' +
                ", height=" + height +
                ", weight=" + weight +
                ", age=" + age +
                ", orthosis='" + orthosis + '\'' +
                ", shoes='" + shoes + '\'' +
                ", walkingAid='" + walkingAid + '\'' +
                ", notes='" + notes + '\'' +
                '}';
    }
}
