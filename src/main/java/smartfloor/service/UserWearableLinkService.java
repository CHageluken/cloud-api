package smartfloor.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import smartfloor.domain.TimeWindow;
import smartfloor.domain.entities.Group;
import smartfloor.domain.entities.User;
import smartfloor.domain.entities.UserWearableLink;
import smartfloor.domain.entities.Wearable;
import smartfloor.domain.exception.UserIsArchivedException;
import smartfloor.domain.exception.WearableInUseException;
import smartfloor.repository.jpa.UserWearableLinkRepository;

/**
 * Service class responsible for handling business logic relating to user wearable links.
 */
@Service
public class UserWearableLinkService {

    private final UserWearableLinkRepository<UserWearableLink> userWearableLinkRepository;
    private final AuthorizationService authorizationService;

    /**
     * TODO.
     */
    @Autowired
    public UserWearableLinkService(
            UserWearableLinkRepository<UserWearableLink> userWearableLinkRepository,
            AuthorizationService authorizationService
    ) {
        this.userWearableLinkRepository = userWearableLinkRepository;
        this.authorizationService = authorizationService;
    }

    /**
     * <p>Create a new user-wearable link from the provided UWL object.</p>
     * Note: the UWL can only be created (persisted) if there are no existing links (for the given wearable) that
     * overlap it.
     * In principle, we maintain the invariant that there can only be one overlapping UWL for any given wearable.
     * Namely, the only overlapping UWL for the given wearable should be an active UWL (i.e. no end time set) that
     * has not been completed (ended) yet. In that case, we throw a "wearable in use" exception.
     *
     * @param userWearableLink the user-wearable link to be created
     * @return the created user-wearable link
     * @throws WearableInUseException if there already exists an (active) user-wearable link for the given wearable
     */
    @Transactional
    public UserWearableLink createUserWearableLink(UserWearableLink userWearableLink)
            throws WearableInUseException, UserIsArchivedException {
        authorizationService.validateUserOperationAuthority(userWearableLink.getUser());

        if (userWearableLink.getUser().isArchived()) {
            throw new UserIsArchivedException();
        }
        /* The wearable that we want to link the user with is possibly still in active use. */
        Optional<UserWearableLink> possiblyOverlapping =
                userWearableLinkRepository
                        .findLatestByWearableIdWithOverlappingTimeWindow(userWearableLink.getWearable()
                                .getId(), userWearableLink.getBeginTime(), userWearableLink.getEndTime());
        if (possiblyOverlapping.isPresent()) {
            UserWearableLink overlapping = possiblyOverlapping.get();
            boolean isLinkedToSameUser = overlapping.getUser().equals(userWearableLink.getUser());
            if (isLinkedToSameUser) { // then the overlapping link must still be active for that user
                return overlapping; // just return the overlapping link as the active one for the user
            } else { // then the overlapping link is linked to a different user, can't link: throw an "in use" exception
                throw new WearableInUseException(overlapping.getWearable().getId(), overlapping.getUser().getId());
            }
        }
        /* The user that we want to link a wearable to is possibly currently wearing a different wearable.
        We close any active links that we can find for the user directly. We use a repository method for this that
        executes a native query on the database directly since issues were encountered with letting Hibernate / JPA
        handle this (through updating the active UWL entity and persisting it).
        */
        userWearableLinkRepository.completeActiveByUserId(userWearableLink.getUser().getId());
        userWearableLink.setBeginTime(LocalDateTime.now());
        return userWearableLinkRepository.saveAndFlush(userWearableLink);
    }

    /**
     * Find all user wearable links for a given user.
     */
    public List<UserWearableLink> getByUserId(Long userId) {
        authorizationService.validateUserOperationAuthority(userId);
        return userWearableLinkRepository.findByUserIdOrderByBeginTimeAsc(userId, UserWearableLink.class);
    }

    /**
     * <p>Find all user wearable links for a given user that overlap the given time window W.</p>
     * This method is useful when (for example) we want to fetch all footsteps for a given user within W.
     * We would first obtain the user wearable links for the user that overlap W and then lookup all the footsteps
     * within W for the wearables obtained from the links returned from this method.
     *
     * @return a list of all user wearable links for the user that overlap the given time window W
     */
    public List<UserWearableLink> getByUserWithOverlappingTimeWindow(User user, TimeWindow timeWindow) {
        authorizationService.validateUserOperationAuthority(user);
        return  userWearableLinkRepository.findAllByUserIdWithOverlappingTimeWindow(
                        user.getId(),
                        timeWindow.getBeginTime(),
                        timeWindow.getEndTime()
                );
    }

    /**
     * TODO.
     */
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserWearableLink> getByOverlappingTimeWindow(TimeWindow timeWindow) {
        return userWearableLinkRepository.findAllByOverlappingTimeWindow(
                timeWindow.getBeginTime(),
                timeWindow.getEndTime()
        );
    }

    /**
     * Find all user wearable links for a given wearable.
     */
    public List<UserWearableLink> getByWearableId(String wearableId) {
        return userWearableLinkRepository.findByWearableId(wearableId, UserWearableLink.class);
    }

    /**
     * TODO.
     */
    public Optional<UserWearableLink> getActiveByWearable(Wearable wearable) {
        return userWearableLinkRepository.findActiveByWearableId(wearable.getId());
    }

    /**
     * TODO.
     */
    public Optional<UserWearableLink> getActiveByUser(User user) {
        authorizationService.validateUserOperationAuthority(user);

        return userWearableLinkRepository.findActiveByUserId(user.getId());
    }

    /**
     * Update a user wearable link of an existing user.
     */
    public UserWearableLink updateUserWearableLink(UserWearableLink userWearableLink) {
        authorizationService.validateUserOperationAuthority(userWearableLink.getUser());

        return userWearableLinkRepository.save(userWearableLink);
    }

    /**
     * Remove a user wearable link.
     */
    public void deleteUserWearableLink(UserWearableLink userWearableLink) {
        authorizationService.validateUserOperationAuthority(userWearableLink.getUser());

        userWearableLinkRepository.delete(userWearableLink);
    }

    /**
     * TODO.
     */
    public List<UserWearableLink> getActiveWearableLinksForGroup(Group group) {
        List<Long> userIds = group.getUsers().stream().map(User::getId).toList();
        return userWearableLinkRepository.findActiveByUserIds(userIds);
    }

    /**
     * TODO.
     */
    public Boolean isWearableActive(Wearable wearable) {
        authorizationService.validateCurrentWearableOperationAuthority(wearable);
        return userWearableLinkRepository.findActiveByWearableId(wearable.getId()).isPresent();
    }
}
