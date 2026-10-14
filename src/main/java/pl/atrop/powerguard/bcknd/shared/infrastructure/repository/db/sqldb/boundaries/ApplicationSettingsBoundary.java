package pl.atrop.powerguard.bcknd.shared.infrastructure.repository.db.sqldb.boundaries;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import pl.atrop.powerguard.bcknd.shared.infrastructure.repository.db.sqldb.entities.ApplicationSettingsEntity;

@ApplicationScoped
public class ApplicationSettingsBoundary extends AbstractFacade<ApplicationSettingsEntity> {
    @PersistenceContext(unitName = "powerguradPU")
    private EntityManager em;

    public ApplicationSettingsBoundary() { super(ApplicationSettingsEntity.class); }

    @Override
    protected EntityManager getEntityManager() { return em; }

    public ApplicationSettingsEntity getOrCreate() {
        ApplicationSettingsEntity entity = find(1);
        if (entity == null) {
            entity = new ApplicationSettingsEntity(1);
            create(entity);
            em.flush();
        }
        return entity;
    }

    public ApplicationSettingsEntity updateSettings(ApplicationSettingsEntity entity) {
        ApplicationSettingsEntity managed = em.merge(entity);
        em.flush();
        return managed;
    }
}
