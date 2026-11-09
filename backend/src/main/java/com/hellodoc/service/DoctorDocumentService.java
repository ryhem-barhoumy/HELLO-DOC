package com.hellodoc.service;

import com.hellodoc.entity.DoctorApplication;
import com.hellodoc.repository.DoctorApplicationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class DoctorDocumentService {
    private final DoctorApplicationRepository applications;
    private final DoctorDocumentStorageService storage;
    public DoctorDocumentService(DoctorApplicationRepository applications,DoctorDocumentStorageService storage){this.applications=applications;this.storage=storage;}

    @Transactional(readOnly=true)
    public DownloadedDocument download(UUID applicationId){
        DoctorApplication application=applications.findById(applicationId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Candidature introuvable."));
        if(!application.hasDoctorDocument())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Aucun justificatif n’est associé à cette candidature.");
        return new DownloadedDocument(storage.read(application.getDocumentStorageKey()),application.getDocumentOriginalName(),application.getDocumentContentType(),application.getDocumentSizeBytes());
    }
    public record DownloadedDocument(byte[] content,String originalName,String contentType,long sizeBytes){}
}
