package com.hellodoc.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record DoctorRegistrationRequest(@NotBlank @Size(max=100) String firstName, @NotBlank @Size(max=100) String lastName,
 @NotBlank @Email @Size(max=254) String email, @NotBlank @Size(max=32) String phone, @NotBlank @Size(max=300) String address,
 @NotBlank @Size(min=8,max=72) String password, @AssertTrue boolean consent,
 @NotBlank String specialty, @NotBlank String licenseNumber, @NotBlank String serviceTitle,
 @Min(5) @Max(240) int durationMinutes, @NotBlank String workStart, @NotBlank String workEnd,
 String secondWorkStart, String secondWorkEnd,
 @NotEmpty List<String> workingDays, @Min(0) int pauseMinutes, @NotBlank String location,
 @NotBlank String city, @NotBlank @Size(max=2000) String description, @NotEmpty List<@Valid ServiceInput> services) {
 public DoctorRegistrationRequest(String firstName,String lastName,String email,String phone,String address,String password,boolean consent,String specialty,String licenseNumber,String serviceTitle,int durationMinutes,String workStart,String workEnd,List<String> workingDays,int pauseMinutes,String location,String city,String description,List<@Valid ServiceInput> services){this(firstName,lastName,email,phone,address,password,consent,specialty,licenseNumber,serviceTitle,durationMinutes,workStart,workEnd,null,null,workingDays,pauseMinutes,location,city,description,services);}
 public record ServiceInput(@NotBlank String name,@NotBlank String price) { }
}
