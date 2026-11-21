package com.hellodoc.repository;
import com.hellodoc.entity.AdministrativeActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface AdministrativeActivityRepository extends JpaRepository<AdministrativeActivity,UUID>{}
