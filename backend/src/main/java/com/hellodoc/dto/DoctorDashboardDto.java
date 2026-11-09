package com.hellodoc.dto;
import java.util.List;
public record DoctorDashboardDto(long pendingCount,long confirmedCount,long completedCount,long todayCount,long unreadNotificationCount,List<AppointmentDto> nextAppointments) { }
