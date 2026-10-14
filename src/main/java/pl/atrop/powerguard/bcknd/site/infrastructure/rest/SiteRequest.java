package pl.atrop.powerguard.bcknd.site.infrastructure.rest;
public record SiteRequest(String clientId,String siteId,String name,boolean enabled) {}
