package com.hellodoc.controller;

import com.hellodoc.service.DoctorDocumentService;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/doctors")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDoctorDocumentController {
    private final DoctorDocumentService documents;
    public AdminDoctorDocumentController(DoctorDocumentService documents){this.documents=documents;}
    @GetMapping("/{id}/document")
    public ResponseEntity<byte[]> download(@PathVariable UUID id){
        var document=documents.download(id);
        ContentDisposition disposition=ContentDisposition.inline().filename(document.originalName(),StandardCharsets.UTF_8).build();
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(document.contentType())).contentLength(document.sizeBytes()).header(HttpHeaders.CONTENT_DISPOSITION,disposition.toString()).header(HttpHeaders.CACHE_CONTROL,"no-store").header("X-Content-Type-Options","nosniff").body(document.content());
    }
}
