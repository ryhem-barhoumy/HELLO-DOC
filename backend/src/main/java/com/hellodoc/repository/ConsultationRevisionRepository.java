package com.hellodoc.repository;
import com.hellodoc.entity.ConsultationRevision;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface ConsultationRevisionRepository extends JpaRepository<ConsultationRevision,UUID> {
    List<ConsultationRevision> findTop50ByNoteIdOrderByChangedAtDesc(UUID noteId);
}
