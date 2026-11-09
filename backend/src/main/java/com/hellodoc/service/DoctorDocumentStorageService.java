package com.hellodoc.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

@Service
public class DoctorDocumentStorageService {
    public static final long MAX_BYTES = 5L * 1024L * 1024L;
    private final Path storageDirectory;

    public DoctorDocumentStorageService(@Value("${HELLODOC_DOCUMENTS_DIR:${user.home}/.hellodoc/doctor-documents}") String directory) {
        this.storageDirectory=Path.of(directory).toAbsolutePath().normalize();
    }

    public StoredDocument store(MultipartFile file) {
        if(file==null||file.isEmpty())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"L’attestation d’inscription à l’Ordre est obligatoire.");
        if(file.getSize()>MAX_BYTES)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Le justificatif dépasse la taille maximale de 5 Mo.");
        final byte[] bytes;
        try(InputStream input=file.getInputStream()){bytes=input.readNBytes((int)MAX_BYTES+1);}
        catch(IOException e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Le justificatif n’a pas pu être lu.");}
        if(bytes.length==0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Le justificatif est vide.");
        if(bytes.length>MAX_BYTES)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Le justificatif dépasse la taille maximale de 5 Mo.");
        FileType type=detect(bytes);
        if(type==null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Format refusé. Téléversez un fichier PDF, JPG ou PNG valide.");
        String key=UUID.randomUUID()+type.extension;
        Path destination=storageDirectory.resolve(key).normalize();
        if(!destination.startsWith(storageDirectory))throw new IllegalStateException("Invalid generated document path.");
        try{Files.createDirectories(storageDirectory);Files.write(destination,bytes,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE);}
        catch(IOException e){throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"Le justificatif n’a pas pu être stocké.");}
        return new StoredDocument(key,originalName(file.getOriginalFilename(),type.extension),type.mediaType,bytes.length);
    }

    public byte[] read(String storageKey) {
        if(storageKey==null||!storageKey.matches("[0-9a-fA-F-]{36}\\.(pdf|jpg|png)"))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Document introuvable.");
        Path path=storageDirectory.resolve(storageKey).normalize();
        if(!path.startsWith(storageDirectory)||!Files.isRegularFile(path))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Document introuvable.");
        try{return Files.readAllBytes(path);}catch(IOException e){throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"Le justificatif n’a pas pu être lu.");}
    }

    public void delete(String storageKey){if(storageKey==null||!storageKey.matches("[0-9a-fA-F-]{36}\\.(pdf|jpg|png)"))return;try{Files.deleteIfExists(storageDirectory.resolve(storageKey).normalize());}catch(IOException ignored){}}
    private FileType detect(byte[] b){
        for(int i=0;i<=Math.min(b.length-5,1024);i++)if(b[i]=='%'&&b[i+1]=='P'&&b[i+2]=='D'&&b[i+3]=='F'&&b[i+4]=='-')return new FileType(".pdf","application/pdf");
        if(b.length>=3&&(b[0]&255)==0xff&&(b[1]&255)==0xd8&&(b[2]&255)==0xff)return new FileType(".jpg","image/jpeg");
        if(b.length>=8&&(b[0]&255)==0x89&&b[1]=='P'&&b[2]=='N'&&b[3]=='G'&&(b[4]&255)==0x0d&&(b[5]&255)==0x0a&&(b[6]&255)==0x1a&&(b[7]&255)==0x0a)return new FileType(".png","image/png");
        return null;
    }
    private String originalName(String name,String extension){
        String clean=name==null||name.isBlank()?"attestation"+extension:name.replace('\\','/');
        int slash=clean.lastIndexOf('/');if(slash>=0)clean=clean.substring(slash+1);
        clean=clean.replaceAll("[\\p{Cntrl}]","_").trim();if(clean.isBlank())clean="attestation"+extension;
        return clean.length()>255?clean.substring(0,255):clean;
    }
    private record FileType(String extension,String mediaType){}
    public record StoredDocument(String storageKey,String originalName,String mediaType,long sizeBytes){}
}
