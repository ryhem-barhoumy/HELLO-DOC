package com.hellodoc.dto;
public record PatientDashboardDto(long upcomingCount,long pastCount,long unreadNotificationCount,AppointmentDto upcomingAppointment) { }
