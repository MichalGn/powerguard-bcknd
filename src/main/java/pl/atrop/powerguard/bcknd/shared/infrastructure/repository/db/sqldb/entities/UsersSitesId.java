package pl.atrop.powerguard.bcknd.shared.infrastructure.repository.db.sqldb.entities;
import java.io.Serializable; import java.util.Objects;
public class UsersSitesId implements Serializable { public Long userId; public Long siteId; public UsersSitesId(){} public UsersSitesId(Long userId,Long siteId){this.userId=userId;this.siteId=siteId;} @Override public boolean equals(Object o){if(this==o)return true;if(!(o instanceof UsersSitesId x))return false;return Objects.equals(userId,x.userId)&&Objects.equals(siteId,x.siteId);} @Override public int hashCode(){return Objects.hash(userId,siteId);} }
