package pl.atrop.powerguard.bcknd.auth.domain;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import pl.atrop.powerguard.bcknd.site.domain.SitesRepository;

/** User business rules stay independent of persistence and REST. */
@ApplicationScoped
public class DomainUsersService implements UsersService {
    @Inject UsersRepository usersRepository;
    @Inject PasswordHasherPort passwords;
    @Inject SitesRepository sitesRepository;

    @Override
    public Optional<UserAccount> authenticate(String username, String passwordMd5) {
        if (!validUsername(username) || !validMd5(passwordMd5)) return Optional.empty();
        return usersRepository.findByUsername(username.trim())
                .filter(UserAccount::enabled)
                .filter(user -> passwords.verify(passwordMd5.trim(), user.passwordHash()));
    }

    @Override
    public boolean initializeAdminIfEmpty(String username, String initialPasswordMd5) {
        if (!usersRepository.isEmpty()) return false;
        usersRepository.create(new UserAccount(null, username.trim(), null, null, null, null,
                passwords.hash(initialPasswordMd5), UserRole.ADMIN, true, Set.of()));
        return true;
    }

    @Override public Optional<UserAccount> findById(Long id) { return id == null ? Optional.empty() : usersRepository.findById(id); }
    @Override public List<UserAccount> findAll() { return usersRepository.findAll(); }

    @Override
    public List<UserAccount> findManagedUsers(Long actorUserId) {
        UserAccount actor = usersRepository.findById(actorUserId).orElse(null);
        if (actor == null || !actor.enabled()) return List.of();
        if (actor.role() == UserRole.ADMIN) return usersRepository.findAll();
        if (actor.role() == UserRole.INSTALATOR) {
            return usersRepository.findAll().stream().filter(u -> u.role() == UserRole.USER).toList();
        }
        return List.of();
    }

    @Override
    public Optional<UserAccount> updateSelf(Long userId, UserProfileUpdate update) {
        if (userId == null || update == null || !validUsername(update.username())) return Optional.empty();
        if (update.passwordMd5() != null && !update.passwordMd5().isBlank() && !validMd5(update.passwordMd5())) return Optional.empty();
        UserAccount existing = usersRepository.findById(userId).orElse(null);
        if (existing == null || !existing.enabled()) return Optional.empty();
        String username = update.username().trim();
        if (usersRepository.usernameExistsForOtherUser(username, userId)) return Optional.empty();
        String hash = existing.passwordHash();
        if (update.passwordMd5() != null && !update.passwordMd5().isBlank()) hash = passwords.hash(update.passwordMd5().trim());
        return Optional.of(usersRepository.update(new UserAccount(existing.id(), username, cleanName(update.firstname()),
                cleanName(update.lastname()), cleanEmail(update.email()), cleanPhone(update.phone()), hash,
                existing.role(), existing.enabled(), existing.siteIds())));
    }

    @Override
    public Optional<UserAccount> createManagedUser(Long actorUserId, ManagedUserInput input) {
        UserAccount actor = usersRepository.findById(actorUserId).orElse(null);
        if (!canManage(actor) || !validManagedInput(input, true)) return Optional.empty();
        UserRole role = input.role() == null ? UserRole.USER : input.role();
        if (actor.role() == UserRole.INSTALATOR && role != UserRole.USER) return Optional.empty();
        Set<Long> siteIds = normalizedSiteIds(input.siteIds());
        if (!validAssignedSites(actor, role, siteIds)) return Optional.empty();
        String username = input.username().trim();
        if (usersRepository.usernameExistsForOtherUser(username, null)) return Optional.empty();
        UserAccount created = new UserAccount(null, username, cleanName(input.firstname()), cleanName(input.lastname()),
                cleanEmail(input.email()), cleanPhone(input.phone()), passwords.hash(input.passwordMd5().trim()),
                role, input.enabled(), role == UserRole.ADMIN ? Set.of() : siteIds);
        return Optional.of(usersRepository.create(created));
    }

