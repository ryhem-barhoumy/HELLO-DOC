package com.hellodoc.config;

import com.hellodoc.service.LegacyPatientProfileMigration;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
public class PatientProfileMigrationConfiguration {
    @Bean @Order(2)
    ApplicationRunner createLegacyPatientProfiles(LegacyPatientProfileMigration migration){
        return args->{int created=migration.createMissingProfiles();if(created>0)System.getLogger(getClass().getName()).log(System.Logger.Level.INFO,"Profils patients manquants restaurés pour "+created+" ancien(s) compte(s).");};
    }
}
