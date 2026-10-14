package pl.atrop.powerguard.bcknd.edge.domain;
import java.util.List; import java.util.Optional;
public interface EdgesRepository {
 Optional<EdgeIdentity> findById(Long id); Optional<EdgeIdentity> findByIdentity(Long sitePk,String edgeId); List<EdgeIdentity> findAll(); EdgeIdentity create(EdgeIdentity edge); boolean delete(Long id);
}
