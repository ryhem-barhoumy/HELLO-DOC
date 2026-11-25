package com.hellodoc.repository;
import com.hellodoc.entity.TeleconsultationMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;
public interface TeleconsultationMessageRepository extends JpaRepository<TeleconsultationMessage,UUID>{
 List<TeleconsultationMessage> findTop200BySession_IdOrderBySentAtAsc(UUID sessionId);
}
