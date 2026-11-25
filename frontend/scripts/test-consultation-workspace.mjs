import { readFile } from 'node:fs/promises';
import test from 'node:test';
import assert from 'node:assert/strict';

const read=path=>readFile(new URL(path,import.meta.url),'utf8');
const [detailHtml,detailTs,workspace,note,meetService,appointmentService,routes,guard]=await Promise.all([
  read('../src/app/features/appointments/appointment-detail.component.html'),
  read('../src/app/features/appointments/appointment-detail.component.ts'),
  read('../src/app/features/appointments/teleconsultation.component.ts'),
  read('../src/app/features/doctor/consultation-note.component.ts'),
  read('../../backend/src/main/java/com/hellodoc/service/AppointmentMeetLinkService.java'),
  read('../../backend/src/main/java/com/hellodoc/service/AppointmentService.java'),
  read('../src/app/app.routes.ts'),
  read('../src/app/core/guards/unsaved-consultation-workspace.guard.ts')
]);
const workspaceCss=await read('../src/app/features/appointments/teleconsultation.component.css');
const accessGuard=await read('../src/app/core/guards/consultation-access.guard.ts');
const noteCss=await read('../src/app/features/doctor/consultation-note.component.css');
const reminder=await read('../src/app/shared/teleconsultation-reminder.component.ts');
const workspaceHeader=await read('../src/app/shared/workspace-header.component.ts');
const portal=await read('../src/app/features/portal/portal.component.ts');

