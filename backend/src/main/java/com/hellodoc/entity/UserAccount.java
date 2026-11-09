package com.hellodoc.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email"))
public class UserAccount {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(nullable = false, length = 100) private String firstName;
    @Column(nullable = false, length = 100) private String lastName;
    @Column(nullable = false, length = 254) private String email;
    @Column(length = 32) private String phone;
    @Column(length = 300) private String address;
    @Column(nullable = false, length = 255) private String passwordHash;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private UserRole role;
    @Column(nullable = false, length = 20) private String status;
    protected UserAccount() { }
    public UserAccount(String firstName, String lastName, String email, String passwordHash, UserRole role, String status) {
        this(firstName,lastName,email,null,null,passwordHash,role,status);
    }
    public UserAccount(String firstName,String lastName,String email,String phone,String address,String passwordHash,UserRole role,String status) {
        this.firstName=firstName; this.lastName=lastName; this.email=email; this.phone=phone; this.address=address; this.passwordHash=passwordHash; this.role=role; this.status=status;
    }
    public UUID getId(){return id;} public String getFirstName(){return firstName;} public String getLastName(){return lastName;} public String getPhone(){return phone;} public String getAddress(){return address;}
    public String getEmail(){return email;} public String getPasswordHash(){return passwordHash;} public UserRole getRole(){return role;} public String getStatus(){return status;}
    public void updateProfile(String firstName,String lastName){this.firstName=firstName;this.lastName=lastName;}
    public void updateContactProfile(String firstName,String lastName,String phone,String address){this.firstName=firstName;this.lastName=lastName;this.phone=phone;this.address=address;}
    public void changePassword(String passwordHash){this.passwordHash=passwordHash;}
    public void setRoleAndStatus(UserRole role,String status){this.role=role;this.status=status;}
    public void setStatus(String status){this.status=status;}
}
