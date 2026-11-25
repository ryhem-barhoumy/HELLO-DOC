package com.hellodoc.dto;

import com.hellodoc.entity.Prescription;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PrescriptionDto(UUID id, UUID appointmentId, UUID supersedesId, Instant prescribedAt, String doctorName,
                              String recommendations, boolean amended, List<Item> items,String status) {
    public record Item(String medication, String instructions) {}
    public static PrescriptionDto from(Prescription p) {
        var appointment=p.getAppointment(); var doctor=appointment.getDoctor().getUser();
        return new PrescriptionDto(p.getId(), appointment.getId(), p.getSupersedes()==null?null:p.getSupersedes().getId(), p.getPrescribedAt(), doctor.getFirstName()+" "+doctor.getLastName(),
            p.getRecommendations(), p.isAmended(), p.getItems().stream().map(i->new Item(i.getMedication(),i.getInstructions())).toList(),p.getStatus().name());
    }
}
