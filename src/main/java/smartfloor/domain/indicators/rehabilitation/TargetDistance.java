package smartfloor.domain.indicators.rehabilitation;

import java.util.Objects;
import smartfloor.domain.entities.UserInfo;
import smartfloor.domain.indicators.Indicator;

/**
 * The 6MWT measures the speed of covering a user-specific target distance.
 * The target distance (in meters) is calculated based on the user's height, weight, age and gender.
 */
public final class TargetDistance implements Indicator {
    private Double value;
    private final String unit = "m";

    private TargetDistance() {
    }

    public TargetDistance(UserInfo userInfo) {
        this.value = compute(userInfo);
    }

    private Double compute(UserInfo userInfo) {
        if (userInfo == null ||
                (userInfo.getHeight() == null || userInfo.getAge() == null || userInfo.getWeight() == null ||
                        userInfo.getGender() == null)) {
            return 0.0;
        }
        double targetDistanceInM;
        double bmi = userInfo.getWeight() / Math.pow(userInfo.getHeight(), 2);

        if (Objects.equals(userInfo.getGender(), "f")) {
            targetDistanceInM = 1064 - (5.28 * userInfo.getAge()) - (6.55 * bmi);
            return (double) Math.round((Math.max(targetDistanceInM, 0.0)) * 100) / 100;
        }

        // gender is "m"
        targetDistanceInM = 1266 - (7.8 * userInfo.getAge()) - (5.92 * bmi);
        return (double) Math.round((Math.max(targetDistanceInM, 0.0)) * 100) / 100;
    }

    @Override
    public Double getValue() {
        return this.value;
    }

    @Override
    public String getUnit() {
        return unit;
    }
}
