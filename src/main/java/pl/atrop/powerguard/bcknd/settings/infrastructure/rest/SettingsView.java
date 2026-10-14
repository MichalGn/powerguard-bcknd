package pl.atrop.powerguard.bcknd.settings.infrastructure.rest;

import pl.atrop.powerguard.bcknd.settings.domain.ApplicationSettings;

public record SettingsView(
        String companyName, String installationName, String address, String nip,
        String email, String phone, String contactPerson) {
    public static SettingsView from(ApplicationSettings value) {
        return new SettingsView(value.companyName(), value.installationName(), value.address(), value.nip(),
                value.email(), value.phone(), value.contactPerson());
    }
}
