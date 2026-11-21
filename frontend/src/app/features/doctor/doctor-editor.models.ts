export interface DoctorService { name: string; price: string; }
export interface DoctorProfile {
  id: string; firstName: string; lastName: string; fullName: string; email: string;
  phone: string; address: string; serviceTitle: string; specialty: string;
  location: string; description: string; services: DoctorService[];
  durationMinutes: number; pauseMinutes: number; photoUrl: string | null;
}
export interface ScheduleWindow { period: number; startTime: string; endTime: string; }
export interface ScheduleDay { isoWeekday: number; label: string; active: boolean; windows: ScheduleWindow[]; }
export interface AvailabilityRow extends ScheduleWindow { isoWeekday: number; }
export interface ScheduleDraft { slotDurationMinutes: number; pauseMinutes: number; days: ScheduleDay[]; }
export const weekdayLabels = ['Lundi','Mardi','Mercredi','Jeudi','Vendredi','Samedi','Dimanche'];

export function scheduleFromApi(rows: AvailabilityRow[], profile: Pick<DoctorProfile,'durationMinutes'|'pauseMinutes'>): ScheduleDraft {
  return {slotDurationMinutes: profile.durationMinutes, pauseMinutes: profile.pauseMinutes,
    days: weekdayLabels.map((label,i) => {
      const windows=rows.filter(row=>row.isoWeekday===i+1).sort((a,b)=>a.period-b.period)
        .map(row=>({period:row.period,startTime:row.startTime.slice(0,5),endTime:row.endTime.slice(0,5)}));
      return {isoWeekday:i+1,label,active:windows.length>0,windows};
    })};
}
export function schedulePayload(draft: ScheduleDraft) {
  return {slotDurationMinutes:Number(draft.slotDurationMinutes),pauseMinutes:Number(draft.pauseMinutes),
    windows:draft.days.filter(day=>day.active).flatMap(day=>day.windows.map(window=>({isoWeekday:day.isoWeekday,...window})))};
}
export function minutes(time: string): number {
  if(!/^([01]\d|2[0-3]):[0-5]\d$/.test(time)) return NaN;
  return Number(time.slice(0,2))*60+Number(time.slice(3));
}
/** Mirrors the existing weekly endpoint, including its strict separation of periods. */
export function scheduleErrors(draft: ScheduleDraft): Record<string,string> {
  const errors:Record<string,string>={};
  const duration=Number(draft.slotDurationMinutes),pause=Number(draft.pauseMinutes);
  if(!Number.isInteger(duration)||duration<5||duration>240||duration%5) errors['duration']='Choisissez une durée de 5 à 240 minutes, par pas de 5.';
  if(!Number.isInteger(pause)||pause<0||pause>240) errors['pause']='Indiquez une pause entière de 0 à 240 minutes.';
  for(const day of draft.days.filter(d=>d.active)) {
    if(day.windows.length>2||new Set(day.windows.map(w=>w.period)).size!==day.windows.length) errors[`day-${day.isoWeekday}`]='Deux plages distinctes au maximum sont autorisées par jour.';
    if(!day.windows.length) errors[`day-${day.isoWeekday}`]='Ajoutez au moins une plage ou désactivez cette journée.';
    for(const window of day.windows) {
      const start=minutes(window.startTime),end=minutes(window.endTime),key=`${day.isoWeekday}-${window.period}`;
      if(!Number.isFinite(start)||!Number.isFinite(end)) errors[key]='Renseignez une heure de début et de fin.';
      else if(start%5||end%5) errors[key]='Les heures doivent être réglées par pas de 5 minutes.';
      else if(end<=start) errors[key]='L’heure de fin doit être postérieure au début.';
      else if(end-start<duration) errors[key]=`Cette plage doit contenir au moins ${duration} minutes.`;
    }
    const first=day.windows.find(w=>w.period===1),second=day.windows.find(w=>w.period===2);
    if(first&&second&&minutes(second.startTime)<=minutes(first.endTime)) errors[`${day.isoWeekday}-2`]='La deuxième plage doit commencer après la fin de la première, sans chevauchement.';
  }
  return errors;
}
export function copySchedule(source: ScheduleDay, target: ScheduleDay) {
  target.active=source.active;
  target.windows=source.windows.map(window=>({...window}));
}
/** Prices remain exactly as stored; no currency or amount is invented or normalized. */
export function servicePrice(price:string): {amount:string;currency:string} {
  const match=/^(\d+(?:[.,]\d+)?)\s+([^\d]+)$/.exec(price.trim());
  return match?{amount:match[1],currency:match[2]}:{amount:price,currency:'—'};
}
