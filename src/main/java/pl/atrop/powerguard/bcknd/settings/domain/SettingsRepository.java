package pl.atrop.powerguard.bcknd.settings.domain;

public interface SettingsRepository {
    ApplicationSettings get();
    ApplicationSettings update(ApplicationSettings settings);
}
