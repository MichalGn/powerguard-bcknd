package pl.atrop.powerguard.bcknd.auth.domain;

import java.util.List;
import java.util.Optional;

/** Business operations for authentication, self-editing and managed user administration. */
public interface UsersService {
    Optional<UserAccount> authenticate(String username, String passwordMd5);
    boolean initializeAdminIfEmpty(String username, String initialPasswordMd5);

    Optional<UserAccount> findById(Long id);
    List<UserAccount> findAll();
    List<UserAccount> findManagedUsers(Long actorUserId);
    Optional<UserAccount> updateSelf(Long userId, UserProfileUpdate update);

    Optional<UserAccount> createManagedUser(Long actorUserId, ManagedUserInput input);
    Optional<UserAccount> updateManagedUser(Long actorUserId, Long targetUserId, ManagedUserInput input);
    boolean deleteManagedUser(Long actorUserId, Long targetUserId);

    boolean canAccessSite(UserAccount user, Long siteId);
}
