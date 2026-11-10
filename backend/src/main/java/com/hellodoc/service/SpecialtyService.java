package com.hellodoc.service;

import com.hellodoc.dto.SpecialtyDto;
import com.hellodoc.entity.Specialty;
import com.hellodoc.repository.DoctorRepository;
import com.hellodoc.repository.SpecialtyRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class SpecialtyService {
    private final SpecialtyRepository specialties;
    private final DoctorRepository doctors;

    public SpecialtyService(SpecialtyRepository specialties, DoctorRepository doctors) {
        this.specialties = specialties;
        this.doctors = doctors;
    }

    @Transactional(readOnly = true)
    public List<SpecialtyDto> list() {
        return specialties.findAllByOrderByNameAsc().stream().map(SpecialtyDto::from).toList();
    }

    @Transactional
    public Specialty create(String name) {
        String clean = cleanName(name);
        ensureNameAvailable(clean, null);
        return specialties.save(new Specialty(clean));
    }

    @Transactional
    public Specialty update(UUID id, String name) {
        Specialty specialty = specialties.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Spécialité introuvable."));
        String clean = cleanName(name);
        ensureNameAvailable(clean, specialty.getId());
        specialty.rename(clean);
        return specialties.save(specialty);
    }

    @Transactional
    public void delete(UUID id) {
        Specialty specialty = specialties.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Spécialité introuvable."));
        long doctorCount = doctors.countDoctorsBySpecialtyId(id);
        if (doctorCount > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Suppression impossible : cette spécialité est associée à " + doctorCount
                            + (doctorCount == 1 ? " médecin. Retirez ou réaffectez d’abord cette spécialité."
                            : " médecins. Retirez ou réaffectez d’abord cette spécialité."));
        }
        specialties.delete(specialty);
    }

    @Transactional
    public void renameLegacy(String oldName, String fullName) {
        String oldKey = Specialty.normalize(oldName);
        String newKey = Specialty.normalize(fullName);
        var legacy = specialties.findByNormalizedName(oldKey);
        if (legacy.isPresent() && specialties.findByNormalizedName(newKey).isEmpty()) {
            legacy.get().rename(fullName);
            specialties.save(legacy.get());
        }
    }

    @Transactional
    public Specialty findOrCreate(String name) {
        String clean = cleanName(name);
        String key = Specialty.normalize(clean);
        return specialties.findByNormalizedName(key).orElseGet(() -> specialties.save(new Specialty(clean)));
    }

    private void ensureNameAvailable(String name, UUID ignoredId) {
        specialties.findByNormalizedName(Specialty.normalize(name)).ifPresent(existing -> {
            if (ignoredId == null || !ignoredId.equals(existing.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Une spécialité portant ce nom existe déjà.");
            }
        });
    }

    private String cleanName(String name) {
        String clean = name == null ? "" : name.trim().replaceAll("\\s+", " ");
        if (clean.isBlank()) throw new IllegalArgumentException("Le nom de la spécialité est obligatoire.");
        if (clean.length() > 120) throw new IllegalArgumentException("Le nom ne peut pas dépasser 120 caractères.");
        return clean;
    }
}
