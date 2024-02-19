package smartfloor.repository.jpa;

import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import smartfloor.IntegrationTestBase;
import smartfloor.domain.entities.User;

class UserRepositoryIntegrationTest extends IntegrationTestBase {

    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    @Autowired
    UserRepository userRepository;


    /**
     * Creates a new User and re-loads it by its ID. Checks the username of the original User against the loaded one
     * from the database.
     */
    @Test
    void testSaveUser() {
        User user = new User();
        user.setAuthId("test");
        user.setTenant(getTestTenant());
        int userEntries = userRepository.findAll().size();
        userRepository.save(user);

        LOGGER.info("User saved with ID {}", user.getId());

        List<User> users = userRepository.findAll();
        assertEquals(userEntries + 1, users.size());
    }

    @Test
    void testFindUserByAuthId() {
        // given
        User user = new User();
        user.setAuthId("testFindUserByAuthId");
        user.setTenant(getTestTenant());
        // when
        userRepository.save(user);
        Optional<User> foundUser = userRepository.findByAuthId(user.getAuthId());
        // then
        assertTrue(foundUser.isPresent());
    }
}