    @Override
    public Optional<UserAccount> updateManagedUser(Long actorUserId, Long targetUserId, ManagedUserInput input) {
        if (actorUserId == null || targetUserId == null || !validManagedInput(input, false)) return Optional.empty();
        UserAccount actor = usersRepository.findById(actorUserId).orElse(null);
        UserAccount existing = usersRepository.findById(targetUserId).orElse(null);
        if (!canManage(actor) || existing == null) return Optional.empty();
        if (actor.role() == UserRole.INSTALATOR && existing.role() != UserRole.USER) return Optional.empty();

        UserRole role = input.role() == null ? UserRole.USER : input.role();
        if (actor.role() == UserRole.INSTALATOR && role != UserRole.USER) return Optional.empty();
        Set<Long> siteIds = normalizedSiteIds(input.siteIds());
        if (!validAssignedSites(actor, role, siteIds)) return Optional.empty();

        String username = input.username().trim();
        if (usersRepository.usernameExistsForOtherUser(username, targetUserId)) return Optional.empty();
        String hash = existing.passwordHash();
        if (input.passwordMd5() != null && !input.passwordMd5().isBlank()) {
            if (!validMd5(input.passwordMd5())) return Optional.empty();
            hash = passwords.hash(input.passwordMd5().trim());
        }
        return Optional.of(usersRepository.update(new UserAccount(existing.id(), username, cleanName(input.firstname()),
                cleanName(input.lastname()), cleanEmail(input.email()), cleanPhone(input.phone()), hash, role, input.enabled(),
                role == UserRole.ADMIN ? Set.of() : siteIds)));
    }

    @Override
    public boolean deleteManagedUser(Long actorUserId, Long targetUserId) {
        if (actorUserId == null || targetUserId == null || actorUserId.equals(targetUserId)) return false;
        UserAccount actor = usersRepository.findById(actorUserId).orElse(null);
        UserAccount existing = usersRepository.findById(targetUserId).orElse(null);
        if (!canManage(actor) || existing == null) return false;
        if (actor.role() == UserRole.INSTALATOR && existing.role() != UserRole.USER) return false;
        usersRepository.delete(targetUserId);
        return true;
    }

    @Override
    public boolean canAccessSite(UserAccount user, Long siteId) {
        if (user == null || !user.enabled() || siteId == null) return false;
        return user.role() == UserRole.ADMIN || user.siteIds().contains(siteId);
    }

    private boolean canManage(UserAccount actor) {
        return actor != null && actor.enabled() && (actor.role() == UserRole.ADMIN || actor.role() == UserRole.INSTALATOR);
    }

    private boolean validAssignedSites(UserAccount actor, UserRole targetRole, Set<Long> siteIds) {
        if (targetRole == UserRole.ADMIN) return actor.role() == UserRole.ADMIN;
        Set<Long> existing = sitesRepository.findAll().stream().map(s -> s.id()).collect(java.util.stream.Collectors.toSet());
        if (!existing.containsAll(siteIds)) return false;
        return actor.role() == UserRole.ADMIN || actor.siteIds().containsAll(siteIds);
    }

    private Set<Long> normalizedSiteIds(Set<Long> values) {
        if (values == null || values.isEmpty()) return Set.of();
        Set<Long> result = new LinkedHashSet<>();
        values.stream().filter(java.util.Objects::nonNull).forEach(result::add);
        return Set.copyOf(result);
    }

    private boolean validManagedInput(ManagedUserInput input, boolean passwordRequired) {
        if (input == null || !validUsername(input.username())) return false;
        if (passwordRequired) return validMd5(input.passwordMd5());
        return input.passwordMd5() == null || input.passwordMd5().isBlank() || validMd5(input.passwordMd5());
    }
    private boolean validUsername(String value) { return value != null && !value.trim().isEmpty() && value.trim().length() <= 64; }
    private boolean validMd5(String value) { return value != null && value.trim().matches("(?i)[0-9a-f]{32}"); }
    private String cleanName(String value) { return clean(value, 64); }
    private String cleanEmail(String value) { return clean(value, 320); }
    private String cleanPhone(String value) { return clean(value, 32); }
    private String clean(String value, int maxLength) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed.substring(0, Math.min(maxLength, trimmed.length()));
    }
}
