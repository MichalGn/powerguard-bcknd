package pl.atrop.powerguard.bcknd.shared.infrastructure.repository.db.sqldb.entities;
import jakarta.persistence.*;
@Entity @Table(name="edges",uniqueConstraints=@UniqueConstraint(name="uq_edges_site_edge",columnNames={"site_fk","edge_id"}))
public class Edges {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(name="site_fk",nullable=false) private Long siteFk; @Column(name="edge_id",nullable=false,length=128) private String edgeId;
 protected Edges(){} public Edges(Long siteFk,String edgeId){this.siteFk=siteFk;this.edgeId=edgeId;} public Long getId(){return id;} public Long getSiteFk(){return siteFk;} public String getEdgeId(){return edgeId;}
}
