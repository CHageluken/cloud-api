package smartfloor.service;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import smartfloor.domain.TimeWindow;
import smartfloor.domain.dto.rehabilitation.TestResultForm;
import smartfloor.domain.entities.User;
import smartfloor.domain.entities.Wearable;
import smartfloor.domain.entities.rehabilitation.TestResult;
import smartfloor.domain.entities.rehabilitation.TestSurvey;
import smartfloor.domain.entities.rehabilitation.TestSurveyType;
import smartfloor.domain.entities.rehabilitation.TestTrial;
import smartfloor.domain.entities.rehabilitation.TestType;
import smartfloor.domain.entities.rehabilitation.WearableWithSide;
import smartfloor.domain.exception.TestResultNotFoundException;
import smartfloor.domain.exception.UserIsArchivedException;
import smartfloor.domain.exception.UserNotFoundException;
import smartfloor.domain.exception.WearableNotFoundException;
import smartfloor.domain.indicators.rehabilitation.TargetDistance;
import smartfloor.repository.jpa.TestResultRepository;

@Service
public class RehabilitationService {
    private final TestResultRepository testResultRepository;
    private final UserService userService;
    private final WearableService wearableService;
    private final AuthorizationService authorizationService;

    /**
     * TODO.
     */
    @Autowired
    public RehabilitationService(
            TestResultRepository testResultRepository,
            UserService userService,
            WearableService wearableService,
            AuthorizationService authorizationService
    ) {
        this.testResultRepository = testResultRepository;
        this.userService = userService;
        this.wearableService = wearableService;
        this.authorizationService = authorizationService;
    }

    /**
     * Creates a test result and, if such exist, its trials and surveys.
     */
    public TestResult createTestResult(TestResultForm testResultForm)
            throws UserIsArchivedException, UserNotFoundException, WearableNotFoundException {
        authorizationService.validateUserOperationAuthority(testResultForm.getUserId());

        User testUser = userService.getUser(testResultForm.getUserId());
        if (testUser.isArchived()) {
            throw new UserIsArchivedException();
        }
        TestResult testResult = new TestResult(testResultForm);
        testResult.setUser(testUser);
        boolean isWearableProvided = testResultForm.getWearable() != null;
        if (isWearableProvided) {
            authorizationService.validateCurrentWearableOperationAuthority(testResultForm.getWearable().getId());
            Wearable wearable = wearableService.getWearable(testResultForm.getWearable().getId());
            testResult.setWearableWithSide(new WearableWithSide(wearable, testResultForm.getWearable().getSide()));
        }
        testResult.setTrials(testResultForm.getTrials().stream()
                .map(TestTrial::new)
                .toList()
        );
        testResult.setSurveys(testResultForm.getSurveys().stream()
                .map(TestSurvey::new)
                .toList()
        );
        return testResultRepository.save(testResult);
    }

    /**
     * TODO.
     */
    public TestResult updateTestResult(TestResult testResult, TestResultForm testResultForm) {
        authorizationService.validateUserOperationAuthority(testResultForm.getUserId());

        testResult.updateDetails(testResultForm);
        return testResultRepository.save(testResult);
    }

    public void deleteTestResult(TestResult testResult) {
        testResultRepository.delete(testResult);
    }

    /**
     * TODO.
     */
    public void softDeleteTestResult(TestResult testResult) throws UserIsArchivedException {
        authorizationService.validateUserOperationAuthority(testResult.getUser());

        if (testResult.getUser().isArchived()) {
            throw new UserIsArchivedException();
        }
        testResultRepository.softDelete(testResult.getId());
    }

    public TestResult getTestResultById(Long id) throws TestResultNotFoundException {
        return testResultRepository.findByIdAndDeleted(id, false)
                .orElseThrow(() -> new TestResultNotFoundException(id));
    }

    public List<TestResult> getTestResults() {
        return testResultRepository.findAllByDeleted(false);
    }

    public List<TestType> getTestTypes() {
        return Arrays.asList(TestType.values());
    }

    public List<TestSurveyType> getTestSurveyTypes() {
        return Arrays.asList(TestSurveyType.values());
    }

    /**
     * TODO.
     */
    public List<TestResult> getTestResultsForUserWithinTimeWindow(User user, TimeWindow timeWindow) {
        authorizationService.validateUserOperationAuthority(user);

        return testResultRepository.findAllByUserIdAndBeginTimeGreaterThanEqualAndEndTimeLessThanEqualAndDeleted(
                user.getId(),
                timeWindow.getBeginTime(),
                timeWindow.getEndTime(),
                false
        );
    }

    /**
     * TODO.
     */
    public List<TestResult> getTestResultsOfTypeForUserWithinTimeWindow(
            TestType type,
            User user,
            TimeWindow timeWindow
    ) {
        authorizationService.validateUserOperationAuthority(user);

        return testResultRepository.findAllByUserIdAndTypeAndBeginTimeGreaterThanEqualAndEndTimeLessThanEqualAndDeleted(
                user.getId(),
                type,
                timeWindow.getBeginTime(),
                timeWindow.getEndTime(),
                false
        );
    }

    /**
     * TODO.
     */
    public List<TestResult> getTestResultsForUserWithinTimeWindowIncludingDeleted(User user, TimeWindow timeWindow) {
        authorizationService.validateUserOperationAuthority(user);

        return testResultRepository.findAllByUserIdAndBeginTimeGreaterThanEqualAndEndTimeLessThanEqual(
                user.getId(),
                timeWindow.getBeginTime(),
                timeWindow.getEndTime()
        );
    }

    /**
     * TODO.
     */
    public List<TestResult> getTestResultsForUsers(List<User> users) {
        for (User user : users) {
            authorizationService.validateUserOperationAuthority(user);
        }
        return testResultRepository.findByUserIdInAndDeleted(users.stream()
                .map(User::getId)
                .toList(), false);
    }

    /**
     * TODO.
     */
    public List<TestResult> getTestResultsForUsersIncludingDeleted(List<User> users) {
        for (User user : users) {
            authorizationService.validateUserOperationAuthority(user);
        }

        return testResultRepository.findByUserIdIn(users.stream()
                .map(User::getId)
                .toList());
    }

    /**
     * Calculate the distance a user should walk during the 6MWT. The calculation uses their current UserInfo.
     */
    public TargetDistance calculateTargetDistanceForUser(User user) {

        return new TargetDistance(user.getInfo());
    }
}
