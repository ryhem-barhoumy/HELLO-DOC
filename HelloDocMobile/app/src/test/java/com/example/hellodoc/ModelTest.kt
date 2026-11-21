package com.example.hellodoc

import org.junit.Assert.*
import org.junit.Test
import com.example.hellodoc.model.SessionUser
import com.example.hellodoc.model.Doctor

class ModelTest {
    @Test fun patientCanUseMobile() {
        assertTrue(SessionUser("1", "Ali", "Ben Salah", "ali@example.com", "PATIENT", "ACTIVE", null).isPatient)
        assertTrue(SessionUser("2", "Amel", "Trabelsi", "amel@example.com", "USER", "ACTIVE", null).isPatient)
    }
    @Test fun doctorAndAdminRejected() {
        assertFalse(SessionUser("3", "Doctor", "A", "a@example.com", "DOCTOR", "ACTIVE", null).isPatient)
        assertFalse(SessionUser("4", "Admin", "B", "b@example.com", "ADMIN", "ACTIVE", null).isPatient)
    }
    @Test fun blockedPatientRejected() {
        assertFalse(SessionUser("5", "Ali", "B", "b@example.com", "PATIENT", "BLOCKED", null).isPatient)
    }
    @Test fun doctorNameFallback() {
        val d = Doctor("id", "Amel", "Haddad", null, null, "Cardiologie", "Tunis", null, null, "ACTIVE", null, null)
        assertEquals("Amel Haddad", d.displayName())
    }
}
