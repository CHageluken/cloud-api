package smartfloor.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import smartfloor.domain.dto.rehabilitation.TestResultForm;
import smartfloor.domain.entities.Group;
import smartfloor.domain.entities.User;
import smartfloor.domain.entities.rehabilitation.TestResult;
import smartfloor.domain.entities.rehabilitation.TestSurveyType;
import smartfloor.domain.entities.rehabilitation.TestType;
import smartfloor.domain.exception.GroupNotFoundException;
import smartfloor.domain.exception.TestResultNotFoundException;
import smartfloor.domain.exception.UserIsArchivedException;
import smartfloor.domain.exception.UserNotFoundException;
import smartfloor.domain.exception.WearableNotFoundException;
import smartfloor.domain.indicators.rehabilitation.TargetDistance;
import smartfloor.service.GroupService;
import smartfloor.service.RehabilitationService;
import smartfloor.service.UserService;

@Tag(name = "Rehabilitation API", description = "Provides CRUD operations for working with rehabilitation (tests).")
@RestController
@RequestMapping("/rehabilitation")
public class RehabilitationController {

    private final GroupService groupService;
    private final RehabilitationService rehabilitationService;
    private final UserService userService;

    /**
     * TODO.
     */
    @Autowired
    public RehabilitationController(
            GroupService groupService,
            RehabilitationService rehabilitationService,
            UserService userService
    ) {
        this.groupService = groupService;
        this.rehabilitationService = rehabilitationService;
        this.userService = userService;
    }

    @GetMapping("/tests/results")
    @ResponseStatus(HttpStatus.OK)
    public List<TestResult> getTestResults() {
        return rehabilitationService.getTestResults();
    }

    @GetMapping("/tests/results/groups/{groupId}")
    @ResponseStatus(HttpStatus.OK)
    public List<TestResult> getTestResultsByGroupId(@PathVariable Long groupId) throws GroupNotFoundException {
        Group group = groupService.getGroup(groupId);
        return rehabilitationService.getTestResultsForUsers(group.getUsers(true));
    }

    @GetMapping("/tests/results/{testResultId}")
    @ResponseStatus(HttpStatus.OK)
    public TestResult getTestResultById(@PathVariable Long testResultId) throws TestResultNotFoundException {
        return rehabilitationService.getTestResultById(testResultId);
    }

    @GetMapping("/tests/types")
    @ResponseStatus(HttpStatus.OK)
    public List<TestType> getTestTypes() {
        return rehabilitationService.getTestTypes();
    }

    @GetMapping("/tests/surveys/types")
    @ResponseStatus(HttpStatus.OK)
    public List<TestSurveyType> getTestSurveyTypes() {
        return rehabilitationService.getTestSurveyTypes();
    }

    @Operation(description = "Create a new test result from a given test result DTO.")
    @PostMapping("/tests/results")
    @ResponseStatus(HttpStatus.CREATED)
    public TestResult createTestResult(@RequestBody TestResultForm testResultForm)
            throws UserNotFoundException, UserIsArchivedException, WearableNotFoundException {
        return rehabilitationService.createTestResult(testResultForm);
    }

    /**
     * TODO.
     */
    @Operation(description = "Update a test result with a provided identifier.")
    @PutMapping("/tests/results/{testResultId}")
    @ResponseStatus(HttpStatus.OK)
    public Optional<TestResult> updateTestResult(
            @PathVariable Long testResultId,
            @RequestBody TestResultForm testResultForm
    ) throws TestResultNotFoundException {
        TestResult testResult = rehabilitationService.getTestResultById(testResultId);
        boolean isTestResultCompletelyReset = testResultForm
                .getEndTime()
                .toInstant(ZoneOffset.UTC)
                .toEpochMilli() == -1;
        if (isTestResultCompletelyReset) {
            rehabilitationService.deleteTestResult(testResult);
            return Optional.empty();
        }
        return Optional.ofNullable(rehabilitationService.updateTestResult(testResult, testResultForm));
    }

    @Operation(description = "Delete a test result with a provided identifier.")
    @DeleteMapping("/tests/results/{testResultId}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteTestResult(@PathVariable Long testResultId)
            throws TestResultNotFoundException, UserIsArchivedException {
        TestResult testResult = rehabilitationService.getTestResultById(testResultId);
        rehabilitationService.softDeleteTestResult(testResult);
    }

    @Operation(description = "Calculate the distance a user should cover during the 6MWT.")
    @GetMapping("/target-distance/users/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public TargetDistance calculateTargetDistanceForUser(@PathVariable Long userId) throws UserNotFoundException {
        User user = userService.getUser(userId);
        return rehabilitationService.calculateTargetDistanceForUser(user);
    }
}
