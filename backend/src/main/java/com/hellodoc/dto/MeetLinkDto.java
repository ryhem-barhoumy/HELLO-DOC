package com.hellodoc.dto;

import java.util.UUID;

public record MeetLinkDto(UUID appointmentId, String url, java.time.Instant externalCallEndedAt, String externalCallEndedBy) {
    public MeetLinkDto(UUID appointmentId,String url){this(appointmentId,url,null,null);}
}
