package smartfloor.service;

import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import smartfloor.domain.entities.Group;
import smartfloor.domain.entities.Wearable;
import smartfloor.domain.exception.WearableAlreadyExistsException;
import smartfloor.domain.exception.WearableNotFoundException;
import smartfloor.repository.jpa.WearableRepository;

@Service
public class WearableService {

    private final WearableRepository wearableRepository;
    private final AuthorizationService authorizationService;
    private final GroupService groupService;

    /**
     * TODO.
     */
    @Autowired
    public WearableService(
            WearableRepository wearableRepository,
            AuthorizationService authorizationService,
            GroupService groupService
    ) {
        this.wearableRepository = wearableRepository;
        this.authorizationService = authorizationService;
        this.groupService = groupService;
    }

    /**
     * Create/register a new wearable.
     */
    public Wearable createWearable(Wearable wearable) throws WearableAlreadyExistsException {
        try {
            Wearable existingWearable = getWearable(wearable.getId());
            if (existingWearable != null) throw new WearableAlreadyExistsException(wearable.getId());
        } catch (WearableNotFoundException e) {
            return wearableRepository.save(wearable);
        }
        return null;
    }

    /**
     * Retrieves a specific wearable with provided identifier.
     */
    public Wearable getWearable(String wearableId) throws WearableNotFoundException {
        Wearable wearable = wearableRepository.findById(wearableId);
        if (wearable == null) {
            throw new WearableNotFoundException(wearableId);
        }
        return wearable;
    }

    /**
     * TODO.
     */
    public List<Wearable> getWearables() {
        List<Group> groups = groupService.getGroups();
        return groups.stream()
                .map(Group::getWearableGroup)
                .filter(Objects::nonNull)
                .flatMap(wearableGroup -> wearableGroup.getWearables().stream())
                .toList();
    }

    /**
     * TODO: Added during #29: See if this method is really necessary, only useful when we want to update more than just
     * the api key attribute. Otherwise make some refresh api key endpoint.
     */
    public Wearable updateWearable(Wearable wearable) {
        authorizationService.validateCurrentWearableOperationAuthority(wearable);

        return wearableRepository.save(wearable);
    }

    /**
     * TODO.
     */
    public void deleteWearable(String wearableId) throws WearableNotFoundException {
        authorizationService.validateCurrentWearableOperationAuthority(wearableId);

        Wearable wearable = getWearable(wearableId);
        wearableRepository.delete(wearable);
    }
}
