package com.hellodoc.service;
import com.hellodoc.dto.*; import com.hellodoc.entity.Absence; import com.hellodoc.repository.AbsenceRepository; import org.springframework.http.HttpStatus; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional; import org.springframework.web.server.ResponseStatusException; import java.util.*;
@Service public class AbsenceService {
 private final DoctorProfileService profiles; private final AbsenceRepository absences;
 public AbsenceService(DoctorProfileService profiles,AbsenceRepository absences){this.profiles=profiles;this.absences=absences;}
 @Transactional(readOnly=true) public List<AbsenceDto> list(String email){return absences.findByDoctorIdOrderByStartDateAsc(profiles.own(email).getId()).stream().map(AbsenceDto::from).toList();}
 @Transactional public AbsenceDto create(String email,AbsenceRequest r){return AbsenceDto.from(absences.save(new Absence(profiles.own(email),r.startDate(),r.endDate(),r.startTime(),r.endTime(),r.reason().trim(),clean(r.comment()))));}
 @Transactional public AbsenceDto update(String email,UUID id,AbsenceRequest r){var doctor=profiles.own(email);Absence a=absences.findByIdAndDoctorId(id,doctor.getId()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Absence introuvable."));a.update(r.startDate(),r.endDate(),r.startTime(),r.endTime(),r.reason().trim(),clean(r.comment()));return AbsenceDto.from(a);}
 @Transactional public void delete(String email,UUID id){var doctor=profiles.own(email);Absence a=absences.findByIdAndDoctorId(id,doctor.getId()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Absence introuvable."));absences.delete(a);}
 private String clean(String s){return s==null||s.isBlank()?null:s.trim();}
}
