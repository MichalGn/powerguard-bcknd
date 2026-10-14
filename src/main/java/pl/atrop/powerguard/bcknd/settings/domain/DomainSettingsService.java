package pl.atrop.powerguard.bcknd.settings.domain;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DomainSettingsService implements SettingsService {
    @Inject SettingsRepository repository;

    @Override
    public ApplicationSettings get() {
        return repository.get();
    }

    @Override
    public ApplicationSettings update(ApplicationSettingsInput input) {
        if (input == null) throw new IllegalArgumentException("Settings are required.");
        return repository.update(new ApplicationSettings(
                clean(input.companyName(), 255),
                clean(input.installationName(), 255),
                clean(input.address(), 1000),
                clean(input.nip(), 32),
                clean(input.email(), 320),
                clean(input.phone(), 64),
                clean(input.contactPerson(), 255)));
    }

    private String clean(String value, int maxLength) {
        if (value == null) return null;
        String trimmed = value.trim();
        if (trimmed.isEmpty()) return null;
        return trimmed.substring(0, Math.min(maxLength, trimmed.length()));
    }
}
