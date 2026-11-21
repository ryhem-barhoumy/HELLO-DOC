package tn.rnu.isetr.hellodoc

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import tn.rnu.isetr.hellodoc.model.*
import tn.rnu.isetr.hellodoc.network.*
import tn.rnu.isetr.hellodoc.repository.*
import java.util.UUID

/** Run only against an explicitly selected disposable QA server, never production. */
class LiveAuthIntegrationTest {
    @Test fun authenticationAgainstCurrentSpringBackend() = runBlocking {
        val url = System.getenv("HELLODOC_TEST_API_URL")
        assumeTrue("Set HELLODOC_TEST_API_URL to the disposable fixture API", !url.isNullOrBlank())
        val cookies = SessionCookieJar()
        val repository = AuthRepository(RetrofitClient.createApi(url!!, cookies), cookies)
        val suffix = UUID.randomUUID().toString().take(8)
        val patient = RegistrationRequest("Mobile", "Validation", "mobile-$suffix@qa.example", "22123456", "Adresse QA", "OnlyForHelloDocQA2026", true)
        assertEquals("ACTIVE", repository.register(patient, null, null).status)
        assertEquals("USER", repository.login(patient.email, patient.password).role)
        assertEquals(patient.email, repository.me()!!.email)
        repository.logout()
        assertNull(repository.me())
        val specialty = repository.specialties().first().name
        val doctor = patient.copy(email = "doctor-mobile-$suffix@qa.example", specialty = specialty, licenseNumber = "QA-$suffix", city = "Tunis")
        assertEquals("PENDING", repository.register(doctor, "%PDF-1.4\n%QA only\n%%EOF".toByteArray(), "application/pdf").status)
        try { repository.login(doctor.email, doctor.password); fail("Pending physician must be blocked") }
        catch (error: ApiFailure) { assertEquals(403, error.status); assertTrue(error.message.contains("en attente")) }
        val doctorSession = repository.login("doctor@qa.example", "OnlyForHelloDocQA2026")
        assertEquals("DOCTOR", doctorSession.role)
        assertEquals("DOCTOR", repository.me()!!.role)
        repository.logout()
        try { repository.login(patient.email, "incorrect-password"); fail("Wrong password must fail") }
        catch (error: ApiFailure) { assertEquals(401, error.status) }
    }
}
