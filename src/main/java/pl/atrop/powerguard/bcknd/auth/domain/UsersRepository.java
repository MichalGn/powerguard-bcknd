package pl.atrop.powerguard.bcknd.auth.domain;

import java.util.List;
import java.util.Optional;

/** Technology-independent storage port. */
public interface UsersRepository {
    Optional<UserAccount> findById(Long id);
    Optional<UserAccount> findByUsername(String username);
    List<UserAccount> findAll();
    boolean isEmpty();
    boolean usernameExistsForOtherUser(String username, Long userId);
    long countEnabledAdmins();
    UserAccount create(UserAccount user);
    UserAccount update(UserAccount user);
    void delete(Long id);
}
