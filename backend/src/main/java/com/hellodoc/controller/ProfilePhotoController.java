package com.hellodoc.controller;
import com.hellodoc.service.ProfilePhotoService;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;
@RestController @RequestMapping("/api")
public class ProfilePhotoController {
 private final ProfilePhotoService photos;
 public ProfilePhotoController(ProfilePhotoService photos){this.photos=photos;}
 @PostMapping(value="/account/me/photo",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @PreAuthorize("hasAnyRole('USER','PATIENT','DOCTOR','ADMIN')")
 public Map<String,String> replace(Authentication auth,@RequestPart("photo") MultipartFile photo){return Map.of("photoUrl",photos.replace(auth.getName(),photo));}
 @DeleteMapping("/account/me/photo") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasAnyRole('USER','PATIENT','DOCTOR','ADMIN')")
 public void remove(Authentication auth){photos.remove(auth.getName());}
 @GetMapping(value="/account/me/photo",produces=MediaType.IMAGE_JPEG_VALUE) @PreAuthorize("hasAnyRole('USER','PATIENT','DOCTOR','ADMIN')")
 public ResponseEntity<byte[]> own(Authentication auth){return image(photos.ownPhoto(auth.getName()));}
 @GetMapping(value="/doctors/{id}/photo",produces=MediaType.IMAGE_JPEG_VALUE) @PreAuthorize("permitAll()")
 public ResponseEntity<byte[]> doctor(@PathVariable UUID id){return image(photos.doctorPhoto(id));}
 private ResponseEntity<byte[]> image(byte[] jpeg){return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).cacheControl(CacheControl.noStore()).header("X-Content-Type-Options","nosniff").body(jpeg);}
}
