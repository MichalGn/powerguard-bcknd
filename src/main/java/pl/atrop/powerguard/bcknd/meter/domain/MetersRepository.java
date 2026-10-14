package pl.atrop.powerguard.bcknd.meter.domain;
import java.util.List; import java.util.Optional;
public interface MetersRepository {
 List<MeterConfiguration> findAll(); Optional<MeterConfiguration> findById(Integer id); Optional<MeterConfiguration> findByIdentity(String clientId,String siteId,String edgeId,String uniqueKey); Optional<MeterConfiguration> findByLegacyIdentity(String siteId,String edgeId,String uniqueKey); boolean uniqueKeyExistsForOtherMeter(Long edgePk,String uniqueKey,Integer id); MeterConfiguration create(MeterConfiguration meter); MeterConfiguration update(MeterConfiguration meter); boolean moveUp(Integer id); boolean moveDown(Integer id); void delete(Integer id);
}
