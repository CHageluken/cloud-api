package smartfloor.service;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import smartfloor.domain.TimeWindow;
import smartfloor.domain.dto.FallRiskProfileNoteForm;
import smartfloor.domain.dto.FallRiskProfileRemovalForm;
import smartfloor.domain.entities.Group;
import smartfloor.domain.entities.User;
import smartfloor.domain.entities.UserWearableLink;
import smartfloor.domain.entities.Wearable;
import smartfloor.domain.entities.fall.risk.profile.FallRiskProfile;
import smartfloor.domain.entities.fall.risk.profile.FallRiskProfileNote;
import smartfloor.domain.entities.fall.risk.profile.FallRiskProfileRemoval;
import smartfloor.domain.entities.fall.risk.profile.FallRiskProfileRemovalReason;
import smartfloor.domain.exception.FallRiskProfileNotFoundException;
import smartfloor.repository.jpa.FallRiskProfileNoteRepository;
import smartfloor.repository.jpa.FallRiskProfileRepository;
import smartfloor.repository.jpa.FallRiskRemovalRepository;

@Service
public class FallRiskProfileService {

    private final FallRiskProfileRepository fallRiskProfileRepository;
    private final UserWearableLinkService userWearableLinkService;
    private final AuthorizationService authorizationService;
    private final FallRiskRemovalRepository fallRiskRemovalRepository;
    private final FallRiskProfileNoteRepository fallRiskProfileNoteRepository;

    /**
     * TODO.
     */
    @Autowired
    public FallRiskProfileService(
            FallRiskProfileRepository fallRiskProfileRepository,
            FallRiskRemovalRepository fallRiskRemovalRepository,
            UserWearableLinkService userWearableLinkService,
            AuthorizationService authorizationService,
            FallRiskProfileNoteRepository fallRiskProfileNoteRepository
    ) {
        this.fallRiskProfileRepository = fallRiskProfileRepository;
        this.userWearableLinkService = userWearableLinkService;
        this.authorizationService = authorizationService;
        this.fallRiskRemovalRepository = fallRiskRemovalRepository;
        this.fallRiskProfileNoteRepository = fallRiskProfileNoteRepository;
    }

    /**
     * We look up the fall risk profiles for the user with the given user id within the given time window.
     *
     * @param user the user for which to look up the fall risk profiles
     * @param timeWindow the time window within which to look up the fall risk profiles
     * @return a list of fall risk profiles for the user within the given time window
     */
    List<FallRiskProfile> getFallRiskProfilesForUserWithinTimeWindow(User user, TimeWindow timeWindow) {
        authorizationService.validateUserOperationAuthority(user);

        return fallRiskProfileRepository.findByUserIdBetweenTimes(
                user.getId(),
                timeWindow.getBeginTime(),
                timeWindow.getEndTime()
        );
    }

