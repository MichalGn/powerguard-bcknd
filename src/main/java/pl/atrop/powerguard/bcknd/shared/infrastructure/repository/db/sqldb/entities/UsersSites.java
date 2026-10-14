package pl.atrop.powerguard.bcknd.shared.infrastructure.repository.db.sqldb.entities;
import jakarta.persistence.*;
@Entity @Table(name="users_sites") @IdClass(UsersSitesId.class) public class UsersSites { @Id @Column(name="user_id",nullable=false) private Long userId; @Id @Column(name="site_id",nullable=false) private Long siteId; protected UsersSites(){} public UsersSites(Long userId,Long siteId){this.userId=userId;this.siteId=siteId;} public Long getUserId(){return userId;} public Long getSiteId(){return siteId;} }
