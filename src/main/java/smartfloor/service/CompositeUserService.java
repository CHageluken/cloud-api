package smartfloor.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import smartfloor.domain.entities.CompositeUser;
import smartfloor.domain.exception.CompositeUserNotFoundException;
import smartfloor.repository.jpa.CompositeUserRepository;

/**
 * Logic related to user management.
 */
@Service
public class CompositeUserService {
    private final CompositeUserRepository compositeUserRepository;

    @Autowired
    public CompositeUserService(CompositeUserRepository compositeUserRepository) {
        this.compositeUserRepository = compositeUserRepository;
    }


    /**
     * Retrieve a composite user by their identifier.
     */
    public CompositeUser getCompositeUser(Long id) throws CompositeUserNotFoundException {
        return compositeUserRepository.findById(id).orElseThrow(() -> new CompositeUserNotFoundException(id));
    }
}
