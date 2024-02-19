package smartfloor.domain.dto.user.measurements;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Builder;
import org.springframework.validation.annotation.Validated;

@Validated
public final class PomaMeasurementDetails {

    @Min(value = 0, message = "Sitting balance must be between 0 and 1")
    @Max(value = 1, message = "Sitting balance must be between 0 and 1")
    private Integer sittingBalance;

    @Min(value = 0, message = "Arises must be between 0 and 2")
    @Max(value = 2, message = "Arises must be between 0 and 2")
    private Integer arises;

    @Min(value = 0, message = "Attempts to arise must be between 0 and 2")
    @Max(value = 2, message = "Attempts to arise must be between 0 and 2")
    private Integer attemptsToArise;

    @Min(value = 0, message = "Immediate standing balance (first 5 seconds) must be between 0 and 2")
    @Max(value = 2, message = "Immediate standing balance (first 5 seconds) must be between 0 and 2")
    private Integer immediateStandingBalance;

    @Min(value = 0, message = "Standing balance must be between 0 and 2")
    @Max(value = 2, message = "Standing balance must be between 0 and 2")
    private Integer standingBalance;

    @Min(value = 0, message = "Nudged must be between 0 and 2")
    @Max(value = 2, message = "Nudged must be between 0 and 2")
    private Integer nudged;

    @Min(value = 0, message = "Eyes closed must be between 0 and 1")
    @Max(value = 1, message = "Eyes closed must be between 0 and 1")
    private Integer eyesClosed;

    @Min(value = 0, message = "Turning 360 degrees steps must be between 0 and 1")
    @Max(value = 1, message = "Turning 360 degrees steps must be between 0 and 1")
    private Integer turning360DegreesSteps;

    @Min(value = 0, message = "Turning 360 degrees steadiness must be between 0 and 1")
    @Max(value = 1, message = "Turning 360 degrees steadiness must be between 0 and 1")
    private Integer turning360DegreesSteadiness;

    @Min(value = 0, message = "Sitting down must be between 0 and 2")
    @Max(value = 2, message = "Sitting down must be between 0 and 2")
    private Integer sittingDown;

    @Min(value = 0, message = "Balance total must be between 0 and 16")
    @Max(value = 16, message = "Balance total must be between 0 and 16")
    private Integer balanceTotal;

    @Min(value = 0, message = "Initiation of gait must be between 0 and 1")
    @Max(value = 1, message = "Initiation of gait must be between 0 and 1")
    private Integer initiationOfGait;

    @Min(value = 0, message = "Step length and height: right foot passes left must be between 0 and 1")
    @Max(value = 1, message = "Step length and height: right foot passes left must be between 0 and 1")
    private Integer stepLengthHeightRightPassesLeft;

    @Min(value = 0, message = "Step length and height: right foot clears floor must be between 0 and 1")
    @Max(value = 1, message = "Step length and height: right foot clears floor must be between 0 and 1")
    private Integer stepLengthHeightRightClearsFloor;

    @Min(value = 0, message = "Step length and height: left foot passes right must be between 0 and 1")
    @Max(value = 1, message = "Step length and height: left foot passes right must be between 0 and 1")
    private Integer stepLengthHeightLeftPassesRight;

    @Min(value = 0, message = "Step length and height: left foot clears floor must be between 0 and 1")
    @Max(value = 1, message = "Step length and height: left foot clears floor must be between 0 and 1")
    private Integer stepLengthHeightLeftClearsFloor;

    @Min(value = 0, message = "Step symmetry must be between 0 and 1")
    @Max(value = 1, message = "Step symmetry must be between 0 and 1")
    private Integer stepSymmetry;

    @Min(value = 0, message = "Step continuity must be between 0 and 1")
    @Max(value = 1, message = "Step continuity must be between 0 and 1")
    private Integer stepContinuity;

    @Min(value = 0, message = "Path must be between 0 and 2")
    @Max(value = 2, message = "Path must be between 0 and 2")
    private Integer path;

    @Min(value = 0, message = "Trunk must be between 0 and 2")
    @Max(value = 2, message = "Trunk must be between 0 and 2")
    private Integer trunk;

