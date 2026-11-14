"""Exercise real HTTP/session/MySQL workflows in a disposable schema.

Requires the packaged jar and local Laragon root access without a password.
No existing application data is read or modified. --hold keeps the fixture server
available for browser QA until Enter is received. Test credentials are deliberately
local-only and are never used for the real hello_doc database.
"""
import concurrent.futures
import http.cookiejar
import json
import os
from pathlib import Path
import subprocess
import sys
import time
import urllib.error
import urllib.request
import uuid
from datetime import datetime, timedelta, timezone

ROOT = Path(__file__).resolve().parent
MYSQL = Path(r"C:\laragon\bin\mysql\mysql-8.4.3-winx64\bin\mysql.exe")
JAVA = Path(r"C:\Program Files\Java\jdk-21.0.10\bin\java.exe")
SCHEMA = "hello_doc_qa_" + uuid.uuid4().hex[:12]
PASSWORD = "OnlyForHelloDocQA2026"
BASE = "http://localhost:8081/api"
checks = []


def mysql(sql):
    subprocess.run([str(MYSQL), "--protocol=TCP", "--host=127.0.0.1", "--port=3308", "--user=root", "--batch", "--execute", sql], check=True, capture_output=True)


class Client:
    def __init__(self):
        self.cookies = http.cookiejar.CookieJar()
        self.http = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(self.cookies))
        self.csrf = None

    def request(self, method, path, body=None, expected=200, multipart=False):
        headers = {}
        if method != "GET" and self.csrf:
            headers["X-XSRF-TOKEN"] = self.csrf
        if multipart:
            boundary = "HelloDocQA" + uuid.uuid4().hex
            data = (f"--{boundary}\r\nContent-Disposition: form-data; name=\"registration\"\r\nContent-Type: application/json\r\n\r\n".encode()
                    + json.dumps(body).encode() + f"\r\n--{boundary}\r\nContent-Disposition: form-data; name=\"document\"; filename=\"qa-attestation.pdf\"\r\nContent-Type: application/pdf\r\n\r\n%PDF-1.4\n%QA fixture only\n%%EOF\r\n--{boundary}--\r\n".encode())
            headers["Content-Type"] = "multipart/form-data; boundary=" + boundary
        elif body is not None:
            data = json.dumps(body).encode()
            headers["Content-Type"] = "application/json"
        else:
            data = None
        try:
            response = self.http.open(urllib.request.Request(BASE + path, data=data, headers=headers, method=method), timeout=15)
        except urllib.error.HTTPError as error:
            response = error
        status = response.code
        raw = response.read()
        result = json.loads(raw) if raw else None
        assert status == expected, f"{method} {path}: expected {expected}, got {status}: {result}"
        return result

    def bootstrap(self):
        self.request("GET", "/auth/csrf")
        self.csrf = next(c.value for c in self.cookies if c.name == "XSRF-TOKEN")

    def login(self, email):
        self.bootstrap()
        return self.request("POST", "/auth/login", {"email": email, "password": PASSWORD})


def passed(name):
    checks.append(name)
    print("PASS " + name, flush=True)


