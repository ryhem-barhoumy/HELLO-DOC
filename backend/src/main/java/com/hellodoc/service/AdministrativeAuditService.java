package com.hellodoc.service;
import com.hellodoc.entity.AdministrativeActivity;
import com.hellodoc.repository.AdministrativeActivityRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.util.UUID;
@Service
public class AdministrativeAuditService {
    private final AdministrativeActivityRepository activities;
    public AdministrativeAuditService(AdministrativeActivityRepository activities){this.activities=activities;}
    @Transactional(propagation=Propagation.MANDATORY)
    public void record(String action,String type,UUID id){var auth=SecurityContextHolder.getContext().getAuthentication();activities.save(new AdministrativeActivity(auth==null?"Système":auth.getName(),action,type,id));}
}
