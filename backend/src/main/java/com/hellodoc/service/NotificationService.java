package com.hellodoc.service;

import com.hellodoc.dto.*;
import com.hellodoc.entity.*;
import com.hellodoc.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;

@Service
public class NotificationService {
 private final NotificationRepository notifications;private final UserRepository users;private final DoctorProfileService doctorProfiles;
 public NotificationService(NotificationRepository notifications,UserRepository users,DoctorProfileService doctorProfiles){this.notifications=notifications;this.users=users;this.doctorProfiles=doctorProfiles;}
 @Transactional(readOnly=true) public PagedResponse<NotificationDto> list(String email,int page,int size){activeRecipient(email);var found=notifications.findMine(email,PageRequest.of(Math.max(0,page),Math.max(1,Math.min(size,100)),Sort.by("createdAt").descending()));return PagedResponse.from(found.map(NotificationDto::from));}
 @Transactional public NotificationDto markRead(String email,UUID id){activeRecipient(email);Notification notification=notifications.findOwned(id,email).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Notification introuvable."));notification.markRead();return NotificationDto.from(notification);}
 @Transactional(readOnly=true) public long unreadCount(String email){UserAccount user=activeRecipient(email);return notifications.countUnread(user.getId());}
 @Transactional public void create(UserAccount recipient,Appointment appointment,String type,String title,String message){notifications.save(new Notification(recipient,appointment,type,title,message));}
 private UserAccount activeRecipient(String email){UserAccount user=users.findByEmailIgnoreCase(email).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Authentification requise."));if(!"ACTIVE".equals(user.getStatus()))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Un compte actif est requis.");if(user.getRole()==UserRole.DOCTOR)doctorProfiles.own(email);else if(user.getRole()!=UserRole.USER&&user.getRole()!=UserRole.PATIENT)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Notifications non accessibles pour ce compte.");return user;}
}
