package pl.atrop.powerguard.bcknd.settings.domain;

public interface SettingsService {
    ApplicationSettings get();
    ApplicationSettings update(ApplicationSettingsInput input);
}
