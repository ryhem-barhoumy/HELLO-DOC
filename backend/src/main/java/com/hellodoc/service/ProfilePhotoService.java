package com.hellodoc.service;
import com.hellodoc.entity.*;
import com.hellodoc.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;

@Service
public class ProfilePhotoService {
 private final UserRepository users;private final DoctorRepository doctors;private final ProfilePhotoRepository photos;
 public ProfilePhotoService(UserRepository users,DoctorRepository doctors,ProfilePhotoRepository photos){this.users=users;this.doctors=doctors;this.photos=photos;}
 @Transactional public String replace(String email,MultipartFile file){byte[] jpeg=normalize(file);UserAccount user=own(email);ProfilePhoto photo=photos.findById(user.getId()).orElseGet(()->new ProfilePhoto(user));photo.replace(jpeg);photos.save(photo);user.setPhotoVersion(UUID.randomUUID().toString());return user.ownPhotoUrl();}
 @Transactional public void remove(String email){UserAccount user=own(email);photos.findById(user.getId()).ifPresent(photos::delete);user.setPhotoVersion(null);}
 @Transactional(readOnly=true) public byte[] ownPhoto(String email){return bytes(own(email));}
 @Transactional(readOnly=true) public byte[] doctorPhoto(UUID doctorId){Doctor doctor=doctors.findByIdAndActiveTrue(doctorId).filter(d->d.getUser().getRole()==UserRole.DOCTOR&&"ACTIVE".equals(d.getUser().getStatus())).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Photo indisponible."));return bytes(doctor.getUser());}
 private byte[] bytes(UserAccount user){return photos.findById(user.getId()).map(ProfilePhoto::getJpeg).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Aucune photo de profil."));}
 private UserAccount own(String email){return users.findByEmailIgnoreCase(email).filter(u->"ACTIVE".equals(u.getStatus())).orElseThrow(()->new ResponseStatusException(HttpStatus.FORBIDDEN,"Un compte actif est requis."));}
 byte[] normalize(MultipartFile file){
  if(file==null||file.isEmpty())throw bad("Choisissez une photo JPEG ou PNG.");
  if(file.getSize()>5*1024*1024)throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,"La photo ne doit pas dépasser 5 Mo.");
  if(!Set.of("image/jpeg","image/png").contains(Objects.toString(file.getContentType(),"")))throw bad("Formats acceptés : JPEG et PNG.");
  try(var input=ImageIO.createImageInputStream(new ByteArrayInputStream(file.getBytes()))){
   if(input==null)throw bad("Le fichier ne contient pas une image lisible.");
   var readers=ImageIO.getImageReaders(input);if(!readers.hasNext())throw bad("Le fichier ne contient pas une image lisible.");
   var reader=readers.next();try{
    if(!Set.of("JPEG","PNG").contains(reader.getFormatName().toUpperCase(Locale.ROOT)))throw bad("Formats acceptés : JPEG et PNG.");
    reader.setInput(input,true,true);int w=reader.getWidth(0),h=reader.getHeight(0);
    if(w<1||h<1||(long)w*h>20_000_000L)throw bad("Choisissez une photo de 20 mégapixels maximum.");
    BufferedImage source=reader.read(0);double scale=Math.min(1,640d/Math.max(w,h));
    BufferedImage normalized=new BufferedImage(Math.max(1,(int)Math.round(w*scale)),Math.max(1,(int)Math.round(h*scale)),BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics=normalized.createGraphics();try{graphics.setColor(Color.WHITE);graphics.fillRect(0,0,normalized.getWidth(),normalized.getHeight());graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);graphics.drawImage(source,0,0,normalized.getWidth(),normalized.getHeight(),null);}finally{graphics.dispose();}
    var output=new ByteArrayOutputStream();ImageIO.write(normalized,"jpeg",output);return output.toByteArray();
   }finally{reader.dispose();}
  }catch(IOException|IllegalArgumentException e){throw bad("La photo ne peut pas être lue. Choisissez un autre fichier JPEG ou PNG.");}
 }
 private ResponseStatusException bad(String message){return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
}
