package pl.atrop.powerguard.bcknd.shared.infrastructure.repository.db.sqldb.entities;
import jakarta.persistence.*;
@Entity @Table(name="clients", uniqueConstraints=@UniqueConstraint(name="uq_clients_client_id",columnNames="client_id"))
public class Clients {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="client_id",nullable=false,length=128) private String clientId;
 @Column(name="name",nullable=false,length=255) private String name;
 @Column(name="enabled",nullable=false) private boolean enabled;
 @Column(name="display_order",nullable=false) private int displayOrder;
 protected Clients(){}
 public Clients(String clientId,String name,boolean enabled,int displayOrder){this.clientId=clientId;this.name=name;this.enabled=enabled;this.displayOrder=displayOrder;}
 public Long getId(){return id;} public String getClientId(){return clientId;} public String getName(){return name;} public boolean isEnabled(){return enabled;} public int getDisplayOrder(){return displayOrder;}
 public void setName(String v){name=v;} public void setEnabled(boolean v){enabled=v;} public void setDisplayOrder(int v){displayOrder=v;}
}
