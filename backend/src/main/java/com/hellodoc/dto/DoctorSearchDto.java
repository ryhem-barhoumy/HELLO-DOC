package com.hellodoc.dto;

import com.hellodoc.entity.Doctor;

import java.time.Instant;
import java.util.UUID;

public record DoctorSearchDto(
        UUID id, String firstName, String lastName, String fullName, String serviceTitle,
        String specialty, String city, String location, String address, String status,
        Instant nextAvailability, String photoUrl, boolean videoConsultationEnabled
) {
    public static DoctorSearchDto from(Doctor doctor, Instant nextAvailability) {
        var user = doctor.getUser();
        var name = user.getFirstName() + " " + user.getLastName();
        var specialty = doctor.getSpecialties().stream().map(item -> item.getName()).sorted().findFirst().orElse("");
        var photoUrl = user.getPhotoVersion() == null ? null : "/api/doctors/" + doctor.getId() + "/photo?v=" + user.getPhotoVersion();
        return new DoctorSearchDto(doctor.getId(), user.getFirstName(), user.getLastName(), name,
                doctor.getServiceTitle(), specialty, doctor.getCity(), doctor.getLocation(), doctor.getAddress(),
                "ACTIVE", nextAvailability, photoUrl, doctor.isVideoConsultationEnabled());
    }

    public static DoctorSearchDto from(Doctor doctor) { return from(doctor, null); }

    public DoctorSearchDto withoutPrivateContact() {
        return new DoctorSearchDto(id, firstName, lastName, fullName, serviceTitle, specialty, city,
                DoctorPublicDto.publicPracticeLocation(location, address), null, status,
                nextAvailability, photoUrl, videoConsultationEnabled);
    }
}
