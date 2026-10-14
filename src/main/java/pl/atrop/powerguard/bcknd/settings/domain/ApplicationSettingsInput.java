package pl.atrop.powerguard.bcknd.settings.domain;

public record ApplicationSettingsInput(
        String companyName,
        String installationName,
        String address,
        String nip,
        String email,
        String phone,
        String contactPerson) {
}
