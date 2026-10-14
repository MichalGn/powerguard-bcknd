package pl.atrop.powerguard.bcknd.shared.infrastructure.repository.db.sqldb.repositories;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import pl.atrop.powerguard.bcknd.settings.domain.ApplicationSettings;
import pl.atrop.powerguard.bcknd.settings.domain.SettingsRepository;
import pl.atrop.powerguard.bcknd.shared.infrastructure.repository.db.sqldb.boundaries.ApplicationSettingsBoundary;
import pl.atrop.powerguard.bcknd.shared.infrastructure.repository.db.sqldb.entities.ApplicationSettingsEntity;

import jakarta.transaction.Transactional;
@ApplicationScoped
public class SqlSettingsRepository implements SettingsRepository {
    @Inject ApplicationSettingsBoundary boundary;

    @Override
    @Transactional
    public ApplicationSettings get() {
        return toDomain(boundary.getOrCreate());
    }

    @Override
    @Transactional
    public ApplicationSettings update(ApplicationSettings settings) {
        ApplicationSettingsEntity entity = boundary.getOrCreate();
        entity.setCompanyName(settings.companyName());
        entity.setInstallationName(settings.installationName());
        entity.setAddress(settings.address());
        entity.setNip(settings.nip());
        entity.setEmail(settings.email());
        entity.setPhone(settings.phone());
        entity.setContactPerson(settings.contactPerson());
        return toDomain(boundary.updateSettings(entity));
    }

    private ApplicationSettings toDomain(ApplicationSettingsEntity entity) {
        return new ApplicationSettings(entity.getCompanyName(), entity.getInstallationName(),
                entity.getAddress(), entity.getNip(), entity.getEmail(), entity.getPhone(), entity.getContactPerson());
    }
}
