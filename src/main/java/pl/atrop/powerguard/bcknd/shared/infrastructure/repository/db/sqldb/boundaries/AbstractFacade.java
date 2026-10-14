package pl.atrop.powerguard.bcknd.shared.infrastructure.repository.db.sqldb.boundaries;

import jakarta.persistence.EntityManager;
import java.util.List;

/** Shared CRUD operations for JPA entity boundaries. */
public abstract class AbstractFacade<T> {
    private final Class<T> entityClass;

    protected AbstractFacade(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    protected abstract EntityManager getEntityManager();

    public void create(T entity) {
        getEntityManager().persist(entity);
    }

    public void edit(T entity) {
        getEntityManager().merge(entity);
    }

    public void remove(T entity) {
        getEntityManager().remove(getEntityManager().merge(entity));
    }

    public T find(Object id) {
        return getEntityManager().find(entityClass, id);
    }

    public List<T> findAll() {
        return getEntityManager().createQuery(
                "SELECT e FROM " + getEntityManager().getMetamodel().entity(entityClass).getName() + " e",
                entityClass).getResultList();
    }

    public long count() {
        return getEntityManager().createQuery(
                "SELECT COUNT(e) FROM " + getEntityManager().getMetamodel().entity(entityClass).getName() + " e",
                Long.class).getSingleResult();
    }
}
