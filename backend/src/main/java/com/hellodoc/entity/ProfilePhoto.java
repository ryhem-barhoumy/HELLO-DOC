package com.hellodoc.entity;
import jakarta.persistence.*;
import java.util.UUID;
@Entity @Table(name="profile_photos")
public class ProfilePhoto {
 @Id private UUID id;
 @OneToOne(fetch=FetchType.LAZY,optional=false) @MapsId @JoinColumn(name="user_id") private UserAccount user;
 @Lob @Column(nullable=false,length=1_048_576) private byte[] jpeg;
 protected ProfilePhoto(){}
 public ProfilePhoto(UserAccount user){this.user=user;}
 public void replace(byte[] jpeg){this.jpeg=jpeg;}
 public byte[] getJpeg(){return jpeg;}
}
