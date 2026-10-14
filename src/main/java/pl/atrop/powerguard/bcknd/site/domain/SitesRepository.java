package pl.atrop.powerguard.bcknd.site.domain;

import java.util.List; import java.util.Optional;
public interface SitesRepository {
 List<SiteConfiguration> findAll();
 Optional<SiteConfiguration> findById(Long id);
 Optional<SiteConfiguration> findByClientIdAndSiteId(String clientId,String siteId);
 List<SiteConfiguration> findBySiteId(String siteId);
 SiteConfiguration create(SiteConfiguration site);
 SiteConfiguration update(SiteConfiguration site);
 boolean delete(Long id);
}
