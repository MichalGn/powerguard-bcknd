package pl.atrop.powerguard.bcknd.shared.infrastructure.repository.db.sqldb.repositories;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import pl.atrop.powerguard.bcknd.auth.domain.UserAccount;
import pl.atrop.powerguard.bcknd.auth.domain.UserRole;
import pl.atrop.powerguard.bcknd.auth.domain.UsersRepository;
import pl.atrop.powerguard.bcknd.shared.infrastructure.repository.db.sqldb.boundaries.UsersBoundary;
import pl.atrop.powerguard.bcknd.shared.infrastructure.repository.db.sqldb.boundaries.UsersSitesBoundary;
import pl.atrop.powerguard.bcknd.shared.infrastructure.repository.db.sqldb.entities.Users;

@ApplicationScoped
public class SqlUsersRepository implements UsersRepository {
    @Inject UsersBoundary boundary;
    @Inject UsersSitesBoundary usersSites;

    @Override public Optional<UserAccount> findById(Long id) { return Optional.ofNullable(boundary.find(id)).map(this::toDomain); }
    @Override public Optional<UserAccount> findByUsername(String username) { return boundary.findByUsername(username).map(this::toDomain); }
    @Override public List<UserAccount> findAll() {
        return boundary.findAll().stream().map(this::toDomain)
                .sorted((a, b) -> a.username().compareToIgnoreCase(b.username())).toList();
    }
    @Override public boolean isEmpty() { return boundary.isEmpty(); }
    @Override public boolean usernameExistsForOtherUser(String username, Long userId) { return boundary.usernameExistsForOtherUser(username, userId); }
    @Override public long countEnabledAdmins() { return boundary.countEnabledAdmins(); }

    @Override @Transactional
    public UserAccount create(UserAccount account) {
        Users entity = new Users(account.username(), account.firstname(), account.lastname(), account.email(), account.phone(),
                account.passwordHash(), account.role().name(), account.enabled());
        Users created = boundary.createAccount(entity);
        usersSites.replaceSiteIds(created.getId(), account.siteIds());
        return toDomain(created);
    }

    @Override @Transactional
    public UserAccount update(UserAccount account) {
        Users entity = boundary.find(account.id());
        if (entity == null) throw new IllegalArgumentException("User not found");
        entity.setUsername(account.username());
        entity.setFirstname(account.firstname());
        entity.setLastname(account.lastname());
        entity.setEmail(account.email());
        entity.setPhone(account.phone());
        entity.setPasswordHash(account.passwordHash());
        entity.setRole(account.role().name());
        entity.setEnabled(account.enabled());
        Users updated = boundary.updateAccount(entity);
        usersSites.replaceSiteIds(updated.getId(), account.siteIds());
        return toDomain(updated);
    }

    @Override @Transactional
    public void delete(Long id) {
        usersSites.deleteByUserId(id);
        boundary.deleteById(id);
    }

    private UserAccount toDomain(Users entity) {
        return new UserAccount(entity.getId(), entity.getUsername(), entity.getFirstname(),
                entity.getLastname(), entity.getEmail(), entity.getPhone(), entity.getPasswordHash(),
                UserRole.from(entity.getRole()), entity.isEnabled(), usersSites.findSiteIds(entity.getId()));
    }
}
