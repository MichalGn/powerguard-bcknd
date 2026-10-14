package pl.atrop.powerguard.bcknd.site.domain;

public record SiteConfigurationInput(String clientId, String siteId, String name, boolean enabled) {}