server = None
log = None
try:
    assert SCHEMA.startswith("hello_doc_qa_") and len(SCHEMA) == 25
    mysql(f"CREATE DATABASE `{SCHEMA}` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci")
    env = os.environ.copy()
    env.update(HELLODOC_DB_URL=f"jdbc:mysql://127.0.0.1:3308/{SCHEMA}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
               HELLODOC_DB_USERNAME="root", HELLODOC_DB_PASSWORD="", HELLODOC_ADMIN_INITIAL_PASSWORD=PASSWORD,
               HELLODOC_EMAIL_ENABLED="false", HELLODOC_DOCUMENTS_DIR=str(ROOT / "target" / SCHEMA), SERVER_PORT="8081")
    env.pop("SPRING_PROFILES_ACTIVE", None)
    log = (ROOT / "target" / "qa-workflows.log").open("w", encoding="utf-8")
    server = subprocess.Popen([str(JAVA), f"-Djdk.net.unixdomain.tmpdir={ROOT / 'target'}", "-jar", str(ROOT / "target" / "hello-doc-backend-1.0.0.jar")], cwd=ROOT, env=env, stdout=log, stderr=subprocess.STDOUT)
    public = Client()
    for _ in range(90):
        if server.poll() is not None:
            raise RuntimeError("QA server exited; see target/qa-workflows.log")
        try:
            specialties = public.request("GET", "/specialties")
            # Tomcat accepts HTTP before ApplicationRunner finishes seeding.
            if specialties:
                break
            time.sleep(0.5)
        except (OSError, AssertionError):
            time.sleep(0.5)
    else:
        raise RuntimeError("QA server did not become ready")
    public.bootstrap()
    patient_body = dict(firstName="Patient", lastName="Test", email="patient@qa.example", phone="22123456", address="Adresse de test", password=PASSWORD, consent=True)
    invalid = public.request("POST", "/patients/registrations", {**patient_body, "phone": "22ABC456", "email": "invalid@domain"}, expected=400)
    assert "phone" in invalid["fieldErrors"] and "email" in invalid["fieldErrors"]
    passed("HTTP validation rejects letters in phone and incomplete email")
    public.request("POST", "/patients/registrations", patient_body, expected=201)
    doctor_body = {**patient_body, "firstName": "Médecin", "lastName": "Test", "email": "doctor@qa.example", "specialty": specialties[0]["name"], "licenseNumber": "QA-ONLY", "city": "Tunis"}
    application = public.request("POST", "/doctors/registrations", doctor_body, expected=201, multipart=True)
    assert application["status"] == "PENDING"
    public.request("POST", "/auth/login", {"email": doctor_body["email"], "password": PASSWORD}, expected=403)
    passed("Minimal physician registration stores document and awaits approval")
    admin = Client(); admin.login("admin@hellodoc.com")
    admin.request("POST", f"/admin/doctors/{application['id']}/approve", {})
    doctor = Client(); doctor.login(doctor_body["email"])
    profile = doctor.request("GET", "/doctors/me")
    assert profile["services"] == [] and profile["workingDays"] == []
    passed("Approval creates an unconfigured doctor without invented hours or services")
    doctor_id = profile["id"]
    profile_body = dict(firstName="Médecin", lastName="Test", phone="22123456", address="Adresse de test", serviceTitle="Cabinet de test", specialty=doctor_body["specialty"], location="Cabinet de test", description="Compte de recette isolé", services=[dict(name="Consultation de test", price="50 DT")], currentPassword=PASSWORD)
    doctor.request("PUT", "/doctors/me", profile_body)
    assert doctor.request("GET", "/doctors/me")["services"][0]["name"] == "Consultation de test"
    doctor.request("PUT", "/doctors/me/availabilities/weekly", dict(slotDurationMinutes=30, pauseMinutes=0, windows=[dict(isoWeekday=i, period=1, startTime="09:00", endTime="17:00") for i in range(1,8)]))
    passed("Services and weekly schedule are configured after approval and persist")
    date = (datetime.now(timezone(timedelta(hours=1))) + timedelta(days=2)).date().isoformat()
    month = date[:7]
    assert public.request("GET", f"/doctors/{doctor_id}/availability-calendar?month={month}")
    assert public.request("GET", f"/doctors/{doctor_id}/booking-policy")["timeZone"] == "Africa/Tunis"
    patient = Client(); patient.login(patient_body["email"])
    patient.request("GET", "/admin/dashboard", expected=403)
    passed("Public availability calendar and role restriction work over real HTTP")
    start = f"{date}T09:00:00Z"
    book = dict(doctorId=doctor_id, serviceName="Consultation de test", startAt=start)
    patient.request("POST", "/appointments", {**book, "expectedServicePrice": "1 DT"}, expected=409)
    second = Client(); second.login(patient_body["email"])
    def reserve(client):
        try:
            return client.request("POST", "/appointments", book, expected=201)
        except AssertionError as error:
            assert "got 409" in str(error), str(error)
            return None
    with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
        responses = list(pool.map(reserve, [patient, second]))
    assert sum(result is not None for result in responses) == 1
    appointment = next(result for result in responses if result is not None)
    passed("Two concurrent reservations produce one success and one conflict on MySQL")
    absence = dict(startDate=date, endDate=date, startTime="10:00", endTime="10:30", reason="Motif strictement privé QA", comment="Commentaire privé QA")
    assert doctor.request("POST", "/doctors/me/absences/preview", absence)["affectedAppointments"] == 1
    doctor.request("POST", "/doctors/me/absences", absence, expected=409)
    assert patient.request("GET", f"/appointments/{appointment['id']}")["status"] == "PENDING"
    passed("Absence preview is read-only and server requires explicit confirmation")
    saved_absence = doctor.request("POST", "/doctors/me/absences", {**absence, "confirmRescheduling": True}, expected=201)
    updated = patient.request("GET", f"/appointments/{appointment['id']}")
    assert updated["status"] == "TO_RESCHEDULE"
    assert any(a["id"] == appointment["id"] for a in patient.request("GET", "/appointments/mine")["content"])
    notification = patient.request("GET", "/notifications")["content"][0]
    assert notification["type"] == "APPOINTMENT_TO_RESCHEDULE" and "privé" not in json.dumps(notification, ensure_ascii=False)
    assert "privé" not in json.dumps(updated["statusHistory"], ensure_ascii=False)
    passed("Confirmed absence persists status, history and private-safe patient notification")
    new_start = f"{date}T13:00:00Z"
    result = patient.request("PUT", f"/appointments/{appointment['id']}/reschedule", dict(startAt=new_start))
    assert result["status"] == "PENDING" and result["startsAt"].startswith(date + "T13:00")
    doctor.request("DELETE", f"/doctors/me/absences/{saved_absence['id']}", expected=204)
    assert patient.request("GET", f"/appointments/{appointment['id']}")["startsAt"].startswith(date + "T13:00")
    passed("Patient rebooking awaits confirmation and removing absence never restores old dates")
    admin.request("PUT", "/admin/me", dict(firstName="Admin", lastName="Test", phone="22123456", address="Test", currentPassword=PASSWORD))
    assert admin.request("GET", "/admin/me")["firstName"] == "Admin"
    passed("Administrator profile changes persist")
    (ROOT / "target" / "qa-result.json").write_text(json.dumps(dict(passed=checks), ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"SUCCESS {len(checks)} integration scenarios", flush=True)
    if "--hold" in sys.argv:
        print("QA_BROWSER_READY doctor@qa.example / patient@qa.example (local fixture password defined in script). Press Enter to stop and remove the disposable schema.", flush=True)
        try: input()
        except EOFError: pass
finally:
    if server is not None:
        server.terminate()
        try: server.wait(timeout=10)
        except subprocess.TimeoutExpired: server.kill(); server.wait()
    if log: log.close()
    if SCHEMA.startswith("hello_doc_qa_") and len(SCHEMA) == 25:
        mysql(f"DROP DATABASE IF EXISTS `{SCHEMA}`")
    print("Disposable QA schema cleaned up.", flush=True)
