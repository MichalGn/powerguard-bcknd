package pl.atrop.powerguard.bcknd.edge.domain;
import java.util.Optional;
public interface EdgesService { Optional<EdgeIdentity> findByIdentity(String clientId,String siteId,String edgeId); EdgeIdentity ensure(String clientId,String siteId,String edgeId); }
