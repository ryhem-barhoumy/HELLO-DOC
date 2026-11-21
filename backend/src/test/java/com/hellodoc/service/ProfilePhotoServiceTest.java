package com.hellodoc.service;
import com.hellodoc.entity.*;
import com.hellodoc.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class ProfilePhotoServiceTest {
 UserRepository users=mock(UserRepository.class);DoctorRepository doctors=mock(DoctorRepository.class);ProfilePhotoRepository photos=mock(ProfilePhotoRepository.class);ProfilePhotoService service=new ProfilePhotoService(users,doctors,photos);
 MockMultipartFile image(int width,int height)throws Exception{var output=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB),"png",output);return new MockMultipartFile("photo","portrait.png","image/png",output.toByteArray());}
 @Test void acceptedImageIsReencodedAsJpegWithBoundedDimensions()throws Exception{byte[] bytes=service.normalize(image(1200,800));assertEquals(0xff,bytes[0]&0xff);assertEquals(0xd8,bytes[1]&0xff);var decoded=ImageIO.read(new ByteArrayInputStream(bytes));assertEquals(640,decoded.getWidth());assertEquals(427,decoded.getHeight());}
 @Test void declaredImageMustActuallyDecode(){var fake=new MockMultipartFile("photo","portrait.jpg","image/jpeg","<script>invalid</script>".getBytes());assertEquals(400,assertThrows(ResponseStatusException.class,()->service.normalize(fake)).getStatusCode().value());verifyNoInteractions(photos,users);}
 @Test void SvgIsNotAcceptedAsAProfilePhoto(){var file=new MockMultipartFile("photo","portrait.svg","image/svg+xml","<svg/>".getBytes());assertEquals(400,assertThrows(ResponseStatusException.class,()->service.normalize(file)).getStatusCode().value());}
 @Test void fileSizeIsCheckedBeforeDecoding(){var file=mock(org.springframework.web.multipart.MultipartFile.class);when(file.isEmpty()).thenReturn(false);when(file.getSize()).thenReturn(5*1024*1024+1L);assertEquals(413,assertThrows(ResponseStatusException.class,()->service.normalize(file)).getStatusCode().value());}
 @Test void photoReplacementUpdatesVersionWithoutModifyingTheProfile()throws Exception{var user=new UserAccount("Patient","Test","patient@test.local","hash",UserRole.USER,"ACTIVE");UUID id=UUID.randomUUID();org.springframework.test.util.ReflectionTestUtils.setField(user,"id",id);when(users.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));when(photos.findById(id)).thenReturn(Optional.empty());String url=service.replace(user.getEmail(),image(20,20));assertTrue(url.startsWith("/api/account/me/photo?v="));String firstVersion=user.getPhotoVersion();service.replace(user.getEmail(),image(20,20));assertNotEquals(firstVersion,user.getPhotoVersion());assertEquals("hash",user.getPasswordHash());verify(photos,times(2)).save(any(ProfilePhoto.class));}
 @Test void removalClearsOnlyTheAuthenticatedUsersPhoto(){var user=new UserAccount("Patient","Test","patient@test.local","hash",UserRole.USER,"ACTIVE");UUID id=UUID.randomUUID();org.springframework.test.util.ReflectionTestUtils.setField(user,"id",id);user.setPhotoVersion("old");var photo=new ProfilePhoto(user);when(users.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));when(photos.findById(id)).thenReturn(Optional.of(photo));service.remove(user.getEmail());assertNull(user.ownPhotoUrl());verify(photos).delete(photo);}
 @Test void inactiveDoctorPhotoCannotBeReadPublicly(){UUID id=UUID.randomUUID();var doctor=mock(Doctor.class);when(doctor.getUser()).thenReturn(new UserAccount("Doctor","Test","doctor@test.local","hash",UserRole.DOCTOR,"INACTIVE"));when(doctors.findByIdAndActiveTrue(id)).thenReturn(Optional.of(doctor));assertEquals(404,assertThrows(ResponseStatusException.class,()->service.doctorPhoto(id)).getStatusCode().value());verifyNoInteractions(photos);}
}
