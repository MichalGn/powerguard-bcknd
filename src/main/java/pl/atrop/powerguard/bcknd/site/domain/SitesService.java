package pl.atrop.powerguard.bcknd.site.domain;
import java.util.List; import java.util.Optional;
public interface SitesService {
 List<SiteConfiguration> findPublicSites();
 List<SiteConfiguration> findAll();
 Optional<SiteConfiguration> findById(Long id);
 Optional<SiteConfiguration> findByIdentity(String clientId,String siteId);
 Optional<SiteConfiguration> findUniqueBySiteId(String siteId);
 Optional<SiteConfiguration> create(SiteConfigurationInput input);
 Optional<SiteConfiguration> update(String clientId,String siteId,SiteConfigurationInput input);
 boolean delete(String clientId,String siteId);
 SiteConfiguration ensure(String clientId,String siteId);
}
