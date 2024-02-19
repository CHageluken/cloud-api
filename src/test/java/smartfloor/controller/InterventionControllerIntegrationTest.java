package smartfloor.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import smartfloor.IntegrationTestBase;
import smartfloor.domain.dto.interventions.CreateInterventionsForm;
import smartfloor.domain.dto.interventions.InterventionForm;
import smartfloor.domain.dto.interventions.UpdateInterventionForm;
import smartfloor.domain.entities.User;
import smartfloor.domain.entities.interventions.FallPreventionProgram;
import smartfloor.domain.entities.interventions.Intervention;
import smartfloor.domain.entities.interventions.InterventionType;
import smartfloor.repository.jpa.GroupRepository;
import smartfloor.repository.jpa.InterventionRepository;
import smartfloor.repository.jpa.UserRepository;
import smartfloor.util.TestUtils;

class InterventionControllerIntegrationTest extends IntegrationTestBase {
    private static final String INTERVENTIONS_REST_ENDPOINT = "/interventions";
    private final ObjectMapper mapper = new ObjectMapper();
    private User authenticatedUser;
    /**
     * This date will be used as the listing endpoint end time.
     */
    LocalDateTime dateTimeNow = LocalDateTime.now();
    /**
     * This date will be used for end time of an intervention (if the intervention is completed).
     */
    LocalDateTime dateTimeWeekAgo = dateTimeNow.minusWeeks(1);
    /**
     * This will be used as an intervention begin time and the listing endpoint begin time.
     */
    LocalDateTime dateTimeMonthAgo = dateTimeNow.minusMonths(1);
    /**
     * This will be used as an earlier intervention begin time.
     */
    LocalDateTime dateTimeYearAgo = dateTimeNow.minusYears(1);

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private GroupRepository groupRepository;
    @Autowired
    private InterventionRepository interventionRepository;


    /**
     * Set up test suite.
     */
    @BeforeEach
    void setUp() {
        groupRepository.deleteAll();
        userRepository.deleteAll();
        authenticatedUser = getTestUser();
    }

