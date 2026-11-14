package com.hellodoc.service;

import com.hellodoc.dto.*;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RegistrationValidationTest {
    @Test void physicianCanRegisterIdentityWithoutServicesOrSchedule() {
        try(var factory=Validation.buildDefaultValidatorFactory()) {
            var body=new DoctorRegistrationRequest("Amel","Doctor","amel@example.com","22123456","12 rue du Cabinet","password-123",true,"Cardiologie","ORD-123","Tunis");
            assertTrue(factory.getValidator().validate(body).isEmpty());
        }
    }
    @Test void signupRejectsAlphabeticPhoneAndIncompleteEmailOnServer() {
        try(var factory=Validation.buildDefaultValidatorFactory()) {
            var body=new RegistrationRequest("Patient","Test","patient@domain","22AB3456","Tunis","password-123",true);
            var violations=factory.getValidator().validate(body);
            assertTrue(violations.stream().anyMatch(v->v.getPropertyPath().toString().equals("phone")));
            assertTrue(violations.stream().anyMatch(v->v.getPropertyPath().toString().equals("email")));
        }
    }
}
