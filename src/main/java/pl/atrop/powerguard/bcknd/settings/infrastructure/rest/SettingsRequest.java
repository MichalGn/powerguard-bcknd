package pl.atrop.powerguard.bcknd.settings.infrastructure.rest;

public record SettingsRequest(
        String companyName, String installationName, String address, String nip,
        String email, String phone, String contactPerson) {
}
