package smartfloor.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import smartfloor.domain.entities.CompositeUser;
import smartfloor.domain.entities.Floor;
import smartfloor.domain.entities.Group;
import smartfloor.domain.entities.User;
import smartfloor.domain.exception.FloorInaccessibleException;
import smartfloor.domain.exception.FloorNotFoundException;
import smartfloor.repository.jpa.FloorRepository;
import smartfloor.repository.jpa.UserRepository;

/**
 * Responsible for handling all logic related floors and the tags that they consist of.
 */
@Service
public class FloorService {

    private final FloorRepository floorRepository;
    private final UserRepository userRepository;
    private final GroupService groupService;

    /**
     * TODO.
     */
    @Autowired
    public FloorService(FloorRepository floorRepository, UserRepository userRepository, GroupService groupService) {
        this.floorRepository = floorRepository;
        this.userRepository = userRepository;
        this.groupService = groupService;
    }

    /**
     * Create a new floor based on the wrapper object that represents the creation dto content.
     */
    public Floor createFloor(Floor floor) {
        return floorRepository.save(floor);
    }

    /**
     * Retrieve a list of all floors available to the authenticated entity. There are two ways to provide access to a
     * floor - through the usage of the table floor_viewers (which links individual users to a floor), and through
     * floor_groups (which links a group (and therefore all its members and managers) to a floor). This service method
     * queries floors based on both of those conditions and combines the results in a set.
     * TODO: At some point, we should remove floor_viewers.
     *
     * @return List of all available floors from the floor repository for a user.
     */
    public List<Floor> getFloors(UserDetails authenticated) {
        Set<Floor> groupFloors = new HashSet<>();

        if (authenticated instanceof User) {
            User authUser = (User) authenticated;
            List<Floor> authFloors = floorRepository.findByViewer(authUser);
            groupFloors.addAll(authFloors);
        }

        if (authenticated instanceof CompositeUser) {
            CompositeUser authUser = (CompositeUser) authenticated;
            // Authenticated as a composite user.
            List<User> subUsers = userRepository.findByCompositeUserId(authUser.getId());
            groupFloors.addAll(subUsers.stream()
                    .map(floorRepository::findByViewer)
                    .flatMap(Collection::stream)
                    .toList());
        }

        List<Group> userGroups = groupService.getGroups();
        for (Group g : userGroups) {
            List<Floor> gFloors = floorRepository.findByGroup(g);
            groupFloors.addAll(gFloors);
        }
        return new ArrayList<>(groupFloors);
    }

    /**
     * Retrieve a floor with the provided identifier and authenticated user.
     */
    public Floor getFloor(UserDetails authenticated, Long floorId)
            throws FloorNotFoundException, FloorInaccessibleException {
        if (floorId == null) throw new FloorNotFoundException();
        Floor floor = floorRepository.findById(floorId).orElseThrow(() -> new FloorNotFoundException(floorId));

        List<Floor> floors = getFloors(authenticated);
        if (floors.isEmpty() || !floors.contains(floor)) {
            throw new FloorInaccessibleException(floorId.toString());
        } else {
            return floor;
        }
    }
}