    @Min(value = 0, message = "Walking stance must be between 0 and 1")
    @Max(value = 1, message = "Walking stance must be between 0 and 1")
    private Integer walkingStance;

    @Min(value = 0, message = "Mobility total must be between 0 and 12")
    @Max(value = 12, message = "Mobility total must be between 0 and 12")
    private Integer mobilityTotal;

    public PomaMeasurementDetails() {
    }

    /**
     * TODO.
     */
    @Builder
    public PomaMeasurementDetails(
            Integer sittingBalance,
            Integer arises,
            Integer attemptsToArise,
            Integer immediateStandingBalance,
            Integer standingBalance,
            Integer nudged,
            Integer eyesClosed,
            Integer turning360DegreesSteps,
            Integer turning360DegreesSteadiness,
            Integer sittingDown,
            Integer balanceTotal,
            Integer initiationOfGait,
            Integer stepLengthHeightRightPassesLeft,
            Integer stepLengthHeightRightClearsFloor,
            Integer stepLengthHeightLeftPassesRight,
            Integer stepLengthHeightLeftClearsFloor,
            Integer stepSymmetry,
            Integer stepContinuity,
            Integer path,
            Integer trunk,
            Integer walkingStance,
            Integer mobilityTotal
    ) {
        this.sittingBalance = sittingBalance;
        this.arises = arises;
        this.attemptsToArise = attemptsToArise;
        this.immediateStandingBalance = immediateStandingBalance;
        this.standingBalance = standingBalance;
        this.nudged = nudged;
        this.eyesClosed = eyesClosed;
        this.turning360DegreesSteps = turning360DegreesSteps;
        this.turning360DegreesSteadiness = turning360DegreesSteadiness;
        this.sittingDown = sittingDown;
        this.balanceTotal = balanceTotal;
        this.initiationOfGait = initiationOfGait;
        this.stepLengthHeightRightPassesLeft = stepLengthHeightRightPassesLeft;
        this.stepLengthHeightRightClearsFloor = stepLengthHeightRightClearsFloor;
        this.stepLengthHeightLeftPassesRight = stepLengthHeightLeftPassesRight;
        this.stepLengthHeightLeftClearsFloor = stepLengthHeightLeftClearsFloor;
        this.stepSymmetry = stepSymmetry;
        this.stepContinuity = stepContinuity;
        this.path = path;
        this.trunk = trunk;
        this.walkingStance = walkingStance;
        this.mobilityTotal = mobilityTotal;
    }

    public Integer getSittingBalance() {
        return sittingBalance;
    }

    public Integer getArises() {
        return arises;
    }

    public Integer getAttemptsToArise() {
        return attemptsToArise;
    }

    public Integer getImmediateStandingBalance() {
        return immediateStandingBalance;
    }

    public Integer getStandingBalance() {
        return standingBalance;
    }

    public Integer getNudged() {
        return nudged;
    }

    public Integer getEyesClosed() {
        return eyesClosed;
    }

    public Integer getTurning360DegreesSteps() {
        return turning360DegreesSteps;
    }

    public Integer getTurning360DegreesSteadiness() {
        return turning360DegreesSteadiness;
    }

    public Integer getSittingDown() {
        return sittingDown;
    }

    public Integer getBalanceTotal() {
        return balanceTotal;
    }

    public Integer getInitiationOfGait() {
        return initiationOfGait;
    }

    public Integer getStepLengthHeightRightPassesLeft() {
        return stepLengthHeightRightPassesLeft;
    }

    public Integer getStepLengthHeightRightClearsFloor() {
        return stepLengthHeightRightClearsFloor;
    }

    public Integer getStepLengthHeightLeftPassesRight() {
        return stepLengthHeightLeftPassesRight;
    }

    public Integer getStepLengthHeightLeftClearsFloor() {
        return stepLengthHeightLeftClearsFloor;
    }

    public Integer getStepSymmetry() {
        return stepSymmetry;
    }

    public Integer getStepContinuity() {
        return stepContinuity;
    }

    public Integer getPath() {
        return path;
    }

    public Integer getTrunk() {
        return trunk;
    }

    public Integer getWalkingStance() {
        return walkingStance;
    }

    public Integer getMobilityTotal() {
        return mobilityTotal;
    }
}
