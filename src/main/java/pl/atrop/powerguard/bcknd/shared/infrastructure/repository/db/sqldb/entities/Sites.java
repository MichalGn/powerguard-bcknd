package pl.atrop.powerguard.bcknd.shared.infrastructure.repository.db.sqldb.entities;
import jakarta.persistence.*;
@Entity @Table(name="sites",uniqueConstraints=@UniqueConstraint(name="uq_sites_client_site",columnNames={"client_fk","site_id"}))
public class Sites {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="client_fk",nullable=false) private Long clientFk;
 @Column(name="site_id",nullable=false,length=128) private String siteId;
 @Column(name="name",nullable=false,length=255) private String name;
 @Column(name="enabled",nullable=false) private boolean enabled;
 @Column(name="display_order",nullable=false) private int displayOrder;
 protected Sites(){} public Sites(Long clientFk,String siteId,String name,boolean enabled,int displayOrder){this.clientFk=clientFk;this.siteId=siteId;this.name=name;this.enabled=enabled;this.displayOrder=displayOrder;}
 public Long getId(){return id;} public Long getClientFk(){return clientFk;} public String getSiteId(){return siteId;} public String getName(){return name;} public boolean isEnabled(){return enabled;} public int getDisplayOrder(){return displayOrder;}
 public void setName(String v){name=v;} public void setEnabled(boolean v){enabled=v;} public void setDisplayOrder(int v){displayOrder=v;}
}
