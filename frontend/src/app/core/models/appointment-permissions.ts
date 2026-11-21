interface AppointmentState {
  status?: string;
  startsAt?: string;
  endsAt?: string;
}

/** Actions available to each participant at the current stage of an appointment. */
export function appointmentPermissions(role: string, appointment: AppointmentState|null|undefined, now=Date.now()) {
  const doctor=role==='DOCTOR';
  const patient=role==='PATIENT'||role==='USER';
  const status=appointment?.status;
  const start=Date.parse(appointment?.startsAt??'');
  const end=Date.parse(appointment?.endsAt??'');
  const future=Number.isFinite(start)&&start>now;
  const patientMayChange=['PENDING','CONFIRMED','TO_RESCHEDULE'].includes(status??'');
  return {
    canConfirm:doctor&&status==='PENDING'&&future,
    canReject:doctor&&status==='PENDING',
    canCancel:doctor?status==='CONFIRMED'&&future:patient&&patientMayChange&&(status==='TO_RESCHEDULE'||future),
    canReschedule:patient&&patientMayChange&&(status==='TO_RESCHEDULE'||future),
    canMarkCompleted:doctor&&status==='CONFIRMED'&&Number.isFinite(end)&&end<=now,
    canMarkNoShow:doctor&&status==='CONFIRMED'&&Number.isFinite(start)&&start<=now
  };
}