    /**
     * We look up the fall risk profiles for the user with the given user id within the given time window.
     *
     * @param group the group for which to look up the fall risk profiles
     * @param timeWindow the time window within which to look up the fall risk profiles
     * @return a map of fall risk assessments per user within the given time window
     */
    Map<User, List<FallRiskProfile>> getFallRiskProfilesForGroupWithinTimeWindow(Group group, TimeWindow timeWindow) {
        // stream
        return group.getUsers().stream()
                // map to map entry
                .map(user -> new AbstractMap.SimpleEntry<>(
                        user,
                        getFallRiskProfilesForUserWithinTimeWindow(user, timeWindow)
                ))
                // collect to map
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * We look up the fall risk profiles for the user with the given user id. This means we need to first look up all
     * wearables that the user has worn. Then, for each of these wearables, we look up the fall risk profiles that were
     * created during the time window in which the user has worn the wearable.
     *
     * @param userId the id of the user for which to look up the fall risk profiles
     * @param includeHiddenFRPs whether to include hidden FRPs in the result set
     * @return a list of fall risk profiles for the user
     */
    public List<FallRiskProfile> getFallRiskProfilesForUser(Long userId, boolean includeHiddenFRPs) {
        authorizationService.validateUserOperationAuthority(userId);

        List<UserWearableLink> userWearableLinksForUser = userWearableLinkService.getByUserId(userId);
        return getFallRiskProfilesForUserWearableLinks(userWearableLinksForUser, includeHiddenFRPs);
    }

    /**
     * <p>We look up the fall risk profiles for the user with the given user id.
     * This means we need to first look up all wearables that the user has worn. Then, for each of these wearables, we
     * look up the fall risk profiles that were created during the time window in which the user has worn the
     * wearable.</p> Note: this default method will not include hidden FRPs in the result set.
     *
     * @param userId the id of the user for which to look up the fall risk profiles
     * @return a list of fall risk profiles for the user
     */
    public List<FallRiskProfile> getFallRiskProfilesForUser(Long userId) {
        authorizationService.validateUserOperationAuthority(userId);

        return getFallRiskProfilesForUser(userId, false);
    }

    private List<FallRiskProfile> getFallRiskProfilesForUserWearableLinks(
            List<UserWearableLink> userWearableLinks,
            boolean includeHiddenFRPs
    ) {
        return userWearableLinks.stream()
                .map(uwl -> getFallRiskProfilesForWearableWithinTimeWindow(uwl.getWearable(),
                        new TimeWindow(uwl.getBeginTime(), uwl.getEndTime()), includeHiddenFRPs
                ))
                .flatMap(Collection::stream)
                .distinct()
                .toList();
    }

    /**
     * <p>We query the fall risk profiles of the wearable for which the id is given.
     * A fall risk profile has three temporal properties: its creation time, begin time and end time. The begin and end
     * time are respectively the timestamps of the first and last footstep that the FRP is based on.</p> Here, we look
     * up the fall risk profiles which were created within the given time window's period of time.
     *
     * @return a list of fall risk profiles matching the constraints wearable id + time window
     */
    public List<FallRiskProfile> getFallRiskProfilesForWearableWithinTimeWindow(
            Wearable wearable,
            TimeWindow timeWindow,
            boolean includeHiddenFRPs
    ) {
        if (includeHiddenFRPs) {
            return fallRiskProfileRepository.findByWearableIdAndCreationTimeBetweenOrderByCreationTime(
                    wearable.getId(),
                    timeWindow.getBeginTime(),
                    timeWindow.getEndTime()
            );
        }
        return fallRiskProfileRepository.findByWearableIdAndCreationTimeBetweenAndHiddenFalseOrderByCreationTime(
                wearable.getId(),
                timeWindow.getBeginTime(),
                timeWindow.getEndTime()
        );
    }

    List<FallRiskProfile> getFallRiskProfilesForWearableWithinTimeWindow(Wearable wearable, TimeWindow timeWindow) {
        return getFallRiskProfilesForWearableWithinTimeWindow(wearable, timeWindow, false);
    }

    /**
     * Gets the latest FRP (from the latest FRP session) of a given user.
     */
    Optional<FallRiskProfile> getLatestFallRiskProfileForUser(User user) {
        return fallRiskProfileRepository.findLatestByUserId(user.getId());
    }

    /**
     * Soft deletion of FRP with given ID.
     *
     * @param fallRiskProfileId to be deleted FRP ID
     * @param user authenticated user
     * @param fallRiskProfileRemovalForm contains details for the soft-deletion
     * @throws FallRiskProfileNotFoundException When an FRP with the provided ID does not exist
     */
    public void softDeleteFallRiskProfile(
            Long fallRiskProfileId,
            User user,
            FallRiskProfileRemovalForm fallRiskProfileRemovalForm
    )
            throws FallRiskProfileNotFoundException {

        // Find the profile that must be flagged as removed
        Optional<FallRiskProfile> frp = fallRiskProfileRepository.findById(fallRiskProfileId);
        if (frp.isPresent()) {
            FallRiskProfileRemoval frpRemoval = FallRiskProfileRemoval.builder()
                    .fallRiskProfile(frp.get())
                    .reasonForRemoval(fallRiskProfileRemovalForm.getReasonForRemoval())
                    .specificationOther(fallRiskProfileRemovalForm.getSpecificationOther())
                    .deletedBy(user)
                    .build();
            fallRiskRemovalRepository.save(frpRemoval);
        } else {
            throw new FallRiskProfileNotFoundException(fallRiskProfileId);
        }
    }

    /**
     * Create a FallRiskProfileNote for an FRP. If there already is a note, its value is updated.
     * If the new note value is an empty string, the note is deleted.
     *
     * @param fallRiskProfileId ID of FRP to add a note to
     * @param user Who created the note (authenticated entity)
     * @param fallRiskProfileNoteForm FRP note DTO
     * @return An FRP with an up-to-date note
     * @throws FallRiskProfileNotFoundException Thrown when FRP with the given ID does not exist
     */
    public FallRiskProfile manageFRPNote(
            Long fallRiskProfileId,
            User user,
            FallRiskProfileNoteForm fallRiskProfileNoteForm
    ) throws FallRiskProfileNotFoundException {
        Optional<FallRiskProfile> frp = fallRiskProfileRepository.findById(fallRiskProfileId);
        if (frp.isPresent()) {
            FallRiskProfile fallRiskProfile = frp.get();
            if (fallRiskProfile.getFallRiskProfileNote() == null) {
                // The FRP does not have a note, so we create one.
                FallRiskProfileNote fallRiskProfileNote = FallRiskProfileNote.builder()
                        .value(fallRiskProfileNoteForm.getNoteValue())
                        .createdBy(user)
                        .fallRiskProfile(fallRiskProfile)
                        .build();

                fallRiskProfile.setFallRiskProfileNote(fallRiskProfileNote);
            } else if (!Objects.equals(fallRiskProfileNoteForm.getNoteValue(), "")) {
                // The FRP has a note, so we edit it.
                FallRiskProfileNote existingNote = fallRiskProfile.getFallRiskProfileNote();
                existingNote.setValue(fallRiskProfileNoteForm.getNoteValue());
                fallRiskProfile.setFallRiskProfileNote(existingNote);
            } else {
                // The new note value is an empty string, so we completely delete the note.
                FallRiskProfileNote noteToDelete = fallRiskProfile.getFallRiskProfileNote();
                fallRiskProfile.setFallRiskProfileNote(null);
                fallRiskProfileNoteRepository.deleteById(noteToDelete.id);
            }
            return fallRiskProfileRepository.save(fallRiskProfile);
        } else {
            throw new FallRiskProfileNotFoundException();
        }
    }

    public List<FallRiskProfileRemovalReason> getRemovalReasons() {
        return Arrays.asList(FallRiskProfileRemovalReason.values());
    }
}