    /**
     * Create multiple interventions of different types and assert their fields. Then fetch the interventions for a time
     * frame that must not encapsulate one of the interventions.
     */
    @Test
    void testCreateMultipleInterventions_getUserInterventionsInTimeWindow() throws JsonProcessingException {
        // given: Form that must create 4 new interventions with one request
        CreateInterventionsForm form = getCreateInterventionsForm(authenticatedUser);

        // when: We call creation endpoint
        HttpEntity<String> entity = new HttpEntity<>(mapper.writeValueAsString(form), TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(INTERVENTIONS_REST_ENDPOINT, getPort()),
                HttpMethod.POST,
                entity,
                String.class
        );
        List<Intervention> interventionsResponse =
                mapper.readValue(response.getBody(), new TypeReference<List<Intervention>>() {
                });
        // then: We expect the new interventions to be 4. We assess their fields according to their intervention type.
        assertEquals(form.getInterventions().size(), interventionsResponse.size());
        for (Intervention intervention : interventionsResponse) {
            // Common for all
            assertNotNull(intervention.getBeginTime());
            assertNotNull(intervention.getUser());
            assertNotNull(intervention.getType());
            assertFalse(intervention.isDeleted());
            // Depending on the intervention type (and the specific object), different fields are (not) null
            switch (intervention.getType()) {
                case FALL_PREVENTION_PROGRAM:
                    assertNotNull(intervention.getFallPreventionProgram());
                    assertNull(intervention.getOtherProgram());
                    assertNull(intervention.getEndTime());
                    break;
                case INDIVIDUAL_PHYSIOTHERAPY:
                    assertNull(intervention.getFallPreventionProgram());
                    assertNull(intervention.getOtherProgram());
                    assertNotNull(intervention.getEndTime());
                    break;
                case REGULAR_EXERCISE:
                    assertNull(intervention.getFallPreventionProgram());
                    assertNull(intervention.getOtherProgram());
                    assertNull(intervention.getEndTime());
                    break;
                default: // OTHER
                    assertNull(intervention.getFallPreventionProgram());
                    assertNotNull(intervention.getOtherProgram());
                    assertNotNull(intervention.getEndTime());
                    break;
            }
        }
        // ----
        // then when: We call the listing endpoint with a timeframe
        entity = new HttpEntity<>(null, TestUtils.defaultHttpHeaders());
        response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(
                        INTERVENTIONS_REST_ENDPOINT + String.format(
                                "/users/%d?begin=%d&end=%d",
                                form.getUserId(),
                                dateTimeMonthAgo.toInstant(ZoneOffset.UTC).toEpochMilli(),
                                dateTimeNow.toInstant(ZoneOffset.UTC).toEpochMilli()
                        ),
                        getPort()
                ),
                HttpMethod.GET,
                entity,
                String.class
        );
        interventionsResponse =
                mapper.readValue(response.getBody(), new TypeReference<List<Intervention>>() {
                });
        // then: We expect 3 interventions, since one is out of the specified time frame
        assertEquals(form.getInterventions().size() - 1, interventionsResponse.size());
    }

    /**
     * Try to create and intervention by passing a form with an empty list of interventions. There should be at least
     * one intervention in the list.
     */
    @Test
    void testCreateInterventionsWithEmptyForm() throws JsonProcessingException {
        // given: Form with an empty list of interventions
        CreateInterventionsForm form = new CreateInterventionsForm(List.of(), authenticatedUser.id);

        // when: We call creation endpoint
        HttpEntity<String> entity = new HttpEntity<>(mapper.writeValueAsString(form), TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(INTERVENTIONS_REST_ENDPOINT, getPort()),
                HttpMethod.POST,
                entity,
                String.class
        );

        //then: Error
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    /**
     * Soft delete an intervention, then list interventions to ensure the deletion is successful.
     */
    @Test
    void testSoftDeleteIntervention_GetUserInterventionsInTimeWindow() throws JsonProcessingException {
        // given: Pre-existing interventions
        List<Intervention> initialInterventions = createAndGetInterventions(authenticatedUser);
        int initialNumberOfInterventions = interventionRepository.findAll().size();
        // when: We soft-delete one of the interventions
        HttpEntity<String> entity = new HttpEntity<>(null, TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(
                        INTERVENTIONS_REST_ENDPOINT + "/" + initialInterventions.get(0).getId(),
                        getPort()
                ),
                HttpMethod.DELETE,
                entity,
                String.class
        );
        // then: The deletion should be successful
        assertEquals(HttpStatus.OK, response.getStatusCode());
        //----
        // then when: We call the interventions listing endpoint
        entity = new HttpEntity<>(null, TestUtils.defaultHttpHeaders());
        response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(
                        INTERVENTIONS_REST_ENDPOINT + String.format(
                                "/users/%d?begin=%d&end=%d",
                                authenticatedUser.id,
                                dateTimeMonthAgo.toInstant(ZoneOffset.UTC).toEpochMilli(),
                                dateTimeNow.toInstant(ZoneOffset.UTC).toEpochMilli()
                        ),
                        getPort()
                ),
                HttpMethod.GET,
                entity,
                String.class
        );
        List<Intervention> interventionsResponse =
                mapper.readValue(response.getBody(), new TypeReference<List<Intervention>>() {
                });
        // then: We expect one less than the initial number of interventions
        assertEquals(initialNumberOfInterventions - 1, interventionsResponse.size());
    }

    /**
     * Soft-delete an intervention when the associated user is archived. This is not allowed.
     * Also fetch the interventions again, to ensure an update has not occurred.
     */
    @Test
    void testSoftDeleteInterventionForArchivedUser_GetUserInterventionsInTimeWindow()
            throws JsonProcessingException {
        // given: Pre-existing list of interventions and a deactivated user
        List<Intervention> initialInterventions = createAndGetInterventions(authenticatedUser);
        int initialNumberOfInterventions = interventionRepository.findAll().size();
        authenticatedUser.archive();
        userRepository.save(authenticatedUser);
        // when: We try soft-deleting an intervention
        HttpEntity<String> entity = new HttpEntity<>(null, TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(
                        INTERVENTIONS_REST_ENDPOINT + "/" + initialInterventions.get(0).getId(),
                        getPort()
                ),
                HttpMethod.DELETE,
                entity,
                String.class
        );
        // then: The request must return an error
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        //----
        // then when: We list interventions for the user
        entity = new HttpEntity<>(null, TestUtils.defaultHttpHeaders());
        response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(
                        INTERVENTIONS_REST_ENDPOINT + String.format(
                                "/users/%d?begin=%d&end=%d",
                                authenticatedUser.id,
                                dateTimeMonthAgo.toInstant(ZoneOffset.UTC).toEpochMilli(),
                                dateTimeNow.toInstant(ZoneOffset.UTC).toEpochMilli()
                        ),
                        getPort()
                ),
                HttpMethod.GET,
                entity,
                String.class
        );
        List<Intervention> interventionsResponse =
                mapper.readValue(response.getBody(), new TypeReference<List<Intervention>>() {
                });
        // then: The amount of visible interventions should not have changed
        assertEquals(initialNumberOfInterventions, interventionsResponse.size());
        // finally: Unarchive the user to avoid affecting other tests
        authenticatedUser.unarchive();
        userRepository.save(authenticatedUser);
    }

    /**
     * Update an intervention's end date, then fetch all interventions to ensure the update.
     */
    @Test
    void testUpdateInterventionEndDate_GetUserInterventionsInTimeWindow() throws JsonProcessingException {
        // given: Pre-existing interventions
        List<Intervention> interventions = createAndGetInterventions(authenticatedUser);
        Intervention interventionToUpdate = interventions.get(0);
        UpdateInterventionForm form = UpdateInterventionForm.builder()
                .endTime(dateTimeNow)
                .build();
        // when: We update an intervention with an end time
        HttpEntity<String> entity = new HttpEntity<>(mapper.writeValueAsString(form), TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(
                        INTERVENTIONS_REST_ENDPOINT + "/" + interventionToUpdate.getId(),
                        getPort()
                ),
                HttpMethod.PUT,
                entity,
                String.class
        );
        Intervention interventionResponse =
                mapper.readValue(response.getBody(), new TypeReference<Intervention>() {
                });
        // then: We expect the date to have updated
        assertEquals(dateTimeNow.truncatedTo(ChronoUnit.MILLIS), interventionResponse.getEndTime());
        //----
        // then when: We call the listing endpoint
        entity = new HttpEntity<>(null, TestUtils.defaultHttpHeaders());
        response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(
                        INTERVENTIONS_REST_ENDPOINT + String.format(
                                "/users/%d?begin=%d&end=%d",
                                authenticatedUser.id,
                                dateTimeMonthAgo.toInstant(ZoneOffset.UTC).toEpochMilli(),
                                dateTimeNow.toInstant(ZoneOffset.UTC).toEpochMilli()
                        ),
                        getPort()
                ),
                HttpMethod.GET,
                entity,
                String.class
        );
        List<Intervention> interventionsResponse =
                mapper.readValue(response.getBody(), new TypeReference<List<Intervention>>() {
                });
        interventionToUpdate = interventionsResponse.stream()
                .filter(intervention -> intervention.getEndTime() != null)
                .toList()
                .get(0);
        // then: The intervention must have updated
        assertEquals(dateTimeNow.truncatedTo(ChronoUnit.MILLIS), interventionToUpdate.getEndTime());
    }

    /**
     * Update an intervention's end date when the associated user is archived. This is not allowed.
     * Also fetch the interventions again, to ensure an update has not occurred.
     */
    @Test
    void testUpdateInterventionEndDateForArchivedUser_GetUserInterventionsInTimeWindow()
            throws JsonProcessingException {
        // given: Pre-existing interventions, archived user
        List<Intervention> interventions = createAndGetInterventions(authenticatedUser);
        Intervention interventionToUpdate = interventions.get(0);
        UpdateInterventionForm form = UpdateInterventionForm.builder()
                .endTime(dateTimeNow)
                .build();
        authenticatedUser.archive();
        userRepository.save(authenticatedUser);
        // when: We update an intervention with an end time
        HttpEntity<String> entity = new HttpEntity<>(mapper.writeValueAsString(form), TestUtils.defaultHttpHeaders());
        ResponseEntity<String> response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(
                        INTERVENTIONS_REST_ENDPOINT + "/" + interventionToUpdate.getId(),
                        getPort()
                ),
                HttpMethod.PUT,
                entity,
                String.class
        );
        // then: We expect the endpoint to return an error
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        //----
        // then when: We list interventions for the user
        entity = new HttpEntity<>(null, TestUtils.defaultHttpHeaders());
        response = getRestTemplate().exchange(
                TestUtils.createURLWithPort(
                        INTERVENTIONS_REST_ENDPOINT + String.format(
                                "/users/%d?begin=%d&end=%d",
                                authenticatedUser.id,
                                dateTimeMonthAgo.toInstant(ZoneOffset.UTC).toEpochMilli(),
                                dateTimeNow.toInstant(ZoneOffset.UTC).toEpochMilli()
                        ),
                        getPort()
                ),
                HttpMethod.GET,
                entity,
                String.class
        );
        List<Intervention> interventionsResponse =
                mapper.readValue(response.getBody(), new TypeReference<List<Intervention>>() {
                });
        // then: The intervention must not have updated. We just expect both interventions to have a null end time.
        for (Intervention intervention : interventionsResponse) {
            assertNull(intervention.getEndTime());
        }
        // finally: Unarchive the user to avoid affecting other tests
        authenticatedUser.unarchive();
        userRepository.save(authenticatedUser);
    }

    /**
     * Utility method for constructing a CreateInterventionsForm. We aim to create one intervention of each type, with
     * varying begin/end times.
     */
    private CreateInterventionsForm getCreateInterventionsForm(User u) {
        InterventionForm form1 = InterventionForm.builder()
                .interventionType(InterventionType.REGULAR_EXERCISE)
                .beginTime(dateTimeMonthAgo)
                .build();
        InterventionForm form2 = InterventionForm.builder()
                .interventionType(InterventionType.INDIVIDUAL_PHYSIOTHERAPY)
                .beginTime(dateTimeMonthAgo)
                .endTime(dateTimeWeekAgo)
                .build();
        InterventionForm form3 = InterventionForm.builder()
                .interventionType(InterventionType.FALL_PREVENTION_PROGRAM)
                .fallPreventionProgram(FallPreventionProgram.IN_BALANS)
                .beginTime(dateTimeYearAgo)
                .build();
        InterventionForm form4 = InterventionForm.builder()
                .interventionType(InterventionType.OTHER)
                .otherProgram("Some other program")
                .beginTime(dateTimeMonthAgo)
                .endTime(dateTimeNow)
                .build();
        return new CreateInterventionsForm(List.of(form1, form2, form3, form4), u.id);
    }

    /**
     * Create 2 interventions with no specific details.
     */
    private List<Intervention> createAndGetInterventions(User u) {
        List<Intervention> interventions = List.of(
                Intervention.builder()
                        .user(u)
                        .type(InterventionType.REGULAR_EXERCISE)
                        .beginTime(dateTimeMonthAgo)
                        .build(),
                Intervention.builder()
                        .user(u)
                        .type(InterventionType.REGULAR_EXERCISE)
                        .beginTime(dateTimeMonthAgo)
                        .build()
        );
        return interventionRepository.saveAll(interventions);
    }
}
