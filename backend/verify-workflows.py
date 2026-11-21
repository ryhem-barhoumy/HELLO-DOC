"""Exercise real HTTP/session/MySQL workflows in a disposable schema.

Requires the packaged jar and local Laragon root access without a password.
No existing application data is read or modified. --hold keeps the fixture server
available for browser QA until Enter is received. Test credentials are deliberately
local-only and are never used for the real hello_doc database.
"""
import struct
import zlib
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
import socketserver
import threading
from datetime import datetime, timedelta, timezone

ROOT = Path(__file__).resolve().parent
MYSQL = Path(r"C:\laragon\bin\mysql\mysql-8.4.3-winx64\bin\mysql.exe")
JAVA = Path(r"C:\Program Files\Java\jdk-21.0.10\bin\java.exe")
SCHEMA = "hello_doc_qa_" + uuid.uuid4().hex[:12]
PASSWORD = "OnlyForHelloDocQA2026"
QA_PORT = int(os.environ.get("HELLODOC_QA_PORT", "8081"))
BASE = f"http://localhost:{QA_PORT}/api"
checks = []


class FixtureSmtp(socketserver.ThreadingTCPServer):
    allow_reuse_address = True
    daemon_threads = True


class FixtureSmtpHandler(socketserver.StreamRequestHandler):
    def reply(self, message):
        self.wfile.write((message + "\r\n").encode()); self.wfile.flush()

    def handle(self):
        self.reply("220 HelloDoc isolated QA SMTP")
        while True:
            try:
                line = self.rfile.readline()
            except ConnectionError:
                return
            if not line: return
            command = line.decode(errors="replace").strip().upper()
            self.server.commands.append(command)
            if command.startswith(("EHLO", "HELO")):
                self.reply("250 HelloDoc QA")
            elif command.startswith(("MAIL FROM:", "RCPT TO:", "RSET", "NOOP")):
                self.reply("250 OK")
            elif command == "DATA":
                self.reply("354 End with a dot")
                message = bytearray()
                while (part := self.rfile.readline()) and part != b".\r\n":
                    message.extend(part)
                self.server.attempts.append(bytes(message))
                self.server.received.set()
                self.server.release.wait(20)
                self.reply("550 Simulated delivery failure" if self.server.reject else "250 Accepted for QA")
            elif command == "QUIT":
                self.reply("221 Goodbye"); return
            else:
                self.reply("502 Unsupported QA command")


def mysql(sql):
    subprocess.run([str(MYSQL), "--protocol=TCP", "--host=127.0.0.1", "--port=3308", "--user=root", "--batch", "--execute", sql], check=True, capture_output=True)


