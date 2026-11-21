import { Pipe, PipeTransform } from '@angular/core';
export function cabinetDate(value:string|Date|null|undefined,format:'date'|'time'|'datetime'|'day'='date'):string {
 if(!value)return '—';
 // Calendar-only dates must not move to the preceding day in another time zone.
 const date=value instanceof Date?value:new Date(/^\d{4}-\d{2}-\d{2}$/.test(value)?`${value}T12:00:00+01:00`:value);
 if(!Number.isFinite(date.getTime()))return '—';
 const options:Intl.DateTimeFormatOptions=format==='time'?{hour:'2-digit',minute:'2-digit'}:format==='datetime'?{dateStyle:'medium',timeStyle:'short'}:format==='day'?{weekday:'long',day:'numeric',month:'long',year:'numeric'}:{dateStyle:'long'};
 return new Intl.DateTimeFormat('fr-FR',{...options,timeZone:'Africa/Tunis',hourCycle:'h23'}).format(date);
}
@Pipe({name:'cabinetDate',standalone:true})
export class CabinetDatePipe implements PipeTransform {transform(value:string|Date|null|undefined,format:'date'|'time'|'datetime'|'day'='date'){return cabinetDate(value,format);}}
