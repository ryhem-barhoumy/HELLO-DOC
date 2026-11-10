package com.hellodoc.service;

import com.hellodoc.entity.Specialty;
import com.hellodoc.repository.DoctorRepository;
import com.hellodoc.repository.SpecialtyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpecialtyManagementServiceTest {
 @Mock SpecialtyRepository specialties; @Mock DoctorRepository doctors; @InjectMocks SpecialtyService service;
 @Test void createTrimsNameAndRejectsDuplicateIgnoringAccentsAndCase(){when(specialties.findByNormalizedName("cardiologie")).thenReturn(Optional.empty());when(specialties.save(any(Specialty.class))).thenAnswer(i->i.getArgument(0));assertEquals("Cardiologie",service.create(" Cardiologie ").getName());when(specialties.findByNormalizedName("cardiologie")).thenReturn(Optional.of(new Specialty("Cardiologie")));ResponseStatusException ex=assertThrows(ResponseStatusException.class,()->service.create(" CARDIOLOGIE "));assertEquals(HttpStatus.CONFLICT,ex.getStatusCode());}
 @Test void updateRenamesAndRejectsDuplicate(){UUID id=UUID.randomUUID();Specialty current=mock(Specialty.class);when(current.getId()).thenReturn(id);when(specialties.findById(id)).thenReturn(Optional.of(current));when(specialties.findByNormalizedName("dermatologie")).thenReturn(Optional.empty());when(specialties.save(current)).thenReturn(current);assertSame(current,service.update(id," Dermatologie "));verify(current).rename("Dermatologie");when(specialties.findByNormalizedName("neurologie")).thenReturn(Optional.of(new Specialty("Neurologie")));ResponseStatusException ex=assertThrows(ResponseStatusException.class,()->service.update(id,"Neurologie"));assertEquals(HttpStatus.CONFLICT,ex.getStatusCode());}
 @Test void deleteBlocksUsedSpecialtyAndDeletesUnusedOne(){UUID id=UUID.randomUUID();Specialty specialty=new Specialty("Orthopédie");when(specialties.findById(id)).thenReturn(Optional.of(specialty));when(doctors.countDoctorsBySpecialtyId(id)).thenReturn(2L,0L);ResponseStatusException ex=assertThrows(ResponseStatusException.class,()->service.delete(id));assertEquals(HttpStatus.CONFLICT,ex.getStatusCode());assertTrue(ex.getReason().contains("2 médecins"));verify(specialties,never()).delete(specialty);service.delete(id);verify(specialties).delete(specialty);}
 @Test void missingSpecialtyReturnsNotFound(){UUID id=UUID.randomUUID();when(specialties.findById(id)).thenReturn(Optional.empty());ResponseStatusException ex=assertThrows(ResponseStatusException.class,()->service.update(id,"Neurologie"));assertEquals(HttpStatus.NOT_FOUND,ex.getStatusCode());}
}
