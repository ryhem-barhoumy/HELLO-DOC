// Real browser workflows against verify-workflows.py --hold, never the usual database.
import assert from 'node:assert/strict';
import {createRequire} from 'node:module';
import {mkdir} from 'node:fs/promises';
import path from 'node:path';
if(process.env.HELLODOC_QA_BROWSER!=='1')throw new Error('HELLODOC_QA_BROWSER=1 and the disposable QA fixture are required.');
const require=createRequire(import.meta.url);
const {chromium}=require(process.env.HELLODOC_PLAYWRIGHT_PATH||'playwright');
const base='http://127.0.0.1:4201';
const output=path.resolve('../output/doctor-interfaces-2026-11-21');await mkdir(output,{recursive:true});
const browser=await chromium.launch({headless:true,executablePath:process.env.HELLODOC_CHROME_PATH||undefined});
const context=await browser.newContext({viewport:{width:1440,height:1000},locale:'fr-FR'});
let page=await context.newPage();page.setDefaultTimeout(15000);
const jsErrors=[];page.on('pageerror',e=>jsErrors.push(e.message));
const PASSWORD='OnlyForHelloDocQA2026';const NEW_PASSWORD='ChangedForHelloDocQA2026';
async function csrf(ctx=context){await ctx.request.get(base+'/api/auth/csrf');const cookie=(await ctx.cookies()).find(c=>c.name==='XSRF-TOKEN');assert.ok(cookie);return {'X-XSRF-TOKEN':decodeURIComponent(cookie.value)};}
async function get(url){const r=await context.request.get(base+'/api'+url);assert.ok(r.ok(),`${url}: HTTP ${r.status()}`);return r.json();}
async function login(ctx,password){const r=await ctx.request.post(base+'/api/auth/login',{headers:await csrf(ctx),data:{email:'doctor@qa.example',password}});return r;}
async function ready(selector){await page.locator(selector).waitFor({state:'visible'});}
async function saved(text){await page.getByRole('status').filter({hasText:text}).waitFor();}
async function profileSave(){await page.locator('#profile-confirm-password').fill(PASSWORD);await page.getByRole('button',{name:'Enregistrer les modifications',exact:true}).click();await saved('Vos modifications ont été enregistrées.');}
async function selectTab(name){await page.getByRole('tab',{name,exact:true}).click();}
async function assertFits(label){const overflow=await page.evaluate(()=>({width:innerWidth,document:document.documentElement.scrollWidth,body:document.body.scrollWidth}));assert.ok(overflow.document<=overflow.width+1&&overflow.body<=overflow.width+1,`${label}: horizontal overflow ${JSON.stringify(overflow)}`);const tiny=await page.locator('.doctor-ui input:not([type=checkbox]),.doctor-ui select,.doctor-ui textarea').evaluateAll(nodes=>nodes.filter(n=>n.getBoundingClientRect().width>0&&n.getBoundingClientRect().height<43).map(n=>n.id));assert.deepEqual(tiny,[],`${label}: undersized fields`);}
const weekdays=['Lundi','Mardi','Mercredi','Jeudi','Vendredi','Samedi','Dimanche'];
try{
 assert.equal((await login(context,PASSWORD)).status(),200);
 const initial=await get('/doctors/me');const beforeAppointments=(await get('/doctors/me/appointments?size=100')).content;
 const signatures=list=>list.map(a=>({id:a.id,startsAt:a.startsAt,endsAt:a.endsAt,status:a.status,servicePrice:a.servicePrice})).sort((a,b)=>a.id.localeCompare(b.id));
 await page.goto(base+'/medecin/profil');await ready('#profile-firstName');
 await page.getByRole('button',{name:'Modifier la photo',exact:true}).click();
 const image=await page.evaluate(()=>{const canvas=document.createElement('canvas');canvas.width=8;canvas.height=8;const ctx=canvas.getContext('2d');ctx.fillStyle='#167e6d';ctx.fillRect(0,0,8,8);return canvas.toDataURL('image/png').split(',')[1];});
 await page.locator('#doctor-photo-editor input[type=file]').setInputFiles({name:'qa-photo.png',mimeType:'image/png',buffer:Buffer.from(image,'base64')});
 await page.getByRole('button',{name:'Enregistrer la photo',exact:true}).click();await page.getByText('Votre photo a été enregistrée.',{exact:true}).waitFor();
 const withPhoto=await get('/doctors/me');assert.notEqual(withPhoto.photoUrl,initial.photoUrl);const publicPhoto=await get('/doctors/'+initial.id);assert.ok(publicPhoto.photoUrl);assert.equal((await context.request.get(base+publicPhoto.photoUrl)).status(),200);
 await page.getByRole('button',{name:'Fermer l’éditeur',exact:true}).click();
 console.log('PASS profile photo upload and public physician photo availability');
 assert.equal(await page.locator('#profile-email').getAttribute('readonly'),'');
 await page.locator('#profile-firstName').fill('Médecin Interface QA');
 page.once('dialog',dialog=>dialog.dismiss());
 await page.locator('.side-nav').getByRole('link',{name:'Disponibilités',exact:true}).click();
 assert.match(page.url(),/medecin\/profil$/);assert.equal(await page.locator('#profile-firstName').inputValue(),'Médecin Interface QA');
 await profileSave();assert.equal((await get('/doctors/me')).firstName,'Médecin Interface QA');
 assert.deepEqual((await get('/doctors/me')).services,initial.services);
 console.log('PASS real profile save, untouched service data and unsaved-navigation warning');
 await selectTab('Cabinet et services');
 await page.locator('#practice-name').fill('Cabinet HelloDoc QA');await page.locator('#practice-location').fill('Adresse du cabinet QA');await page.locator('#practice-description').fill('Présentation du cabinet enregistrée depuis le navigateur.');
 const originalService=initial.services[0];await page.getByRole('button',{name:'Modifier '+originalService.name,exact:true}).click();await ready('#service-price');await page.locator('#service-price').fill('95 DT');await page.getByRole('button',{name:'Appliquer',exact:true}).click();
 assert.deepEqual((await get('/doctors/me')).services,initial.services,'service edits must remain drafts');
 await page.getByRole('button',{name:'Ajouter un service',exact:true}).click();await page.locator('#service-name').fill('Contrôle QA UI');await page.locator('#service-price').fill('65 DT');await page.getByRole('button',{name:'Ajouter le service',exact:true}).click();await profileSave();
 let profile=await get('/doctors/me');assert.equal(profile.serviceTitle,'Cabinet HelloDoc QA');assert.equal(profile.location,'Adresse du cabinet QA');assert.ok(profile.services.some(s=>s.name==='Contrôle QA UI'&&s.price==='65 DT'));
 const publicProfile=await get('/doctors/'+profile.id);assert.ok(publicProfile.services.some(s=>s.price==='95 DT'));
 await page.getByRole('button',{name:'Supprimer Contrôle QA UI',exact:true}).click();await page.getByRole('button',{name:'Conserver le service',exact:true}).click();assert.equal(await page.getByRole('button',{name:'Supprimer Contrôle QA UI',exact:true}).count(),1);
 await page.getByRole('button',{name:'Supprimer Contrôle QA UI',exact:true}).click();await page.getByRole('button',{name:'Supprimer le service',exact:true}).click();await profileSave();assert.ok(!(await get('/doctors/me')).services.some(s=>s.name==='Contrôle QA UI'));
 console.log('PASS cabinet and service add/edit/delete, confirmation and public API persistence');
 await selectTab('Sécurité');await page.locator('#security-current').fill('incorrect-test-password');await page.locator('#security-new').fill(NEW_PASSWORD);await page.locator('#security-confirm').fill('mismatch-test');assert.equal(await page.getByRole('button',{name:'Modifier mon mot de passe',exact:true}).isDisabled(),true);
 await page.getByRole('button',{name:'Afficher le nouveau mot de passe',exact:true}).click();assert.equal(await page.locator('#security-new').getAttribute('type'),'text');await page.getByRole('button',{name:'Masquer le nouveau mot de passe',exact:true}).click();assert.equal(await page.locator('#security-new').getAttribute('type'),'password');
 await page.locator('#security-confirm').fill(NEW_PASSWORD);await page.getByRole('button',{name:'Modifier mon mot de passe',exact:true}).click();await page.getByRole('alert').filter({hasText:'Le mot de passe actuel est incorrect.'}).waitFor();assert.equal(await page.locator('#security-new').inputValue(),NEW_PASSWORD);
 await page.locator('#security-current').fill(PASSWORD);await page.getByRole('button',{name:'Modifier mon mot de passe',exact:true}).click();await saved('Votre mot de passe a été modifié.');assert.equal(await page.locator('#security-current').inputValue(),'');
 const authCheck=await browser.newContext();assert.equal((await login(authCheck,NEW_PASSWORD)).status(),200);assert.equal((await login(authCheck,PASSWORD)).status(),401);await authCheck.close();
 const restored=await context.request.post(base+'/api/auth/password',{headers:await csrf(),data:{currentPassword:NEW_PASSWORD,newPassword:PASSWORD}});assert.equal(restored.status(),204);
 console.log('PASS real password change, mismatch validation, eye controls and failed-attempt recovery');
 await page.locator('.side-nav').getByRole('link',{name:'Disponibilités',exact:true}).click();await ready('#slot-duration');
 const rows=await get('/doctors/me/availabilities');const sourceId=rows[0].isoWeekday;const targetId=sourceId%7+1;
 const source=page.locator(`[data-weekday="${sourceId}"]`),target=page.locator(`[data-weekday="${targetId}"]`);
 const sourceSwitch=source.getByRole('switch');await sourceSwitch.uncheck();await source.getByText('Aucun horaire de consultation.',{exact:false}).waitFor();await sourceSwitch.check();
 const firstStart=page.locator(`#start-${sourceId}-1`),firstEnd=page.locator(`#end-${sourceId}-1`);
 await firstStart.fill('08:00');await firstEnd.fill('12:00');if(await source.getByRole('button',{name:'Ajouter une plage',exact:true}).count())await source.getByRole('button',{name:'Ajouter une plage',exact:true}).click();
 await page.locator(`#start-${sourceId}-2`).fill('11:00');await page.locator(`#end-${sourceId}-2`).fill('17:00');await page.locator(`#end-${sourceId}-2`).blur();await page.getByRole('button',{name:'Enregistrer mes disponibilités',exact:true}).click();await source.getByText(/sans chevauchement/).waitFor();
 assert.equal((await get('/doctors/me/availabilities')).length,rows.length);
 await page.locator(`#start-${sourceId}-2`).fill('14:00');
 if(!await target.getByRole('switch').isChecked())await target.getByRole('switch').check();
 if(await target.locator(`#start-${targetId}-1`).count()){await page.locator(`#start-${targetId}-1`).fill('09:00');await page.locator(`#end-${targetId}-1`).fill('11:00');}
 await source.getByRole('button',{name:'Copier vers…',exact:true}).click();await page.getByRole('dialog').getByRole('checkbox',{name:new RegExp('^'+weekdays[targetId-1])}).check();assert.equal(await page.getByRole('button',{name:'Copier les horaires',exact:true}).isDisabled(),true);await page.getByRole('checkbox',{name:'Je confirme le remplacement de ces horaires.',exact:true}).check();await page.getByRole('button',{name:'Copier les horaires',exact:true}).click();
 assert.equal(await page.locator(`#start-${targetId}-1`).inputValue(),'08:00');
 await page.getByRole('button',{name:'Enregistrer mes disponibilités',exact:true}).click();await saved('Vos disponibilités ont été enregistrées.');
 assert.equal((await get('/doctors/me/availabilities')).filter(r=>r.isoWeekday===targetId).length,2);
 // Deletion is a draft action with a confirmation, followed by an actual server save.
 await target.getByRole('button',{name:'Supprimer la deuxième plage du '+weekdays[targetId-1].toLowerCase(),exact:true}).click();await page.getByRole('button',{name:'Conserver la plage',exact:true}).click();assert.equal(await page.locator(`#start-${targetId}-2`).count(),1);
 await target.getByRole('button',{name:'Supprimer la deuxième plage du '+weekdays[targetId-1].toLowerCase(),exact:true}).click();await page.getByRole('button',{name:'Supprimer la plage',exact:true}).click();await page.getByRole('button',{name:'Enregistrer mes disponibilités',exact:true}).click();await saved('Vos disponibilités ont été enregistrées.');
 await target.getByRole('switch').uncheck();await page.getByRole('button',{name:'Enregistrer mes disponibilités',exact:true}).click();await saved('Vos disponibilités ont été enregistrées.');await page.reload();await ready('#slot-duration');assert.equal(await target.getByRole('switch').isChecked(),false);assert.equal((await get('/doctors/me/availabilities')).filter(r=>r.isoWeekday===targetId).length,0);
 const testPause=(await page.locator('#slot-pause').inputValue())==='11'?'12':'11';const failWeekly=route=>route.request().method()==='PUT'?route.abort('failed'):route.continue();await page.route('**/api/doctors/me/availabilities/weekly',failWeekly);await page.locator('#slot-pause').fill(testPause);await page.getByRole('button',{name:'Enregistrer mes disponibilités',exact:true}).click();await page.getByRole('alert').filter({hasText:'n’ont pas été enregistrées'}).waitFor();assert.equal(await page.locator('#slot-pause').inputValue(),testPause);assert.equal(await page.locator('.editor-message.is-success').count(),0);await page.unroute('**/api/doctors/me/availabilities/weekly',failWeekly);await page.getByRole('button',{name:'Enregistrer mes disponibilités',exact:true}).click();await saved('Vos disponibilités ont été enregistrées.');assert.equal((await get('/doctors/me')).pauseMinutes,Number(testPause));
 assert.deepEqual(signatures((await get('/doctors/me/appointments?size=100')).content),signatures(beforeAppointments));
 console.log('PASS day toggles, two periods, inline overlap errors, confirmed copy/delete, reload and network retry; existing appointments unchanged');
 for(const width of [1440,1280,1024,768,390]){
  await page.setViewportSize({width,height:1000});await page.goto(base+'/medecin/profil');await ready('#profile-firstName');await assertFits('personal-'+width);await page.screenshot({path:path.join(output,`profil-${width}.png`),fullPage:true});
  await selectTab('Cabinet et services');await ready('#practice-name');await assertFits('practice-'+width);await page.screenshot({path:path.join(output,`cabinet-${width}.png`),fullPage:true});
  await selectTab('Sécurité');await ready('#security-current');await assertFits('security-'+width);await page.screenshot({path:path.join(output,`securite-${width}.png`),fullPage:true});
  await page.goto(base+'/medecin/disponibilites');await ready('#slot-duration');await assertFits('weekly-'+width);await page.screenshot({path:path.join(output,`disponibilites-${width}.png`),fullPage:true});
  console.log('PASS responsive width '+width+'px at normal zoom');
 }
 // Desktop 1440px at 200%: equivalent 720px layout viewport, rasterized at 2x.
 const zoomContext=await browser.newContext({storageState:await context.storageState(),viewport:{width:720,height:500},deviceScaleFactor:2,locale:'fr-FR'});page=await zoomContext.newPage();page.on('pageerror',e=>jsErrors.push(e.message));assert.equal(await page.evaluate(()=>devicePixelRatio),2);
 for(const area of ['profil','cabinet','securite','disponibilites']){
  await page.goto(base+'/medecin/'+(area==='disponibilites'?'disponibilites':'profil'));await ready(area==='disponibilites'?'#slot-duration':'#profile-firstName');
  if(area==='cabinet')await selectTab('Cabinet et services');if(area==='securite')await selectTab('Sécurité');
  await assertFits(area+'-200-percent');await page.screenshot({path:path.join(output,`${area}-zoom200.png`),fullPage:true});
 }
 await page.goto(base+'/medecin/profil');await ready('#profile-firstName');
 await page.getByRole('tab',{name:'Profil personnel',exact:true}).focus();await page.keyboard.press('ArrowRight');assert.equal(await page.getByRole('tab',{name:'Cabinet et services',exact:true}).getAttribute('aria-selected'),'true');
 await page.getByRole('button',{name:'Ajouter un service',exact:true}).click();await ready('#service-name');assert.equal(await page.locator('#service-name').evaluate(n=>n===document.activeElement),true);
 await page.keyboard.press('Escape');assert.equal(await page.getByRole('dialog').count(),0);
 assert.equal(await page.getByRole('button',{name:'Ajouter un service',exact:true}).evaluate(n=>n===document.activeElement),true);
 await page.getByRole('button',{name:'Ouvrir le menu',exact:true}).click();await page.locator('.sidebar.open').waitFor();await page.keyboard.press('Escape');assert.equal(await page.locator('.sidebar.open').count(),0);
 console.log('PASS keyboard tabs, modal Escape/focus restoration and responsive navigation');

 assert.deepEqual(jsErrors,[],'Browser runtime errors');console.log('PASS 200% zoom-equivalent layouts and no JavaScript runtime errors');
}catch(error){await page.screenshot({path:path.join(output,'failure.png'),fullPage:true}).catch(()=>{});throw error;}
finally{await browser.close();}
