package com.aidoctor.model;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "users")
public class AppUser {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true)
    private String username;
    private String passwordHash;
    private String role = "USER";

    @OneToMany(mappedBy = "owner")
    private List<Report> reports;

    // getters/setters
    public Long getId(){return id;}
    public void setId(Long id){this.id = id;}
    public String getUsername(){return username;}
    public void setUsername(String username){this.username = username;}
    public String getPasswordHash(){return passwordHash;}
    public void setPasswordHash(String passwordHash){this.passwordHash = passwordHash;}
    public String getRole(){return role;}
    public void setRole(String role){this.role = role;}
    public List<Report> getReports(){return reports;}
    public void setReports(List<Report> reports){this.reports = reports;}
}
