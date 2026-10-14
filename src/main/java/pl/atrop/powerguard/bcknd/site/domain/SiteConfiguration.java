package pl.atrop.powerguard.bcknd.site.domain;

public record SiteConfiguration(Long id, Long clientPk, String clientId, String clientName, String siteId, String name, boolean enabled, int displayOrder) {}
