package pl.atrop.powerguard.bcknd.shared.infrastructure.repository.db.sqldb.boundaries;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Optional;
import pl.atrop.powerguard.bcknd.shared.infrastructure.repository.db.sqldb.entities.Users;

@ApplicationScoped
public class UsersBoundary extends AbstractFacade<Users> {
    @PersistenceContext(unitName = "powerguradPU")
    private EntityManager em;

    public UsersBoundary() {
        super(Users.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public Optional<Users> findByUsername(String username) {
        return em.createQuery("SELECT u FROM Users u WHERE u.username = :username", Users.class)
                .setParameter("username", username)
                .setMaxResults(1)
                .getResultStream().findFirst();
    }

    public boolean usernameExistsForOtherUser(String username, Long userId) {
        String jpql = userId == null
                ? "SELECT COUNT(u) FROM Users u WHERE u.username = :username"
                : "SELECT COUNT(u) FROM Users u WHERE u.username = :username AND u.id <> :userId";
        var query = em.createQuery(jpql, Long.class).setParameter("username", username);
        if (userId != null) query.setParameter("userId", userId);
        return query.getSingleResult() > 0;
    }

    public long countEnabledAdmins() {
        return em.createQuery(
                "SELECT COUNT(u) FROM Users u WHERE u.role = 'ADMIN' AND u.enabled = true", Long.class)
                .getSingleResult();
    }

    public boolean isEmpty() {
        return count() == 0;
    }

    public Users createAccount(Users user) {
        create(user);
        em.flush();
        return user;
    }

    public Users updateAccount(Users user) {
        Users managed = editAndReturn(user);
        em.flush();
        return managed;
    }

    public void deleteById(Long id) {
        Users user = find(id);
        if (user != null) {
            remove(user);
            em.flush();
        }
    }

    private Users editAndReturn(Users entity) {
        return em.merge(entity);
    }
}
