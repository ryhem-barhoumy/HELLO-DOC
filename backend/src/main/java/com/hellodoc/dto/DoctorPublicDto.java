package com.hellodoc.dto;

import com.hellodoc.entity.Doctor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DoctorPublicDto(
        UUID id, String firstName, String lastName, String fullName, String serviceTitle,
        String specialty, List<String> specialties, String city, String location,
        String address, String phone, String description, int durationMinutes,
        List<Integer> workingDays, List<MedicalServiceDto> services, Instant nextAvailability,
        String photoUrl, boolean videoConsultationEnabled
) {
    public static DoctorPublicDto from(Doctor doctor, List<Integer> days, List<MedicalServiceDto> services,
                                       Instant nextAvailability) {
        var user = doctor.getUser();
        var names = doctor.getSpecialties().stream().map(specialty -> specialty.getName()).sorted().toList();
        var photoUrl = user.getPhotoVersion() == null ? null : "/api/doctors/" + doctor.getId() + "/photo?v=" + user.getPhotoVersion();
        return new DoctorPublicDto(doctor.getId(), user.getFirstName(), user.getLastName(),
                user.getFirstName() + " " + user.getLastName(), doctor.getServiceTitle(),
                names.isEmpty() ? null : names.getFirst(), names, doctor.getCity(), doctor.getLocation(),
                doctor.getAddress(), user.getPhone(), doctor.getDescription(), doctor.getDurationMinutes(),
                days, services, nextAvailability, photoUrl, doctor.isVideoConsultationEnabled());
    }

    public DoctorPublicDto withoutPrivateContact() {
        return new DoctorPublicDto(id, firstName, lastName, fullName, serviceTitle, specialty, specialties,
                city, publicPracticeLocation(location, address), null, null, description, durationMinutes, workingDays, services,
                nextAvailability, photoUrl, videoConsultationEnabled);
    }

    static String publicPracticeLocation(String location, String accountAddress) {
        if (location == null || accountAddress == null) return location;
        // Approval can copy the account address into the location before a doctor configures the practice.
        return location.strip().equalsIgnoreCase(accountAddress.strip()) ? null : location;
    }
}
