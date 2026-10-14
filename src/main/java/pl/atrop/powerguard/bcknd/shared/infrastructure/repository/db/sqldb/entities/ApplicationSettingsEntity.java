package pl.atrop.powerguard.bcknd.shared.infrastructure.repository.db.sqldb.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "application_settings")
public class ApplicationSettingsEntity {
    @Id
    private Integer id;

    @Column(name = "company_name", length = 255)
    private String companyName;

    @Column(name = "installation_name", length = 255)
    private String installationName;

    @Column(name = "address", length = 1000)
    private String address;

    @Column(name = "nip", length = 32)
    private String nip;

    @Column(name = "email", length = 320)
    private String email;

    @Column(name = "phone", length = 64)
    private String phone;

    @Column(name = "contact_person", length = 255)
    private String contactPerson;

    protected ApplicationSettingsEntity() { }

    public ApplicationSettingsEntity(Integer id) { this.id = id; }

    public Integer getId() { return id; }
    public String getCompanyName() { return companyName; }
    public String getInstallationName() { return installationName; }
    public String getAddress() { return address; }
    public String getNip() { return nip; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getContactPerson() { return contactPerson; }

    public void setCompanyName(String value) { companyName = value; }
    public void setInstallationName(String value) { installationName = value; }
    public void setAddress(String value) { address = value; }
    public void setNip(String value) { nip = value; }
    public void setEmail(String value) { email = value; }
    public void setPhone(String value) { phone = value; }
    public void setContactPerson(String value) { contactPerson = value; }
}
