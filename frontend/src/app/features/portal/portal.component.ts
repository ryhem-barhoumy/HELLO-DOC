import { DoctorAccountComponent } from '../doctor/doctor-account.component';
import { WeeklyAvailabilityComponent } from '../doctor/weekly-availability.component';
import { AvatarComponent } from '../../shared/avatar.component';
import { ProfilePhotoComponent } from '../../shared/profile-photo.component';
import { TeleconsultationReminderComponent } from '../../shared/teleconsultation-reminder.component';
import { NotificationCounter } from '../../core/services/notification-counter.service';
import { WorkspaceFooterComponent } from '../../shared/workspace-footer.component';
import { AdminInboxComponent } from '../admin/admin-inbox.component';
import { DoctorPatientsComponent } from '../doctor/doctor-patients.component';
import { AdminOperationsComponent } from '../admin/admin-operations.component';
import { CabinetDatePipe } from '../../shared/cabinet-date.pipe';
import { AdminSpecialtiesComponent } from '../admin/admin-specialties.component';
import { ApprovalEmailComponent } from '../admin/approval-email.component';
import { IconComponent } from '../../shared/icon.component';
import { Title } from '@angular/platform-browser';
import { Component, OnInit, inject, DestroyRef, HostListener, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AbstractControl, FormArray, FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ApiService } from '../../core/services/api.service';
import { AuthService } from '../../core/services/auth.service';
import { AgendaComponent } from '../appointments/agenda.component';
import { DigitsOnlyDirective } from '../../shared/digits-only.directive';
import { Subscription } from 'rxjs';
import { environment } from '../../../environments/environment';
import { appointmentPermissions } from '../../core/models/appointment-permissions';
import { ConsultationMode } from '../../core/models/api.models';
import { PasswordFieldComponent } from '../../shared/password-field.component';
import { DoctorDirectoryComponent } from '../public/doctor-directory.component';

interface Item { label: string; icon: string; path: string; }
type PatientAccountTab = 'photo' | 'details' | 'security';