class Client:
    def __init__(self):
        self.cookies = http.cookiejar.CookieJar()
        self.http = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(self.cookies))
        self.csrf = None

    def request(self, method, path, body=None, expected=200, multipart=False, upload=None):
        headers = {}
        if method != "GET" and self.csrf:
            headers["X-XSRF-TOKEN"] = self.csrf
        if upload is not None:
            filename,content_type,photo_bytes = upload
            boundary = "HelloDocPhotoQA" + uuid.uuid4().hex
            data = (f'--{boundary}\r\nContent-Disposition: form-data; name="photo"; filename="{filename}"\r\nContent-Type: {content_type}\r\n\r\n'.encode() + photo_bytes + f"\r\n--{boundary}--\r\n".encode())
            headers["Content-Type"] = "multipart/form-data; boundary=" + boundary
        elif multipart:
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
        result = raw if response.headers.get_content_type().startswith("image/") else json.loads(raw) if raw else None
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
smtp = None
try:
    assert SCHEMA.startswith("hello_doc_qa_") and len(SCHEMA) == 25
    mysql(f"CREATE DATABASE `{SCHEMA}` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci")
    env = os.environ.copy()
    env.update(HELLODOC_DB_URL=f"jdbc:mysql://127.0.0.1:3308/{SCHEMA}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
               HELLODOC_FRONTEND_ORIGIN="http://localhost:4201", HELLODOC_FRONTEND_URL="http://localhost:4201", HELLODOC_DB_USERNAME="root", HELLODOC_DB_PASSWORD="", HELLODOC_ADMIN_INITIAL_PASSWORD=PASSWORD,
               HELLODOC_EMAIL_ENABLED="false", HELLODOC_DOCUMENTS_DIR=str(ROOT / "target" / SCHEMA), SERVER_PORT=str(QA_PORT))
    if "--mail" in sys.argv:
        smtp = FixtureSmtp(("127.0.0.1", 0), FixtureSmtpHandler)
        smtp.received = threading.Event(); smtp.release = threading.Event(); smtp.reject = True; smtp.attempts = []; smtp.commands = []
        threading.Thread(target=smtp.serve_forever, daemon=True).start()
        env.update(HELLODOC_EMAIL_ENABLED="true", HELLODOC_SMTP_HOST="127.0.0.1", HELLODOC_SMTP_PORT=str(smtp.server_address[1]),
                   HELLODOC_SMTP_AUTH="false", HELLODOC_SMTP_STARTTLS="false", HELLODOC_SMTP_USERNAME="", HELLODOC_SMTP_PASSWORD="", HELLODOC_EMAIL_FROM="approval@qa.example")
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
    approval_started = time.monotonic()
    approved = admin.request("POST", f"/admin/doctors/{application['id']}/approve", {})
    assert approved["status"] == "APPROVED"
    if smtp:
        assert time.monotonic() - approval_started < 8, "Approval must not wait for SMTP"
        mail_path = f"/admin/doctors/{application['id']}/approval-email"
        assert smtp.received.wait(15), f"Mail must be queued after approval commits: commands={smtp.commands}, status={admin.request('GET', mail_path)}"
        assert admin.request("GET", mail_path)["status"] == "QUEUED"
        smtp.release.set()
        def mail_status(expected):
            for _ in range(60):
                result = admin.request("GET", mail_path)
                if result["status"] == expected: return result
                time.sleep(0.1)
            raise AssertionError(f"Email state did not become {expected}: {result}")
        assert mail_status("FAILED")["attemptedAt"]
        assert admin.request("GET", "/admin/inbox")["failedEmails"]["totalElements"] == 1
        assert len(smtp.attempts) == 1
        passed("Approval commits and responds before SMTP; delivery failure is recorded separately")
        smtp.reject = False
        assert admin.request("POST", mail_path, {})["status"] == "QUEUED"
        assert mail_status("SENT")["sentAt"]
        assert admin.request("GET", "/admin/inbox")["failedEmails"]["totalElements"] == 0
        assert len(smtp.attempts) == 2 and b"doctor@qa.example" in smtp.attempts[-1]
        passed("Admin can retry failed approval email without reapproving the physician")
        assert admin.request("POST", f"/admin/doctors/{application['id']}/approve", {})["status"] == "APPROVED"
        assert admin.request("POST", mail_path, {})["status"] == "SENT"
        time.sleep(0.2)
        assert len(smtp.attempts) == 2
        public.request("GET", mail_path, expected=401)
        passed("Repeated approval and repeated successful mail retry create no duplicate profile or mail")
    else:
        assert approved["approvalEmailStatus"] == "DISABLED"
    doctor = Client(); doctor.login(doctor_body["email"])
    profile = doctor.request("GET", "/doctors/me")
    assert profile["services"] == [] and profile["workingDays"] == []
    passed("Approval creates an unconfigured doctor without invented hours or services")
    doctor_id = profile["id"]
    profile_body = dict(firstName="Médecin", lastName="Test", phone="22123456", address="Adresse de test", serviceTitle="Cabinet de test", specialty=doctor_body["specialty"], location="Cabinet de test", description="Compte de recette isolé", services=[dict(name="Consultation de test", price="50 DT")], currentPassword=PASSWORD)
    doctor.request("PUT", "/doctors/me", profile_body)
    assert doctor.request("GET", "/doctors/me")["services"][0]["name"] == "Consultation de test"
    weekly = dict(slotDurationMinutes=30, pauseMinutes=0, windows=[dict(isoWeekday=i, period=1, startTime="09:00", endTime="17:00") for i in range(1,8)])
    doctor.request("PUT", "/doctors/me/availabilities/weekly", weekly)
    doctor.request("PUT", "/doctors/me/availabilities/weekly", weekly)
    assert len(doctor.request("GET", "/doctors/me/availabilities")) == 7
    passed("Existing weekly hours can be replaced without a duplicate-window conflict")
    passed("Services and weekly schedule are configured after approval and persist")
    date = (datetime.now(timezone(timedelta(hours=1))) + timedelta(days=2)).date().isoformat()
    month = date[:7]
    assert public.request("GET", f"/doctors/{doctor_id}/availability-calendar?month={month}")
    assert public.request("GET", f"/doctors/{doctor_id}/booking-policy")["timeZone"] == "Africa/Tunis"
    patient = Client(); patient.login(patient_body["email"])
    patient.request("GET", "/admin/dashboard", expected=403)
    if smtp: patient.request("POST", mail_path, {}, expected=403)
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
    doctor.request("POST", f"/appointments/{appointment['id']}/cancellation", dict(reason="Annulation d’une demande en attente"), expected=409)
    doctor.request("GET", f"/appointments/{appointment['id']}/reschedule-options?date={date}", expected=403)
    doctor.request("PUT", f"/appointments/{appointment['id']}/reschedule", dict(startAt=f"{date}T13:00:00Z"), expected=403)
    assert patient.request("GET", f"/appointments/{appointment['id']}")["status"] == "PENDING"
    passed("Doctor can neither cancel a pending request nor reprogram a patient appointment")
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
    doctor.request("PATCH", f"/appointments/{appointment['id']}/status", dict(status="CONFIRMED"))
    assert doctor.request("POST", f"/appointments/{appointment['id']}/cancellation", dict(reason="Annulation après confirmation"))["status"] == "CANCELLED"
    assert patient.request("GET", f"/appointments/{appointment['id']}")["status"] == "CANCELLED"
    passed("Doctor can still cancel a confirmed appointment and the patient sees its cancellation")

    # All SQL below targets only the generated, disposable QA schema.
    assert SCHEMA.startswith("hello_doc_qa_") and len(SCHEMA) == 25
    clinical = patient.request("POST", "/appointments", {**book, "startAt": f"{date}T14:00:00Z", "reason": "Motif clinique confidentiel QA"}, expected=201)
    clinical_id = clinical["id"]
    doctor.request("PATCH", f"/appointments/{clinical_id}/status", dict(status="CONFIRMED"))
    moved = patient.request("PUT", f"/appointments/{clinical_id}/reschedule", dict(startAt=f"{date}T15:00:00Z"))
    assert moved["status"] == "PENDING"
    passed("Moving a confirmed appointment requires a fresh physician confirmation")
    doctor.request("PATCH", f"/appointments/{clinical_id}/status", dict(status="CONFIRMED"))
    mysql(f"UPDATE `{SCHEMA}`.appointments SET start_at=UTC_TIMESTAMP()-INTERVAL 2 HOUR,end_at=UTC_TIMESTAMP()-INTERVAL 90 MINUTE WHERE id=UNHEX(REPLACE('{clinical_id}','-',''))")
    patient.request("GET", f"/appointments/{clinical_id}/reschedule-options?date={date}", expected=409)
    doctor.request("PATCH", f"/appointments/{clinical_id}/status", dict(status="COMPLETED"))
    note_path = f"/appointments/{clinical_id}/consultation-note"
    assert doctor.request("GET", note_path) is None
    patient.request("GET", note_path, expected=403)
    admin.request("GET", note_path, expected=403)
    public.request("GET", note_path, expected=401)
    passed("Only the treating physician can open a completed consultation note")
    note = dict(resume="Résumé clinique QA", observation="Observations QA "+"Élément de suivi. "*120, treatment="Recommandations QA", privateNote="SECRET_MEDICAL_QA", nextCheck=date)
    doctor.request("PUT", note_path, {**note, "resume": ""}, expected=400)
    saved = doctor.request("PUT", note_path, note)
    assert saved["version"] == 0 and saved["privateNote"] == "SECRET_MEDICAL_QA"
    assert doctor.request("GET", note_path)["observation"] == note["observation"].strip()
    doctor.request("PUT", note_path, note, expected=409)
    revised = doctor.request("PUT", note_path, {**note, "resume":"Résumé révisé QA", "version":saved["version"]})
    assert revised["version"] > saved["version"]
    doctor.request("PUT", note_path, {**note, "version":saved["version"]}, expected=409)
    assert doctor.request("GET", note_path)["resume"] == "Résumé révisé QA"
    assert len(doctor.request("GET", note_path+"/history")) == 2
    followups = [n for n in patient.request("GET", "/notifications")["content"] if n["type"] == "FOLLOW_UP_RECOMMENDED"]
    assert len(followups) == 1 and "SECRET_MEDICAL_QA" not in json.dumps(followups)
    passed("Clinical notes persist with revision checks and a private-safe follow-up notification")
    directory = doctor.request("GET", "/doctors/me/patients?q=Patient")
    assert directory["totalElements"] == 1 and directory["content"][0]["completedCount"] == 1
    patient_id = directory["content"][0]["id"]
    dossier = doctor.request("GET", f"/doctors/me/patients/{patient_id}")
    assert dossier["appointments"][0]["consultationNote"]["resume"] == "Résumé révisé QA"
    doctor.request("GET", f"/doctors/me/patients/{uuid.uuid4()}", expected=404)
    patient.request("GET", f"/doctors/me/patients/{patient_id}", expected=403)
    passed("Physician patient directory and paged dossier use completed visits only")
    # Another approved physician cannot infer a consultation from its identifier.
    other_public = Client(); other_public.bootstrap()
    other_body = {**doctor_body, "email":"other-doctor@qa.example", "licenseNumber":"QA-OTHER-DOCTOR"}
    other_public.request("POST", "/doctors/registrations", other_body, expected=201, multipart=True)
    pending = admin.request("GET", "/admin/doctors?status=PENDING")[0]
    admin.request("POST", f"/admin/doctors/{pending['id']}/approve", {})
    other_doctor = Client(); other_doctor.login(other_body["email"])
    other_doctor.request("GET", note_path, expected=404)
    other_doctor.request("PUT", note_path, note, expected=404)
    other_doctor.request("GET", f"/doctors/me/patients/{patient_id}", expected=404)
    passed("Another physician cannot read, overwrite or discover another physician's clinical dossier")
    operational = admin.request("GET", f"/admin/appointments/{clinical_id}")
    assert operational["status"] == "COMPLETED" and "reason" not in operational and "consultationNote" not in operational
    assert admin.request("GET", "/admin/appointments?status=COMPLETED")["totalElements"] == 1
    patient.request("GET", "/admin/appointments", expected=403)
    admin.request("DELETE", f"/admin/doctors/{doctor_id}", expected=409)
    admin.request("DELETE", f"/admin/patients/{patient_id}", expected=409)
    overview = admin.request("GET", "/admin/overview")
    assert overview["appointments"]["completed"] == 1 and len(overview["appointmentsByMonth"]) == 6
    assert any(a["action"] == "DOCTOR_APPROVED" for a in admin.request("GET", "/admin/activity-logs")["content"])
    passed("Administrative statistics, filters and activity are real and exclude clinical content; history blocks hard deletion")
    admin.request("PATCH", f"/admin/doctors/{doctor_id}/status", dict(status="INACTIVE"))
    revoked = doctor.request("GET", "/doctors/me", expected=401)
    assert revoked["code"] == "SESSION_REVOKED"
    doctor.request("GET", "/doctors/me", expected=401)
    assert any(a["action"] == "ACCOUNT_DEACTIVATED" for a in admin.request("GET", "/admin/activity-logs")["content"])
    admin.request("PATCH", f"/admin/doctors/{doctor_id}/status", dict(status="ACTIVE"))
    doctor.login(doctor_body["email"])
    passed("Deactivation revokes an existing session and records the administrative decision")

    doctor.request("PUT", "/doctors/me/availabilities/weekly",{**weekly,"windows":weekly["windows"][:-1]})
    assert len(doctor.request("GET", "/doctors/me/availabilities")) == 6
    assert patient.request("GET", f"/appointments/{clinical_id}")["status"] == "COMPLETED"
    doctor.request("PUT", "/doctors/me/availabilities/weekly",weekly)
    passed("Changing weekly hours preserves existing appointments and consultation history")
    # Synthetic PNG used only to exercise storage, authorization and image reencoding.
    def png_chunk(kind, payload):
        return struct.pack(">I", len(payload)) + kind + payload + struct.pack(">I", zlib.crc32(kind + payload) & 0xffffffff)
    pixels = b"".join(b"\x00" + bytes([30,130,110,255])*16 for _ in range(16))
    png = b"\x89PNG\r\n\x1a\n" + png_chunk(b"IHDR",struct.pack(">IIBBBBB",16,16,8,6,0,0,0)) + png_chunk(b"IDAT",zlib.compress(pixels)) + png_chunk(b"IEND",b"")
    (ROOT / "target" / "qa-profile-photo.png").write_bytes(png)
    doctor.request("POST", "/account/me/photo", upload=("fake.jpg","image/jpeg",b"not an image"), expected=400)
    first_photo = doctor.request("POST", "/account/me/photo", upload=("portrait.png","image/png",png))
    assert first_photo["photoUrl"].startswith("/api/account/me/photo?v=")
    assert doctor.request("GET", "/account/me/photo").startswith(b"\xff\xd8")
    assert public.request("GET", f"/doctors/{doctor_id}/photo").startswith(b"\xff\xd8")
    public_profile = public.request("GET", f"/doctors/{doctor_id}")
    assert public_profile["photoUrl"].startswith(f"/api/doctors/{doctor_id}/photo?v=")
    assert any(d["id"] == doctor_id and d["photoUrl"] for d in public.request("GET", "/doctors")["content"])
    public.request("GET", "/account/me/photo", expected=401)
    patient.request("GET", "/account/me/photo", expected=404)
    passed("Doctor photo is persisted, reencoded as JPEG and linked from real public profiles and search")
    second_photo = doctor.request("POST", "/account/me/photo", upload=("portrait.png","image/png",png))
    assert second_photo["photoUrl"] != first_photo["photoUrl"]
    fresh_doctor = Client(); fresh_doctor.login(doctor_body["email"])
    assert fresh_doctor.request("GET", "/auth/me")["photoUrl"] == second_photo["photoUrl"]
    fresh_doctor.request("DELETE", "/account/me/photo", expected=204)
    assert fresh_doctor.request("GET", "/auth/me")["photoUrl"] is None
    public.request("GET", f"/doctors/{doctor_id}/photo", expected=404)
    doctor.request("POST", "/account/me/photo", upload=("portrait.png","image/png",png))
    passed("Photo replacement changes the version, survives a new login and can be removed")
    patient.request("POST", "/account/me/photo", upload=("portrait.png","image/png",png))
    assert patient.request("GET", "/account/me/photo").startswith(b"\xff\xd8")
    assert patient.request("GET", "/patients/me")["photoUrl"]
    other_doctor.request("GET", "/account/me/photo", expected=404)
    passed("Patient photo stays private to the account and cannot be read through another account's own-photo endpoint")
    parallel_patient = Client(); parallel_patient.login(patient_body["email"])
    contact_update = dict(firstName="Patient",lastName="Test",phone="22123456",address="Adresse de recette",currentPassword=PASSWORD)
    with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
        upload_future = pool.submit(patient.request,"POST","/account/me/photo",upload=("portrait.png","image/png",png))
        contact_future = pool.submit(parallel_patient.request,"PUT","/patients/me",contact_update)
        last_photo = upload_future.result(); contact_future.result()
    assert patient.request("GET", "/auth/me")["photoUrl"] == last_photo["photoUrl"]
    assert patient.request("GET", "/patients/me")["address"] == "Adresse de recette"
    passed("Concurrent photo and contact updates keep both changes")

    (ROOT / "target" / "qa-result.json").write_text(json.dumps(dict(passed=checks), ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"SUCCESS {len(checks)} integration scenarios", flush=True)
    if "--hold" in sys.argv:
        # Deliberate QA-only density: exercise the condensed weekly view and the full-day action.
        for hour in (8,9,10,11,12):
            try:
                dense = patient.request("POST", "/appointments", {**book, "startAt":f"{date}T{hour:02d}:00:00Z"}, expected=201)
                if hour in (9,11):doctor.request("PATCH", f"/appointments/{dense['id']}/status", dict(status="CONFIRMED"))
            except AssertionError as error:
                if "got 409" not in str(error): raise
        print("QA_BROWSER_READY doctor@qa.example / patient@qa.example (local fixture password defined in script). Press Enter to stop and remove the disposable schema.", flush=True)
        try: input()
        except EOFError: pass
finally:
    if smtp:
        smtp.release.set()
        smtp.shutdown(); smtp.server_close()
    if server is not None:
        server.terminate()
        try: server.wait(timeout=10)
        except subprocess.TimeoutExpired: server.kill(); server.wait()
    if log: log.close()
    if SCHEMA.startswith("hello_doc_qa_") and len(SCHEMA) == 25:
        mysql(f"DROP DATABASE IF EXISTS `{SCHEMA}`")
    print("Disposable QA schema cleaned up.", flush=True)
