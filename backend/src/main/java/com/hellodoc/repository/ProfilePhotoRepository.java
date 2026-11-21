package com.hellodoc.repository;
import com.hellodoc.entity.ProfilePhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface ProfilePhotoRepository extends JpaRepository<ProfilePhoto,UUID>{}