test('doctor keeps the original entry and can open it before the video window',()=>{
  assert.match(detailHtml,/Commencer la téléconsultation/);
  assert.match(detailTs,/get canOpenVideo\(\).*consultationMode==='VIDEO'/);
  assert.doesNotMatch(detailTs,/get canOpenVideo\(\).*videoWindowOpen/);
  assert.doesNotMatch(detailHtml,/Rejoindre la téléconsultation|Rejoindre WebRTC/);
});
test('patient can open the existing room for any confirmed video appointment, before a session exists',()=>{
  assert.match(detailHtml,/role==='PATIENT'&&appointment\.status==='CONFIRMED'&&consultationMode==='VIDEO'/);
  assert.match(detailHtml,/\['\/patient\/rendez-vous',appointment\.id,'teleconsultation'\]/);
  assert.match(workspace,/role==='PATIENT'&&e\.status===404\)this\.watchForSession\(\)/);
  assert.match(workspace,/private watchForSession\(createForDoctor=false\)/);
});
test('in-person consultations open the same clinical workspace without creating video',()=>{
  assert.match(detailHtml,/consultationMode==='IN_PERSON'.*teleconsultation/);
  assert.match(workspace,/visit\.consultationMode==='VIDEO'/);
  assert.match(workspace,/app-consultation-note \[embedded\]="true"/);
  assert.doesNotMatch(workspace,/<iframe/);
});
test('opening or hiding the medical panel keeps the child mounted and the WebRTC component intact',()=>{
  assert.match(workspace,/\[hidden\]="!showClinical"/);
  assert.match(workspace,/this\.api\.post<SessionDto>\(`\/teleconsultations\/\$\{this\.session\.id\}\/start`/);
  const loadSession=workspace.match(/private loadSession\(\)[\s\S]*?(?=private createSession\()/)?.[0]??'';
  assert.ok(loadSession);
  assert.doesNotMatch(loadSession,/\.join\(/);
});
test('Meet has an explicit demo notice, participant link and explicit doctor end action',()=>{
  assert.match(workspace,/Google Meet — démonstration uniquement/);
  assert.match(workspace,/meet-link\/external-call-ended/);
  assert.match(workspace,/J’ai terminé l’appel Google Meet/);
  assert.match(meetService,/now\.isBefore\(a\.getStartAt\(\)\.minusSeconds\(15\*60L\)\)/);
  assert.match(meetService,/now\.isAfter\(a\.getStartAt\(\)\.plusSeconds\(30\*60L\)\)/);
});
test('Meet controls live only in the teleconsultation room, never in appointment details',()=>{
  assert.doesNotMatch(detailHtml,/meet-backup|meet-link|Google Meet|saveMeetLink/);
  assert.doesNotMatch(detailTs,/loadMeetLink|saveMeetLink|removeMeetLink/);
  assert.match(workspace,/Google Meet — démonstration uniquement/);
  assert.match(workspace,/meet-link\/external-call-ended/);
});
test('both video tiles render before session state arrives and admission stays inside the existing room',()=>{
  assert.match(workspace,/class="teleconsult-layout"/);
  const videoArea=workspace.match(/class="teleconsult-layout"[\s\S]*?class="meet-backup"/)?.[0]??'';
  assert.match(videoArea,/class="video-grid"[\s\S]*?class="video-tile"[\s\S]*?<video[\s\S]*?class="video-tile"[\s\S]*?<video/);
  assert.doesNotMatch(videoArea,/teleconsult-layout" \*ngIf="session/);
  assert.match(videoArea,/Rejoindre la séance/);
});
test('the existing chat still uses the session message API and CHAT signal path',()=>{
  assert.match(workspace,/loadMessages\(\)/);
  assert.match(workspace,/teleconsultations\/\$\{this\.session\.id\}\/messages/);
  assert.match(workspace,/this\.signal\('CHAT'/);
});
test('Meet completion is separate from appointment completion and a WebRTC session is never fabricated',()=>{
  assert.match(workspace,/this\.api\.patch<any>\(`\/appointments\/\$\{this\.appointmentId\}\/status`/);
  assert.match(appointmentService,/a\.getMeetCallEndedAt\(\)!=null/);
  assert.match(workspace,/Terminer l’appel vidéo/);
  assert.match(workspace,/Terminer la consultation/);
});
test('a finalized clinical report needs a correction reason and keeps the earlier version',()=>{
  assert.match(note,/Motif de correction du compte rendu finalisé/);
  assert.match(note,/Enregistrer la correction traçable/);
  assert.match(note,/Consulter la version précédente/);
});
test('leaving the consultation route warns about unsaved clinical and prescription input',()=>{
  assert.match(routes,/teleconsultation[^\n]*canDeactivate:\[unsavedConsultationWorkspaceGuard\]/);
  assert.match(guard,/hasUnsavedClinicalChanges\(\)/);
  assert.match(note,/hasUnsavedChanges\(\).*this\.form\.dirty/s);
});
test('the existing chat stays visible before and after the session starts, with sending gated until joined',()=>{
  assert.match(workspace,/<aside class="surface chat-panel">/);
  assert.doesNotMatch(workspace,/<ng-container \*ngIf="session as currentSession">/);
  assert.match(workspace,/!session\|\|session\.state==='WAITING'/);
  assert.match(workspace,/\[disabled\]="!session\|\|session\.state!=='ACTIVE'\|\|!joined"/);
  assert.match(workspace,/\[disabled\]="!draft\.trim\(\)\|\|!session\|\|session\.state!=='ACTIVE'\|\|!joined"/);
  assert.match(workspace,/teleconsultations\/\$\{this\.session\.id\}\/messages/);
  assert.match(workspace,/this\.signal\('CHAT'/);
  assert.match(workspaceCss,/\.teleconsult-layout\{display:grid;grid-template-columns:minmax\(0,1fr\) 340px/);
  assert.doesNotMatch(workspaceCss,/teleconsult-layout\.awaiting-session/);
  assert.match(workspaceCss,/@media\(max-width:850px\)\{\.teleconsult-layout\{grid-template-columns:1fr\}\}/);
});
test('clinical and prescription fields are locked before video starts and editable after the call ends',()=>{
  assert.match(workspace,/\[editable\]="clinicalEditable"/);
  assert.match(workspace,/get clinicalEditable\(\).*session\?\.state==='ACTIVE'.*session\?\.state==='COMPLETED'/);
  assert.match(note,/@Input\(\) editable=true/);
  assert.match(note,/fieldset \[disabled\]="!editable"/);
  assert.match(note,/\[disabled\]="!editable\|\|busy"/);
});
test('doctor opens the room immediately and the detail button does not create a WebRTC session',()=>{
  const open=detailTs.match(/openTeleconsultation\(\)\{([^}]+)\}/)?.[1]??'';
  assert.match(open,/router\.navigate\(\['\/medecin\/rendez-vous',this\.appointmentId,'teleconsultation'\]\)/);
  assert.doesNotMatch(open,/api\.post/);
});
test('doctor room defers session creation until the 15-minute window and preserves the existing start action',()=>{
  const loadSession=workspace.match(/private loadSession\(\)[\s\S]*?(?=private watchForSession\()/)?.[0]??'';
  const createSession=workspace.match(/private createSession\(\)\{([^}]+)\}/)?.[1]??'';
  assert.match(loadSession,/e\.status===404&&this\.videoWindowOpen\)this\.createSession\(\)/);
  assert.match(loadSession,/\(e\.status===404\|\|e\.status===409\)\)this\.watchForSession\(true\)/);
  assert.match(createSession,/this\.role!=='DOCTOR'\|\|!this\.videoWindowOpen/);
  assert.match(workspace,/get videoWindowMessage\(\).*videoWindowOpensAt/s);
  assert.match(workspace,/\[disabled\]="busy\|\|!videoWindowOpen"/);
  const start=workspace.match(/start\(\)\{([^}]+)\}/)?.[1]??'';
  assert.match(start,/this\.role!=='DOCTOR'\|\|!this\.videoWindowOpen/);
  assert.match(workspace,/this\.api\.post<SessionDto>\(`\/teleconsultations\/\$\{this\.session\.id\}\/start`/);
});
test('consultation guard rejects non-participants, cancelled visits and patient in-person visits',()=>{
  assert.match(routes,/teleconsultation[^\n]*canActivate:\[roleGuard,consultationAccessGuard\]/);
  assert.match(accessGuard,/appointment\.status\s*===\s*'CONFIRMED'\s*\|\|\s*appointment\.status\s*===\s*'COMPLETED'/);
  assert.match(accessGuard,/role\s*===\s*'DOCTOR'\s*\?\s*\['VIDEO',\s*'IN_PERSON'\]/);
  assert.match(accessGuard,/router\.parseUrl\('\/access-denied'\)/);
  assert.match(appointmentService,/Appointment appointment=owned\(email,id\)/);
});
test('clinical report keeps its model and validations while arranging fields in a responsive grid',()=>{
  assert.match(note,/class="clinical-fields"/);
  assert.match(note,/clinical-field-wide"><label for="note-resume"/);
  assert.match(note,/formControlName="resume"/);
  assert.match(note,/formControlName="observation"/);
  assert.match(note,/formControlName="treatment"/);
  assert.match(note,/formControlName="nextCheck"/);
  assert.match(note,/formControlName="recommendedDelay"/);
  assert.match(note,/formControlName="followUpInstructions"/);
  assert.match(note,/formControlName="privateNote"/);
  assert.match(noteCss,/grid-template-columns:repeat\(2,minmax\(0,1fr\)\)/);
  assert.match(noteCss,/@media\(max-width:640px\)\{\.clinical-fields\{grid-template-columns:minmax\(0,1fr\)/);
});
test('patient Meet link synchronizes through the existing API without overlapping polls or touching the doctor draft',()=>{
  const meetPolling=workspace.match(/if\(this\.role==='PATIENT'\)\{this\.meetPoll=interval\(5000\)\.pipe\([\s\S]*?subscribe\(\{next:m=>\{this\.meet=m;this\.meetError='';\}\}\);/)?.[0]??'';
  assert.ok(meetPolling);
  assert.match(meetPolling,/exhaustMap/);
  assert.match(meetPolling,/\/appointments\/\$\{this\.appointmentId\}\/meet-link/);
  assert.match(meetPolling,/e\.status===404\?of\(/);
  assert.doesNotMatch(meetPolling,/this\.load\(\)|join\(|messages|RTCPeerConnection/);
  assert.match(workspace,/this\.meetPoll\?\.unsubscribe\(\)/);
  assert.match(workspace,/meetPoll\?\.unsubscribe\(\);\}\n  load\(/);
  assert.match(workspace,/next:m=>\{this\.meet=m;this\.meetLinkDraft=m\.url\?\?''/);
});
test('teleconsultation reminders are local, deduplicated, and link to the existing role-specific room',()=>{
  assert.match(reminder,/status === 'CONFIRMED' && a\.consultationMode === 'VIDEO'/);
  assert.match(reminder,/open - 5 \* 60_000/);
  assert.match(reminder,/timeZone: 'Africa\/Tunis'/);
  assert.match(reminder,/sessionStorage\.setItem/);
  assert.match(reminder,/exhaustMap/);
  assert.match(reminder,/\['\/medecin\/rendez-vous', appointmentId, 'teleconsultation'\]/);
  assert.match(reminder,/\['\/patient\/rendez-vous', appointmentId, 'teleconsultation'\]/);
  assert.doesNotMatch(reminder,/getUserMedia|RTCPeerConnection|WebSocket|privateNote|reason/);
  assert.match(workspaceHeader,/app-teleconsultation-reminder/);
  assert.match(portal,/TeleconsultationReminderComponent/);
});
