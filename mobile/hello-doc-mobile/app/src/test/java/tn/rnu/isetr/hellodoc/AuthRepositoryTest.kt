package tn.rnu.isetr.hellodoc

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.*
import org.junit.Assert.*
import tn.rnu.isetr.hellodoc.model.*
import tn.rnu.isetr.hellodoc.network.*
import tn.rnu.isetr.hellodoc.repository.*
import java.util.concurrent.TimeUnit

class AuthRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var cookies: SessionCookieJar
    private lateinit var repository: AuthRepository
    private val userJson = """{"id":"a8503747-4fd8-4371-8905-2d7161970675","firstName":"Amel","lastName":"Test","email":"amel@example.com","role":"USER","status":"ACTIVE"}"""
    @Before fun setup() {
        server = MockWebServer(); server.start()
        cookies = SessionCookieJar()
        repository = AuthRepository(RetrofitClient.createApi(server.url("/api/").toString(), cookies), cookies)
    }
    @After fun teardown() { server.shutdown() }
    private fun json(body: String, code: Int = 200) = MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body)
    private fun csrf() { server.enqueue(json("""{"headerName":"X-XSRF-TOKEN","token":"masked-value-not-for-spa-header"}""")
        .addHeader("Set-Cookie", "XSRF-TOKEN=raw-token; Path=/")) }
    private suspend fun failure(block: suspend () -> Unit): ApiFailure {
        try { block(); fail("Expected API failure") } catch (error: ApiFailure) { return error }
        throw AssertionError("No failure")
    }
    @Test fun loginUsesPasswordAndRawCsrfCookieThenSessionCookie() = runBlocking {
        csrf()
        server.enqueue(json(userJson).addHeader("Set-Cookie", "JSESSIONID=rotated-session; Path=/; HttpOnly"))
        server.enqueue(json(userJson))
        val user = repository.login("amel@example.com", "secret-password")
        assertEquals("Espace patient", user.spaceLabel)
        assertEquals(user, repository.me())
        assertEquals("/api/auth/csrf", server.takeRequest().path)
        val login = server.takeRequest()
        assertEquals("/api/auth/login", login.path)
        assertEquals("raw-token", login.getHeader("X-XSRF-TOKEN"))
        assertTrue(login.getHeader("Cookie")!!.contains("XSRF-TOKEN=raw-token"))
        assertNull(login.getHeader("Authorization"))
        assertEquals("""{"email":"amel@example.com","password":"secret-password"}""", login.body.readUtf8())
        assertTrue(server.takeRequest().getHeader("Cookie")!!.contains("JSESSIONID=rotated-session"))
    }
    @Test fun logoutInvalidatesServerSessionAndClearsCookies() = runBlocking {
        csrf(); server.enqueue(json(userJson).addHeader("Set-Cookie", "JSESSIONID=old; Path=/; HttpOnly"))
        repository.login("amel@example.com", "secret-password")
        csrf(); server.enqueue(MockResponse().setResponseCode(204))
        repository.logout()
        repeat(3) { server.takeRequest() }
        val logout = server.takeRequest()
        assertEquals("/api/auth/logout", logout.path)
        assertTrue(logout.getHeader("Cookie")!!.contains("JSESSIONID=old"))
        assertTrue(cookies.loadForRequest(server.url("/api/auth/me")).isEmpty())
    }
    @Test fun pendingDoctorCannotLoginAndGetsApprovalMessage() = runBlocking {
        csrf(); server.enqueue(json("""{"message":"Doctor registration is awaiting administrator approval."}""", 403))
        val error = failure { repository.login("doctor@example.com", "secret-password") }
        assertEquals(403, error.status)
        assertTrue(error.message.contains("en attente de validation"))
    }
    @Test fun patientRegistrationPreservesPhoneAndConsentAndReportsFields() = runBlocking {
        csrf(); server.enqueue(json("""{"message":"Certaines données sont invalides.","fieldErrors":{"phone":"Téléphone invalide"}}""", 400))
        val request = RegistrationRequest("Amel", "Test", "amel@example.com", "22123456", "Tunis", "password-123", true)
        val error = failure { repository.register(request, null, null) }
        server.takeRequest()
        val sent = server.takeRequest()
        assertEquals("/api/patients/registrations", sent.path)
        assertTrue(sent.body.readUtf8().contains("\"consent\":true"))
        assertEquals("Téléphone invalide", error.fields["phone"])
    }
    @Test fun doctorRegistrationIsMultipartAndContainsNoPracticeConfiguration() = runBlocking {
        csrf(); server.enqueue(json("""{"id":"a8503747-4fd8-4371-8905-2d7161970675","status":"PENDING"}""", 201))
        val request = RegistrationRequest("Amel", "Test", "amel@example.com", "22123456", "Tunis", "password-123", true, "Cardiologie", "ORDRE-12", "Tunis")
        assertEquals("PENDING", repository.register(request, "%PDF-1.4\n%%EOF".toByteArray(), "application/pdf").status)
        server.takeRequest()
        val sent = server.takeRequest()
        assertEquals("/api/doctors/registrations", sent.path)
        assertTrue(sent.getHeader("Content-Type")!!.startsWith("multipart/form-data"))
        val body = sent.body.readUtf8()
        assertTrue(body.contains("name=\"registration\"")); assertTrue(body.contains("name=\"document\""))
        assertTrue(body.contains("\"licenseNumber\":\"ORDRE-12\"")); assertTrue(body.contains("%PDF-1.4"))
        assertFalse(body.contains("workingHours")); assertFalse(body.contains("services"))
    }
    @Test fun missingCsrfCookieStopsMutation() = runBlocking {
        server.enqueue(json("""{"headerName":"X-XSRF-TOKEN","token":"masked"}"""))
        assertEquals(403, failure { repository.login("amel@example.com", "password") }.status)
        server.takeRequest()
        assertNull(server.takeRequest(100, TimeUnit.MILLISECONDS))
    }
    @Test fun expiredSessionIsNotTreatedAsLoggedIn() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(401))
        assertNull(repository.me())
    }
    @Test fun serverErrorCanBeRetriedWithoutInventingSuccess() = runBlocking {
        csrf(); server.enqueue(json("{}", 503))
        assertEquals(503, failure { repository.login("amel@example.com", "password") }.status)
        csrf(); server.enqueue(json(userJson))
        assertEquals("ACTIVE", repository.login("amel@example.com", "password").status)
    }
    @Test fun validationRejectsPhoneLettersEmailAndMissingDoctorIdentity() {
        val invalid = RegistrationRequest("Amel", "Test", "amel@domain", "22ABC456", "Tunis", "short", false, "", "", "")
        val errors = RegistrationValidation.errors(invalid, "different", true)
        assertTrue(errors.keys.containsAll(listOf("email", "phone", "password", "confirmation", "consent", "specialty", "licenseNumber", "city")))
        val valid = invalid.copy(email = "amel@example.com", phone = "22123456", password = "password-123", consent = true, specialty = null)
        assertTrue(RegistrationValidation.errors(valid, "password-123", false).isEmpty())
        assertEquals("Espace médecin", SessionUser("a8503747-4fd8-4371-8905-2d7161970675", "Amel", "Test", "amel@example.com", "DOCTOR", "ACTIVE").spaceLabel)
    }
}