@Component({ selector: 'app-portal', standalone: true, imports: [DoctorAccountComponent,WeeklyAvailabilityComponent,CommonModule, FormsModule, ReactiveFormsModule, RouterLink, AgendaComponent, DigitsOnlyDirective, IconComponent, AdminSpecialtiesComponent, ApprovalEmailComponent,DoctorPatientsComponent,AdminOperationsComponent,CabinetDatePipe,AdminInboxComponent,AvatarComponent,ProfilePhotoComponent,WorkspaceFooterComponent,TeleconsultationReminderComponent,PasswordFieldComponent,DoctorDirectoryComponent], templateUrl: './portal.component.html', styleUrls: ['./patient-portal.css', './patient-appointment-list.css'] })
export class PortalComponent implements OnInit {
  private readonly notificationCounter = inject(NotificationCounter, { optional: true });
  private route = inject(ActivatedRoute); private api = inject(ApiService); private auth = inject(AuthService); private router = inject(Router); private fb = inject(FormBuilder); private destroyRef=inject(DestroyRef);
  role = this.route.snapshot.data['roles']?.[0] ?? 'PATIENT'; roleLabel = this.role === 'ADMIN' ? 'ADMINISTRATION' : this.role === 'DOCTOR' ? 'MÉDECIN' : 'PATIENT';
  base = this.role === 'ADMIN' ? '/admin' : this.role === 'DOCTOR' ? '/medecin' : '/patient';
  area = this.route.snapshot.paramMap.get('area') ?? ''; loading = false; error = ''; errorTitle='Impossible de charger les données'; notice = ''; unavailableMessage=''; expandedRecordId:string|null=null; menuOpen = false; records: any[] = []; dashboard: any = null;
  @ViewChild('refusalDialog') refusalDialog?:ElementRef<HTMLDialogElement>;
  refusalCandidate:any=null;refusalReason='';refusalError='';
  confirmRefusal(){if(this.busy||!this.refusalCandidate)return;if(!this.refusalReason.trim()){this.refusalError='Indiquez le motif du refus.';return;}this.sendDecision(this.refusalCandidate,false,this.refusalReason.trim());}
  closeRefusal(){if(this.busy)return;this.refusalDialog?.nativeElement.close();this.refusalCandidate=null;}
  approvalOutcome: {applicationId:string;name:string}|null=null;
  private title=inject(Title);
  isMobile=window.matchMedia('(max-width:760px)').matches;
  @HostListener('window:resize') resize(){this.isMobile=window.matchMedia('(max-width:760px)').matches;}
  @HostListener('document:keydown.escape') closeMenu(){if(this.menuOpen){this.menuOpen=false;document.querySelector<HTMLElement>('.mobile-menu')?.focus();}}
  @ViewChild(DoctorAccountComponent) doctorAccount?:DoctorAccountComponent;
  @ViewChild(WeeklyAvailabilityComponent) weeklyEditor?:WeeklyAvailabilityComponent;
  get editorSaving(){return this.doctorAccount?.saving===true||this.weeklyEditor?.saving===true;}
  hasUnsavedDoctorChanges(){return this.doctorAccount?.hasUnsavedChanges()===true||this.weeklyEditor?.hasUnsavedChanges()===true;}
  @HostListener('window:beforeunload',['$event']) protectDoctorDraft(event:BeforeUnloadEvent){if(this.hasUnsavedDoctorChanges()||this.editorSaving){event.preventDefault();event.returnValue='';}}
  setupProfile:any=null;
  page=0; pageSize=24; totalElements=0; totalPages=0; private loadRequest?: Subscription;
  absenceImpact=0; pendingAbsence:any=null;
  constructor(){this.destroyRef.onDestroy(()=>this.loadRequest?.unsubscribe());}
  monthCursor = new Date(new Date().getFullYear(), new Date().getMonth(), 1);
  search = this.fb.control(''); statusFilter = this.fb.control('');
  patientFilters = { q: '', status: '' };
  readonly readingNotifications = new Set<string>();
  readonly patientAccountTabs: { id: PatientAccountTab; label: string; icon: string }[] = [
    { id: 'photo', label: 'Photo de profil', icon: 'camera' },
    { id: 'details', label: 'Informations personnelles', icon: 'profil' },
    { id: 'security', label: 'Sécurité et mot de passe', icon: 'security' }
  ];
  patientAccountTab: PatientAccountTab = 'photo';
  availabilityForm=this.fb.group({isoWeekday:['1',[Validators.required]],start:['',[Validators.required]],end:['',[Validators.required]]});
  absenceForm=this.fb.group({type:['FULL_DAY'],startDate:['',[Validators.required]],endDate:['',[Validators.required]],startTime:[''],endTime:[''],reason:['',[Validators.required]],comment:['']});
  profile:any=null;profileDays=['1','2','3','4','5'];profileForm=this.fb.group({firstName:['',[Validators.required]],lastName:['',[Validators.required]],phone:['',[Validators.pattern(/^[0-9]{8,15}$/)]],address:[''],serviceTitle:[''],specialty:[''],durationMinutes:[30],workStart:['08:00'],workEnd:['17:00'],pauseMinutes:[0],location:[''],description:[''],currentPassword:['',[Validators.required]],services:this.fb.array([])});
  passwordForm=this.fb.group({currentPassword:['',[Validators.required]],newPassword:['',[Validators.required,Validators.minLength(8),Validators.maxLength(72)]],confirmPassword:['',[Validators.required]]});
  busy=false; today=new Date().toLocaleDateString('en-CA'); specialties:string[]=[]; editingAvailabilityId:string|null=null; editingAbsenceId:string|null=null;
  get userName(){return this.auth.user() ? `${this.auth.user()!.firstName} ${this.auth.user()!.lastName}` : 'Mon espace';}
  get accountPhoto(){return this.auth.user()?.photoUrl??null;}
  get firstName(){return this.auth.user()?.firstName??'Mon espace';}
  readonly headerDate=new Date();
  accountSection(id:string){const target=document.getElementById(id);target?.scrollIntoView({behavior:'smooth',block:'start'});target?.focus({preventScroll:true});}
  get initials() { return this.auth.user() ? (this.auth.user()!.firstName[0] + this.auth.user()!.lastName[0]).toUpperCase() : 'HD'; }
  get profileServices(){return this.profileForm.controls.services as FormArray;}
  profileDayOptions=[{value:'1',label:'Lundi'},{value:'2',label:'Mardi'},{value:'3',label:'Mercredi'},{value:'4',label:'Jeudi'},{value:'5',label:'Vendredi'},{value:'6',label:'Samedi'},{value:'7',label:'Dimanche'}];
  toggleProfileDay(value:string,checked:boolean){this.profileDays=checked?[...new Set([...this.profileDays,value])]:this.profileDays.filter(d=>d!==value);}
  addProfileService(service:any={name:'',price:''}){const required=this.role==='DOCTOR'?[Validators.required]:[];this.profileServices.push(this.fb.group({name:[service.name??'', [...required,Validators.maxLength(160)]],price:[service.price??'', [...required,Validators.maxLength(40)]]}));}
  removeProfileService(i:number){this.profileServices.removeAt(i);}
  patientItems: Item[] = [{label:'Vue d’ensemble',icon:'⌂',path:''},{label:'Trouver un médecin',icon:'⌕',path:'medecins'},{label:'Mes rendez-vous',icon:'▦',path:'rendez-vous'},{label:'Mon dossier médical',icon:'◷',path:'dossier-medical'},{label:'Suivi & historique',icon:'◷',path:'historique'},{label:'Notifications',icon:'♧',path:'notifications'},{label:'Mon compte',icon:'○',path:'profil'}];
  doctorItems: Item[] = [{label:'Vue d’ensemble',icon:'⌂',path:''},{label:'Rendez-vous',icon:'▦',path:'rendez-vous'},{label:'Agenda',icon:'▣',path:'calendrier'},{label:'Disponibilités',icon:'◷',path:'disponibilites'},{label:'Absences',icon:'⊘',path:'absences'},{label:'Patients & dossiers médicaux',icon:'♧',path:'patients'},{label:'Avis patients',icon:'☆',path:'avis'},{label:'Notifications',icon:'♧',path:'notifications'},{label:'Mon compte',icon:'○',path:'profil'}];
  adminItems: Item[] = [{label:'Vue d’ensemble',icon:'⌂',path:''},{label:'Médecins',icon:'✚',path:'medecins'},{label:'Approbations',icon:'✓',path:'approbations'},{label:'Patients',icon:'♧',path:'patients'},{label:'Rendez-vous',icon:'▦',path:'rendez-vous'},{label:'Notifications',icon:'♧',path:'notifications'},{label:'Journal d’activité',icon:'◷',path:'activite'},{label:'Profil administrateur',icon:'○',path:'profil'},{label:'Sécurité',icon:'⚙',path:'parametres'},{label:'Spécialités',icon:'✚',path:'specialties'}];
  get items() { return this.role==='ADMIN' ? this.adminItems : this.role==='DOCTOR' ? this.doctorItems : this.patientItems; }
  get pageTitle() { if(this.role==='DOCTOR'){if(this.area==='disponibilites')return 'Mes disponibilités';if(!this.area||this.area==='dashboard')return 'Tableau de bord';}return this.area==='dashboard' ? 'Vue d’ensemble' : this.items.find(x=>x.path===this.area)?.label ?? 'Vue d’ensemble'; }
  get dashboardCards(){if(this.role==='DOCTOR')return[{label:'En attente',value:this.dashboard?.pendingCount,hint:'Demandes à traiter'},{label:'Confirmés',value:this.dashboard?.confirmedCount,hint:'Rendez-vous prévus'},{label:'Terminés',value:this.dashboard?.completedCount,hint:'Consultations terminées'},{label:'Aujourd’hui',value:this.dashboard?.todayCount,hint:'Rendez-vous du jour'},{label:'Notifications non lues',value:this.dashboard?.unreadNotificationCount,hint:'À consulter'}];return[{label:'Patients',value:this.dashboard?.patientsCount,hint:'Comptes enregistrés'},{label:'Médecins',value:this.dashboard?.doctorsCount,hint:'Comptes professionnels'},{label:'Approbations en attente',value:this.dashboard?.pendingDoctorApprovals,hint:'Demandes à examiner'},{label:'Rendez-vous',value:this.dashboard?.appointments?.total,hint:'Total actuel'},{label:'Confirmés',value:this.dashboard?.appointments?.confirmed,hint:'Statut actuel'},{label:'En attente',value:this.dashboard?.appointments?.pending,hint:'Statut actuel'},{label:'Annulés',value:this.dashboard?.appointments?.cancelled,hint:'Statut actuel'}].filter(card=>card.value!==undefined);}
  chartHeight(value:number){const counts=(this.dashboard?.appointmentsByMonth??[]).map((m:any)=>Number(m.total)||0);const max=Math.max(1,...counts);return Math.max(4,Math.round((Number(value||0)/max)*100));}
  get description() { if(this.role==='DOCTOR'&&this.area==='profil')return 'Gérez vos informations personnelles, votre cabinet et la sécurité de votre compte.';if(this.role==='DOCTOR'&&this.area==='disponibilites')return 'Définissez les horaires de consultation et organisez votre semaine de travail.';const map:Record<string,string>={ '':'Voici les informations récentes de votre espace.', medecins:'Recherchez un professionnel selon vos besoins.', approbations:'Examinez les demandes des médecins inscrits.', patients:'Consultez les comptes patients et leur activité de rendez-vous.', 'rendez-vous':'Consultez et gérez les rendez-vous autorisés.', historique:'Retrouvez le suivi et l’historique de vos rendez-vous.', disponibilites:'Organisez votre semaine de consultation en un coup d’œil.', absences:'Déclarez une absence et organisez les rendez-vous concernés.', calendrier:'Visualisez votre agenda de consultation.', activite:'Consultez les actions administratives consignées.', notifications:'Retrouvez vos notifications Hello Doc.', profil:'Votre photo, vos informations et la sécurité de votre compte, au même endroit.', parametres:'Protégez l’accès à votre compte.', avis:'Consultez les avis liés à votre activité.', }; return map[this.area] ?? map['']; }
  get actionLabel() { if(this.role==='ADMIN'&&['medecins','patients','approbations','rendez-vous','notifications','activite'].includes(this.area))return '';return this.area==='medecins' ? '' : this.area==='disponibilites' ? '' : this.area==='absences' ? 'Déclarer une absence' : this.area==='rendez-vous' ? 'Actualiser' : ''; }
  get emptyIcon() { return this.area==='approbations' ? '✓' : this.area==='medecins' ? '⌕' : '▦'; }
  get emptyTitle() { return this.area==='approbations' ? 'Aucune demande à afficher' : this.area==='medecins' ? 'Trouvez le professionnel qui vous convient' : 'Vos informations apparaîtront ici'; }
  get emptyText() { return this.area==='approbations' ? 'Aucune demande en attente.' : this.area==='medecins' ? 'Aucun profil médecin ne correspond à cette vue.' : this.area==='patients' ? 'Aucun compte patient trouvé.' : 'Aucune donnée disponible.'; }
  get hasData() { return this.records.length > 0; }
  readonly accountStatuses=[['ACTIVE','Actif'],['INACTIVE','Inactif']];
  readonly appointmentStatuses=[['PENDING','En attente'],['CONFIRMED','Confirmé'],['TO_RESCHEDULE','À reprogrammer'],['COMPLETED','Terminé'],['CANCELLED','Annulé'],['REJECTED','Refusé'],['NO_SHOW','Absent']];
  get statusOptions(){return this.role==='ADMIN'&&['medecins','patients'].includes(this.area)?this.accountStatuses:this.appointmentStatuses;}
  get patientStatusOptions() {
    return this.appointmentStatuses.filter(([status]) => this.area === 'historique'
      ? status !== 'TO_RESCHEDULE'
      : ['PENDING', 'CONFIRMED', 'TO_RESCHEDULE'].includes(status));
  }
  get patientPasswordMismatch() {
    const { newPassword, confirmPassword } = this.passwordForm.controls;
    return confirmPassword.touched && !!confirmPassword.value && confirmPassword.value !== newPassword.value;
  }
  modeLabel(mode?: ConsultationMode) { return mode === 'VIDEO' ? 'Téléconsultation' : 'Au cabinet'; }
  patientHistoryLabel(appointment: { status: string }) {
    if (this.area === 'historique') {
      if (appointment.status === 'CONFIRMED') return 'Passé · confirmé';
      if (appointment.status === 'PENDING') return 'Passé · en attente';
    }
    return this.statusLabel(appointment.status);
  }
  notificationIcon(type: string) {
    if (type === 'FOLLOW_UP_RECOMMENDED') return 'dossier-medical';
    if (type === 'APPOINTMENT_CONFIRMED' || type === 'APPOINTMENT_COMPLETED') return 'approbations';
    if (['APPOINTMENT_CANCELLED', 'APPOINTMENT_REJECTED', 'APPOINTMENT_NO_SHOW'].includes(type)) return 'absences';
    if (type?.startsWith('APPOINTMENT_')) return 'rendez-vous';
    return 'notifications';
  }
  invalidField(field: AbstractControl) { return field.touched && field.invalid; }
  selectAccountTab(tab: PatientAccountTab) {
    if (this.patientAccountTab === tab || this.busy) return;
    this.patientAccountTab = tab;
    this.profileForm.controls.currentPassword.reset();
    this.passwordForm.reset();
    this.error = '';
    this.notice = '';
  }
  moveAccountTab(event: KeyboardEvent) {
    if (this.busy) return;
    const current = this.patientAccountTabs.findIndex(tab => tab.id === this.patientAccountTab);
    const last = this.patientAccountTabs.length - 1;
    const positions: Record<string, number> = { Home: 0, End: last, ArrowLeft: current > 0 ? current - 1 : last, ArrowRight: current < last ? current + 1 : 0 };
    const next = positions[event.key];
    if (next === undefined) return;
    event.preventDefault();
    const tab = this.patientAccountTabs[next];
    this.selectAccountTab(tab.id);
    document.getElementById(`patient-tab-${tab.id}`)?.focus();
  }
  applyPatientFilters() {
    this.patientFilters = { q: (this.search.value ?? '').trim(), status: this.statusFilter.value ?? '' };
    this.page = 0;
    this.load();
  }
  resetPatientFilters() {
    this.search.reset('');
    this.statusFilter.reset('');
    this.applyPatientFilters();
  }
  get hasStatusFilter(){return ['rendez-vous','historique'].includes(this.area)||(this.role==='ADMIN'&&['medecins','patients'].includes(this.area));}
  get profileError(){return this.profileForm.controls.phone.touched&&this.profileForm.controls.phone.invalid?'Le téléphone doit contenir entre 8 et 15 chiffres.':'';}
  get embeddedArea(){return (this.role==='DOCTOR'&&['patients','profil','disponibilites'].includes(this.area))||(this.role==='ADMIN'&&['rendez-vous','activite','notifications'].includes(this.area))||(this.role==='PATIENT'&&this.area==='medecins');}
  get listArea(){return !this.embeddedArea&&!['calendrier','profil','parametres','disponibilites','specialties'].includes(this.area)&&!this.unavailableMessage&&!this.dashboard;}
  get filteredRecords() { const q=(this.search.value??'').toLowerCase(), s=this.statusFilter.value; return this.records.filter(r=>(!s||r.status===s)&&(!q||JSON.stringify(r).toLowerCase().includes(q))); }
  weekdayNames = ['Lun.','Mar.','Mer.','Jeu.','Ven.','Sam.','Dim.'];
  get monthLabel() { return new Intl.DateTimeFormat('fr-FR',{month:'long',year:'numeric'}).format(this.monthCursor); }
  get leadingDays() { return Array.from({length:(this.monthCursor.getDay()+6)%7},(_,index)=>index); }
  get monthDays() { const count=new Date(this.monthCursor.getFullYear(),this.monthCursor.getMonth()+1,0).getDate();return Array.from({length:count},(_,index)=>{const date=new Date(this.monthCursor.getFullYear(),this.monthCursor.getMonth(),index+1);return{number:index+1,key:this.dateKey(date)};}); }
  dateKey(date:Date) { const year=date.getFullYear(),month=String(date.getMonth()+1).padStart(2,'0'),day=String(date.getDate()).padStart(2,'0');return `${year}-${month}-${day}`; }
  appointmentsOn(day:string) { return this.records.filter(record=>record.startsAt&&this.dateKey(new Date(record.startsAt))===day); }
  changeMonth(offset:number) { this.monthCursor=new Date(this.monthCursor.getFullYear(),this.monthCursor.getMonth()+offset,1);this.load(); }
  goToToday() { this.monthCursor=new Date(new Date().getFullYear(),new Date().getMonth(),1);this.load(); }
  ngOnInit() {
    if (this.role === 'DOCTOR') {
      this.api.get('/doctors/me').pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: profile => this.setupProfile = profile,
        error: () => { this.setupProfile = null; }
      });
    }
    this.route.paramMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(params => {
      this.area = params.get('area') ?? '';
      this.menuOpen = false;
      this.title.setTitle(`${this.pageTitle} | Hello Doc`);
      this.page = 0;
      this.pendingAbsence = null;
      this.editingAbsenceId = null;
      this.editingAvailabilityId = null;
      this.absenceForm.reset({ type: 'FULL_DAY' });
      this.records = [];
      this.dashboard = null;
      this.error = '';
      this.notice = '';
      this.expandedRecordId = null;
      this.patientFilters = { q: '', status: '' };
      this.patientAccountTab = 'photo';
      this.profileForm.controls.currentPassword.reset();
      this.passwordForm.reset();
      this.search.reset('', { emitEvent: false });
      this.statusFilter.reset('', { emitEvent: false });
      this.load();
    });
  }
  endpoint() { if(this.role==='ADMIN') return this.area==='approbations'?'/admin/doctors?status=PENDING':this.area==='medecins'?'/admin/doctors/directory':this.area==='patients'?'/admin/patients':this.area==='rendez-vous'?'/admin/appointments':this.area==='notifications'?'/admin/notifications':this.area==='activite'?'/admin/activity-logs':this.area==='profil'?'/admin/me':this.area==='parametres'?'/auth/me':this.area===''||this.area==='dashboard'?'/admin/overview':`/admin/${this.area}`; if(this.role==='DOCTOR') return this.area==='rendez-vous'||this.area==='calendrier'?'/doctors/me/appointments':this.area==='patients'?'/doctors/me/patients':this.area==='disponibilites'?'/doctors/me/availabilities':this.area==='absences'?'/doctors/me/absences':this.area==='avis'?'/doctors/me/reviews':this.area==='notifications'?'/notifications':this.area==='profil'?'/doctors/me':this.area==='parametres'?'/doctors/me/preferences':`/doctors/me/dashboard`; return this.area==='rendez-vous'||this.area==='historique'?'/appointments/mine':this.area==='notifications'?'/notifications':this.area==='profil'?'/patients/me':'/patients/me/dashboard'; }
  load() { this.loadRequest?.unsubscribe();this.totalElements=0;this.totalPages=0;this.loading=true; this.error=''; this.errorTitle='Impossible de charger les données'; this.dashboard=null;this.profile=null;this.unavailableMessage='';if(this.embeddedArea||this.area==='parametres'||this.area==='calendrier'||(this.role==='ADMIN'&&this.area==='specialties')){this.records=[];this.loading=false;return;}const path=this.endpoint();const patientAppointments = this.role === 'PATIENT' && ['rendez-vous', 'historique'].includes(this.area);const query = {page:this.page,size:this.pageSize,...(patientAppointments ? { ...this.patientFilters, history: String(this.area === 'historique') } : {})}; this.loadRequest=this.api.get<any>(path,query).subscribe({next:data=>{this.loading=false;this.dashboard=(this.area===''||this.area==='dashboard')&&data&&typeof data==='object'&&!Array.isArray(data)?data:null;this.profile=(this.area==='profil'||(this.role==='ADMIN'&&this.area==='parametres'))&&data&&!Array.isArray(data)?data:null;if(this.profile){if(this.role==='DOCTOR')this.setupProfile=data;this.profileForm.patchValue({firstName:data.firstName??'',lastName:data.lastName??'',phone:data.phone??'',address:data.address??'',serviceTitle:data.serviceTitle??'',specialty:data.specialty??'',durationMinutes:data.durationMinutes??30,workStart:data.workStart??'08:00',workEnd:data.workEnd??'17:00',pauseMinutes:data.pauseMinutes??0,location:data.location??data.address??'',description:data.description??''});this.profileDays=(data.workingDays??['1','2','3','4','5']).map(String);this.profileServices.clear();(data.services??[]).forEach((s:any)=>this.addProfileService(s));if(!this.profileServices.length)this.addProfileService();}this.records=Array.isArray(data)?data:Array.isArray(data?.content)?data.content:[];this.totalElements=data?.totalElements??this.records.length;this.totalPages=data?.totalPages??(this.records.length?1:0);},error:e=>{this.loading=false;this.error=e.message;this.records=[];this.dashboard=null;this.profile=null;}}); }
  primaryAction() { if(this.area==='rendez-vous'){this.load();return;} if(this.area==='disponibilites'||this.area==='absences'){document.querySelector('.workspace-form')?.scrollIntoView({behavior:'smooth',block:'center'});return;} }
  changePage(offset: number) {
    const next = this.page + offset;
    if (next < 0 || next >= this.totalPages || this.loading) return;
    this.page = next;
    if (this.role !== 'PATIENT') {
      this.search.setValue('');
      this.statusFilter.setValue('');
    }
    this.load();
  }
  editAvailability(record:any){this.editingAvailabilityId=record.id;this.availabilityForm.patchValue({isoWeekday:String(record.isoWeekday),start:String(record.startTime).slice(0,5),end:String(record.endTime).slice(0,5)});document.querySelector('.workspace-form')?.scrollIntoView({behavior:'smooth',block:'center'});}
  setAbsencePreset(preset:string){if(preset==='MORNING')this.absenceForm.patchValue({type:'PARTIAL',endDate:this.absenceForm.value.startDate,startTime:'00:00',endTime:'12:00'});if(preset==='AFTERNOON')this.absenceForm.patchValue({type:'PARTIAL',endDate:this.absenceForm.value.startDate,startTime:'12:00',endTime:'23:55'});}
  declareAbsence() { this.absenceForm.markAllAsTouched();const v=this.absenceForm.getRawValue();if(!v.startDate||!v.reason){this.error='Renseignez une date et un motif pour déclarer l’absence.';return;}if(v.type==='FULL_DAY'&&(!v.endDate||v.endDate<v.startDate)){this.error='Choisissez une date de fin égale ou postérieure à la date de début.';return;}if(v.type==='PARTIAL'&&(!v.startTime||!v.endTime||v.endTime<=v.startTime)){this.error='Pour une absence partielle, renseignez une heure de fin après le début.';return;}this.busy=true;this.error='';const partial=v.type==='PARTIAL';const body={startDate:v.startDate,endDate:partial?v.startDate:v.endDate,startTime:partial?v.startTime:null,endTime:partial?v.endTime:null,reason:v.reason,comment:v.comment};this.api.post<{affectedAppointments:number}>('/doctors/me/absences/preview',body).subscribe({next:impact=>{this.busy=false;this.absenceImpact=impact.affectedAppointments;this.pendingAbsence=body;setTimeout(()=>{const panel=document.querySelector<HTMLElement>('.absence-confirmation');panel?.scrollIntoView({block:'center'});panel?.focus();});},error:e=>{this.busy=false;this.error=e.message;}}); }
  confirmAbsence(){if(!this.pendingAbsence||this.busy)return;this.busy=true;const body={...this.pendingAbsence,confirmRescheduling:true};const request=this.editingAbsenceId?this.api.put(`/doctors/me/absences/${this.editingAbsenceId}`,body):this.api.post('/doctors/me/absences',body);request.subscribe({next:()=>{this.busy=false;this.notice=this.absenceImpact?'Absence enregistrée. Les patients concernés sont invités à reprogrammer leur rendez-vous.':'Absence enregistrée.';this.pendingAbsence=null;this.editingAbsenceId=null;this.absenceForm.reset({type:'FULL_DAY'});this.load();},error:e=>{this.busy=false;this.error=e.message;}}); }
  editAbsence(record:any){this.editingAbsenceId=record.id;this.absenceForm.patchValue({type:record.startTime?'PARTIAL':'FULL_DAY',startDate:record.startDate,endDate:record.endDate,startTime:record.startTime?.slice(0,5)??'',endTime:record.endTime?.slice(0,5)??'',reason:record.reason,comment:record.comment??''});document.querySelector('.workspace-form')?.scrollIntoView({behavior:'smooth',block:'center'});}
  weekdayLabel(iso:number){return this.profileDayOptions.find(x=>Number(x.value)===Number(iso))?.label??`Jour ${iso}`;}
  openRecord(record:any) { if(this.role==='ADMIN'&&this.area==='medecins'){if(record.id)void this.router.navigate(['/admin/medecins',record.id]);return;}if(this.role==='ADMIN'&&['patients','approbations'].includes(this.area)){this.expandedRecordId=this.expandedRecordId===record.id?null:record.id;return;}if(!record.id){this.notice='Les détails de cet élément ne sont pas disponibles.';return;}if(['rendez-vous','historique'].includes(this.area)){void this.router.navigate([this.base,'rendez-vous',record.id]);return;}if(this.role==='DOCTOR'&&this.area==='disponibilites'){this.editAvailability(record);return;}if(this.role==='DOCTOR'&&this.area==='absences'){this.editAbsence(record);return;}this.notice='Les détails de cet élément sont fournis par le service Hello Doc.'; }
  doctorDocumentUrl(record:any){return `${environment.apiBaseUrl}/admin/doctors/${record.id}/document`;}
  decide(record:any, approved:boolean) {
    if(this.busy||!record.id)return;
    if(!approved){this.refusalCandidate=record;this.refusalReason='';this.refusalError='';this.refusalDialog?.nativeElement.showModal();return;}
    this.sendDecision(record,true);
  }
  private sendDecision(record:any,approved:boolean,reason='') {
    if(!record.id||this.busy)return;
    this.loadRequest?.unsubscribe();this.loading=false;
    this.busy=true;this.error='';this.notice='';this.approvalOutcome=null;
    this.errorTitle=approved?'Approbation impossible':'Refus impossible';
    this.api.post(`/admin/doctors/${record.id}/${approved?'approve':'reject'}`,approved?{}:{reason}).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next:()=>{
        this.busy=false;
        this.records=this.records.filter(item=>item.id!==record.id);
        this.totalElements=Math.max(0,this.totalElements-1);
        if(!approved)this.closeRefusal();
        this.notice=approved?'Demande approuvée. Le compte médecin est actif.':'Demande refusée.';
        if(approved)this.approvalOutcome={applicationId:record.id,name:record.fullName||record.name||'Médecin'};
      },
      error:e=>{
        this.busy=false;
        if(e.status===0||e.status>=500){this.errorTitle='Décision à vérifier';this.error='La réponse du serveur n’a pas pu être confirmée. Actualisez la liste avant de réessayer : la décision peut déjà être enregistrée.';}
        else this.error=e.message||'Impossible d’enregistrer cette décision.';
        if(!approved)this.refusalError=this.error;
      }
    });
  }
  deleteAccount(record:any) { if(this.busy||!record.id)return;const kind=this.area==='patients'?'patient':'médecin';const label=record.fullName||record.name||record.title||record.email||kind;if(!window.confirm(`Supprimer définitivement le compte de ${label} et ses données de profil ? Cette action est irréversible.`))return;const collection=this.area==='patients'?'patients':'doctors';this.busy=true;this.error='';this.errorTitle='Suppression impossible';this.api.delete(`/admin/${collection}/${record.id}`).subscribe({next:()=>{this.busy=false;this.notice=`Le compte ${kind} a été supprimé.`;this.load();},error:e=>{this.busy=false;this.error=e.message||`Impossible de supprimer ce compte ${kind}.`;}}); }
  setActive(record:any,active:boolean) { const collection=this.area==='patients'?'patients':'doctors';if(this.busy||!record.id)return;if(!active&&!window.confirm('Désactiver ce compte et fermer ses sessions actives ?'))return;this.busy=true;this.api.patch(`/admin/${collection}/${record.id}/status`,{status:active?'ACTIVE':'INACTIVE'}).subscribe({next:()=>{this.busy=false;this.notice=active?'Compte activé.':'Compte désactivé.';this.load();},error:e=>{this.busy=false;this.error=e.message;}}); }
  appointmentPermissions(record:any){return appointmentPermissions(this.role,record);}
  canMarkNoShow(record:any){return this.appointmentPermissions(record).canMarkNoShow;}
  canMarkCompleted(record:any){return record.consultationMode!=='VIDEO'&&this.appointmentPermissions(record).canMarkCompleted;}
  appointmentAction(record:any,status:string) {
    if(!record.id||this.busy)return;
    const actions=this.appointmentPermissions(record);
    const allowed=status==='CONFIRMED'?actions.canConfirm:status==='REJECTED'?actions.canReject:status==='CANCELLED'?actions.canCancel:status==='COMPLETED'?actions.canMarkCompleted:status==='NO_SHOW'?actions.canMarkNoShow:false;
    if(!allowed)return;
    this.busy=true;this.error='';
    const request=status==='CANCELLED'?this.api.post(`/appointments/${record.id}/cancellation`,{reason:'Annulé par le médecin'}):this.api.patch(`/appointments/${record.id}/status`,{status});
    request.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({next:()=>{this.busy=false;this.notice=status==='CONFIRMED'?'Rendez-vous confirmé.':status==='COMPLETED'?'Rendez-vous marqué comme terminé.':status==='REJECTED'?'Demande refusée.':status==='NO_SHOW'?'Absence enregistrée.':'Rendez-vous annulé.';this.load();},error:e=>{this.busy=false;this.error=e.message;}});
  }
  cancelAppointment(record: { id: string; status?: string; startsAt?: string; endsAt?: string }) {
    if (this.busy || !record.id || !this.appointmentPermissions(record).canCancel || !window.confirm('Confirmer l’annulation de ce rendez-vous ?')) return;
    this.busy = true;
    this.error = '';
    this.notice = '';
    this.api.post(`/appointments/${record.id}/cancellation`, { reason: 'Annulation demandée par le patient' })
      .pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: () => { this.busy = false; this.notice = 'Rendez-vous annulé.'; this.load(); },
        error: e => { this.busy = false; this.error = e.message; }
      });
  }
  saveProfile(){if(this.busy)return;this.notice='';this.error='';this.profileForm.markAllAsTouched();if(this.profileForm.invalid){this.error='Vérifiez les champs obligatoires et confirmez avec votre mot de passe actuel.';document.querySelector<HTMLElement>('.patient-account-form input.ng-invalid, .patient-account-form .ng-invalid input, .profile-editor [formControlName].ng-invalid')?.focus();return;}const v=this.profileForm.getRawValue();const services=(v.services as {name:string|null;price:string|null}[]).filter(s=>s.name?.trim()&&s.price?.trim()).map(s=>({name:s.name!.trim(),price:s.price!.trim()}));if(this.role==='DOCTOR'&&!services.length){this.error='Ajoutez au moins un service avec son tarif.';return;}const body=this.role==='DOCTOR'?{firstName:v.firstName,lastName:v.lastName,phone:v.phone,address:v.address,serviceTitle:v.serviceTitle,specialty:v.specialty,location:v.location,description:v.description,services,currentPassword:v.currentPassword}:{firstName:v.firstName,lastName:v.lastName,phone:v.phone,address:v.address,currentPassword:v.currentPassword};this.busy=true;this.error='';const url=this.role==='DOCTOR'?'/doctors/me':this.role==='ADMIN'?'/admin/me':'/patients/me';this.api.put(url,body).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({next:data=>{this.busy=false;this.notice='Profil mis à jour.';this.auth.me().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({ error: () => {} });this.profileForm.controls.currentPassword.reset();if(this.role==='PATIENT')this.profile=data;else this.load();},error:e=>{this.busy=false;this.error=e.message;}});}
  changePassword(){if(this.busy)return;this.notice='';this.error='';this.passwordForm.markAllAsTouched();const v=this.passwordForm.getRawValue();if(this.passwordForm.invalid){this.error='Vérifiez les champs avant de modifier votre mot de passe.';return;}if(v.newPassword!==v.confirmPassword){this.error='Les nouveaux mots de passe ne correspondent pas.';return;}this.busy=true;this.api.post('/auth/password',{currentPassword:v.currentPassword,newPassword:v.newPassword}).subscribe({next:()=>{this.busy=false;this.notice='Mot de passe modifié.';this.passwordForm.reset();},error:e=>{this.busy=false;this.error=e.message;}});}
  updateSlot(slot:any,action:'delete'){if(!slot.id)return;this.api.delete(`/doctors/me/availabilities/${slot.id}`).subscribe({next:()=>{this.notice='Disponibilité supprimée.';this.load();},error:e=>this.error=e.message});}
  markRead(record: { id: string; readAt?: string }) {
    if (!record.id || record.readAt || this.readingNotifications.has(record.id)) return;
    this.readingNotifications.add(record.id);
    this.error = '';
    this.api.patch<{ readAt: string }>(`/notifications/${record.id}/read`).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: updated => {
        this.readingNotifications.delete(record.id);
        this.records = this.records.map(row => row.id === record.id ? { ...row, ...updated } : row);
        this.notificationCounter?.changed.next();
      },
      error: e => {
        this.readingNotifications.delete(record.id);
        if (this.area === 'notifications') this.error = e.message;
      }
    });
  }
  deleteAbsence(record:any){if(!record.id||!window.confirm('Retirer cette absence ?'))return;this.api.delete(`/doctors/me/absences/${record.id}`).subscribe({next:()=>{this.notice='Absence retirée.';this.load();},error:e=>this.error=e.message});}
  statusClass(s:string) { return ['APPROVED','ACTIVE','CONFIRMED','COMPLETED','AVAILABLE'].includes(s)?'positive':['PENDING','TO_RESCHEDULE','RESCHEDULED'].includes(s)?'pending':['REJECTED','INACTIVE','CANCELLED','NO_SHOW','BLOCKED'].includes(s)?'negative':'neutral'; }
  statusLabel(s:string) { return ({APPROVED:'Approuvé',ACTIVE:'Actif',CONFIRMED:'Confirmé',PENDING:'En attente',REJECTED:'Refusé',NO_SHOW:'Patient absent',INACTIVE:'Inactif',CANCELLED:'Annulé',COMPLETED:'Terminé',REVIEW:'Avis',TO_RESCHEDULE:'À reprogrammer',AVAILABLE:'Disponible',BLOCKED:'Bloqué'} as Record<string,string>)[s]??s??'Information'; }
  logout() { if(this.busy||this.editorSaving)return;if(this.hasUnsavedDoctorChanges()&&!window.confirm('Vous avez des modifications non enregistrées. Se déconnecter sans les enregistrer ?'))return;this.busy=true;this.auth.logout().subscribe({error:()=>{this.busy=false;this.error='La déconnexion n’a pas pu être terminée. Réessayez.';}}); }
}
