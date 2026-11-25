package com.hellodoc.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.PageRequest;
import jakarta.persistence.EntityManager;
import com.hellodoc.entity.*;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** Starts Spring Data JPA against an isolated embedded database and validates repository queries. */
@DataJpaTest
class Sprint4RepositoryContextTest {
    @Autowired ApplicationContext context;
    @Autowired EntityManager entityManager;
    @Autowired AppointmentRepository appointmentRepository;
    @Autowired ConsultationRevisionRepository revisionRepository;

    @Test
    void sprint4RepositoriesAndTheirQueriesInitialize() {
        assertNotNull(context.getBean(PrescriptionRepository.class));
        assertNotNull(context.getBean(TeleconsultationMessageRepository.class));
        assertNotNull(context.getBean(TeleconsultationSessionRepository.class));
    }

    @Test
    void appointmentModePersistsAndLegacyNullReadsAsInPerson() {
        var doctorUser=new UserAccount("Doc","Tor","doctor-mode@test.invalid","hash",UserRole.DOCTOR,"ACTIVE");
        var patientUser=new UserAccount("Pat","Ient","patient-mode@test.invalid","hash",UserRole.USER,"ACTIVE");
        entityManager.persist(doctorUser);entityManager.persist(patientUser);
        var application=new DoctorApplication(doctorUser,"Consultation","Généraliste","TEST-123","Tunis","Cabinet test",30,"09:00","17:00",5,"[1,2,3,4,5]","[]","Description test");
        entityManager.persist(application);
        var doctor=new Doctor(doctorUser,application,"Consultation","Tunis","Adresse test","Cabinet test","Description test",30,5,"[]");
        var patient=new Patient(patientUser);entityManager.persist(doctor);entityManager.persist(patient);
        var appointment=new Appointment(doctor,patient,"Consultation","50",Instant.now().plusSeconds(3600),Instant.now().plusSeconds(5400),null,ConsultationMode.VIDEO);
        appointment.setMeetLink("https://meet.google.com/abc-defg-hij");entityManager.persist(appointment);
        var note=new ConsultationNote(appointment);note.update("Résumé clinique","Observation","Suivi",null,"dans deux semaines","Apporter les résultats","Note privée");entityManager.persist(note);
        entityManager.flush();var revision=new ConsultationRevision(note,"Doc Tor","CORRECTED","Précision clinique",note.getVersion(),"Résumé antérieur","Observations antérieures","Traitement antérieur",null,"un mois","Anciennes instructions","Note privée antérieure");entityManager.persist(revision);
        entityManager.flush();UUID id=appointment.getId(),noteId=note.getId();entityManager.clear();
        assertEquals(ConsultationMode.VIDEO,entityManager.find(Appointment.class,id).getConsultationMode());
        assertEquals("https://meet.google.com/abc-defg-hij",entityManager.find(Appointment.class,id).getMeetLink());
        var persistedNote=entityManager.find(ConsultationNote.class,noteId);assertEquals("dans deux semaines",persistedNote.getRecommendedDelay());assertEquals("Apporter les résultats",persistedNote.getFollowUpInstructions());
        var persistedRevision=revisionRepository.findTop50ByNoteIdOrderByChangedAtDesc(noteId).getFirst();assertEquals("Résumé antérieur",persistedRevision.getPreviousResume());assertEquals("Précision clinique",persistedRevision.getCorrectionReason());assertEquals("Note privée antérieure",persistedRevision.getPreviousPrivateNote());
        entityManager.createNativeQuery("update appointments set consultation_mode = null where id = :id").setParameter("id",id).executeUpdate();entityManager.clear();
        var legacy=entityManager.find(Appointment.class,id);assertEquals(ConsultationMode.IN_PERSON,legacy.getConsultationMode());legacy.changeStatus(AppointmentStatus.COMPLETED,null);entityManager.flush();
        var patients=appointmentRepository.findCompletedPatients(legacy.getDoctor().getId(),"%",PageRequest.of(0,20));assertEquals(1,patients.getTotalElements());assertEquals(patient.getId(),patients.getContent().get(0).id());
    }
}
