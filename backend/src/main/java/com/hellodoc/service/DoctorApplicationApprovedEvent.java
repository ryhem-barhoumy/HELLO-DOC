package com.hellodoc.service;

public record DoctorApplicationApprovedEvent(java.util.UUID applicationId,String email,String firstName,String lastName) { }
