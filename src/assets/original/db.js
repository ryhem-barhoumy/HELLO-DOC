(function () {
  const DB_KEY = 'rv_database_v6_final_dynamic';
  const OLD_KEYS = ['rv_database_v5_dynamic_admins', 'rv_database_v4', 'rv_database_v3', 'rv_database_v2', 'rv_database'];
  const SESSION_KEY = 'rv_current_user';
  function today() { const d = new Date(); return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`; }
  function uid(prefix) { return `${prefix}_${Date.now()}_${Math.floor(Math.random() * 10000)}`; }
  function normalizeEmail(v) { return (v || '').trim().toLowerCase(); }
  function toMin(h) { const [hh, mm] = (h || '00:00').split(':').map(Number); return (hh || 0) * 60 + (mm || 0); }
  function toHour(m) {
    return String(Math.floor(m / 60)).padStart(2, '0') + ':' + String(m % 60).padStart(2, '0');
  }
  function currentMinutes() {
    const d = new Date();
    return d.getHours() * 60 + d.getMinutes();
  }

  function isPastSlot(slot) {
    if (!slot || !slot.date) return false;
    if (slot.date < today()) return true;
    if (slot.date > today()) return false;
    return toMin(slot.end || slot.start) <= currentMinutes();
  }

  function syncExpired(db) {
    db.slots.forEach(s => {
      if (isPastSlot(s) && s.status === 'Disponible') s.status = 'Bloqué';
      if (isPastSlot(s) && s.status === 'Réservé') s.status = 'Bloqué';
    });

    db.appointments.forEach(r => {
      const slot = db.slots.find(s => s.id === r.slotId);
      if (slot && isPastSlot(slot) && r.status === 'Confirmé') {
        // Le rendez-vous ne devient pas automatiquement terminé.
        // Le prestataire doit cliquer sur "Terminer" après l'heure de fin.
        slot.status = 'Bloqué';
      }
    });

    return db;
  }
  function numberFromText(v, fallback) { const m = String(v ?? '').match(/\d+/); const n = parseInt(m ? m[0] : '', 10); return Number.isFinite(n) && n >= 0 ? n : fallback; }

  function parseServiceText(text) {
    const raw = String(text || '').replace(/\n+/g, ' / ').trim();
    if (!raw) return [];
    const parts = raw.split(/\s*[\/;]\s*/).map(p => p.trim()).filter(Boolean);
    const source = parts.length ? parts : [raw];
    const parsed = [];
    source.forEach(part => {
      const m = part.match(/^(.+?)\s*[:：]\s*(.+)$/);
      if (m && m[1].trim() && m[2].trim()) {
        const name = m[1].trim();
        const price = m[2].trim();
        const nested = /^(consultation|service|prestation)$/i.test(name) && /[:：]/.test(price) ? parseServiceText(price) : [];
        if (nested.length) parsed.push(...nested);
        else parsed.push({ name, price });
      }
    });
    return parsed;
  }
  function normalizeServices(a) {
    a = a || {};
    const result = [];
    const genericName = name => /^(consultation|service|prestation)$/i.test(String(name || '').trim());
    if (Array.isArray(a.services)) {
      a.services.forEach(item => {
        if (!item) return;
        const name = String(item.name || '').trim();
        const price = String(item.price || '').trim();
        const parsedPrice = parseServiceText(price);
        const parsedName = parseServiceText(name);
        if (parsedPrice.length && (!name || genericName(name) || parsedPrice.length > 1)) {
          result.push(...parsedPrice);
        } else if (parsedName.length && !price) {
          result.push(...parsedName);
        } else if (name || price) {
          result.push({ name: name || 'Consultation', price: price || 'Sur demande' });
        }
      });
    }
    if (!result.length) {
      const parsed = parseServiceText(a.price || a.tarif || '');
      if (parsed.length) result.push(...parsed);
      else if (a.price || a.tarif) result.push({ name: 'Consultation', price: String(a.price || a.tarif).trim() });
    }
    if (!result.length) result.push({ name: 'Consultation', price: 'Sur demande' });
    const seen = new Set();
    return result
      .map(svc => ({ name: String(svc.name || 'Consultation').trim(), price: String(svc.price || 'Sur demande').trim() }))
      .filter(svc => svc.name && svc.price)
      .filter(svc => {
        const key = (svc.name + '|' + svc.price).toLowerCase();
        if (seen.has(key)) return false;
        seen.add(key);
        return true;
      });
  }
  function slotSignature(admin) { return [numberFromText(admin.durationMinutes || admin.duration, 30), admin.workStart || '08:00', admin.workEnd || '17:00', numberFromText(admin.pauseMinutes, 0)].join('|'); }
  function defaultDb() {
    const t = today();
    return {
      clients: [{ id: 'client_default', fullName: 'Client Démo', email: 'client', password: 'client', phone: '', address: '' }],
      admins: [{
        id: 'admin_default', fullName: 'Dr. Ahmed Ben Ali', ownerName: 'Dr. Ahmed Ben Ali', email: 'admin', password: 'admin', phone: '',
        specialty: 'Médecin généraliste', serviceType: 'Consultation médicale', serviceTitle: 'Dr. Ahmed Ben Ali',
        description: 'Consultation médicale avec le Dr. Ahmed Ben Ali dans un cadre simple, rapide et bien organisé.',
        duration: '30 à 45 minutes', availability: 'Selon les créneaux disponibles', location: 'Avenue Habib Bourguiba, Tunis', price: '60 DT', image: 'medical.png', profileImage: 'medical.png', autoSlots: false, durationMinutes: 30, workStart: '09:00', workEnd: '15:00', pauseMinutes: 0
      }],
      slots: [
        { id: 'slot_1', adminId: 'admin_default', date: t, start: '09:00', end: '09:30', status: 'Disponible' },
        { id: 'slot_2', adminId: 'admin_default', date: t, start: '10:00', end: '10:30', status: 'Disponible' },
        { id: 'slot_3', adminId: 'admin_default', date: t, start: '14:00', end: '14:30', status: 'Disponible' }
      ],
      appointments: [],
      notifications: []
    };
  }
  function migrateOldData(db) {
    for (const k of OLD_KEYS) {
      try {
        const old = JSON.parse(localStorage.getItem(k));
        if (old && (old.clients?.length || old.appointments?.length || old.slots?.length || old.admins?.length)) return old;
      } catch (e) { }
    }
    return db;
  }
  function completeDb(db) {
    const def = defaultDb();
    db.clients ||= []; db.admins ||= []; db.slots ||= []; db.appointments ||= []; db.notifications ||= []; db.absences ||= [];
    if (!db.clients.length) db.clients.push(def.clients[0]);
    if (!db.admins.length) db.admins.push(def.admins[0]);
    db.clients = db.clients.map(c => ({
      phone: '', address: '', image: '', profileImage: '',
      ...c,
      fullName: c.fullName || c.name || 'Client',
      image: c.image || c.profileImage || '',
      profileImage: c.profileImage || c.image || ''
    }));
    db.admins = db.admins.map(a => {
      const merged = {
        serviceType: a.specialty || 'Prestation', serviceTitle: a.serviceTitle || a.fullName || 'Prestataire',
        description: 'Prestation disponible avec réservation en ligne.', duration: '30 minutes', availability: 'Selon les créneaux disponibles', location: 'Non précisé', price: 'Sur demande', image: 'medical.png', profileImage: 'medical.png', autoSlots: false, durationMinutes: 30, workStart: '08:00', workEnd: '17:00', pauseMinutes: 0,
        ...a
      };
      merged.services = normalizeServices(merged);
      if (merged.services.length) merged.price = merged.services.map(svc => svc.name + ': ' + svc.price).join(' / ');
      merged.fullName = merged.serviceTitle || merged.fullName || 'Prestataire';
      merged.ownerName = merged.ownerName || a.ownerName || a.fullName || '';
      merged.image = merged.image || merged.profileImage || 'medical.png';
      merged.profileImage = merged.profileImage || merged.image || 'medical.png';
      return merged;
    });
    db.slots = db.slots.filter(s => s && s.date && s.start && s.end && s.start !== 'undefined' && s.end !== 'undefined').map(s => ({ ...s, adminId: s.adminId || db.admins[0].id, status: s.status || 'Disponible' }));
    db.appointments = db.appointments.map(r => ({ ...r, serviceKind: r.serviceKind || (r.adminId ? 'admin' : 'external'), selectedService: r.selectedService || r.service || 'Prestation' }));
    return db;
  }
  function loadDb() {
    let raw = localStorage.getItem(DB_KEY);
    let db;

    if (!raw) db = migrateOldData(defaultDb());
    else {
      try { db = JSON.parse(raw); }
      catch (e) { db = defaultDb(); }
    }

    db = completeDb(db);
    db = syncExpired(db);
    saveDb(db);

    return db;
  }
  function saveDb(db) { localStorage.setItem(DB_KEY, JSON.stringify(completeDb(db))); }
  function addNotification(db, targetRole, targetId, title, message, appointmentId, extra) {
    extra = extra || {};
    db.notifications ||= [];
    db.notifications.push({
      id: uid('notif'), targetRole, targetId,
      recipientRole: targetRole, userId: targetId, recipientId: targetId,
      title, message, appointmentId: appointmentId || null, read: false,
      createdAt: new Date().toISOString(),
      type: extra.type || 'info', reason: extra.reason || '', comment: extra.comment || '',
      actionUrl: extra.actionUrl || '', actionLabel: extra.actionLabel || ''
    });
  }
  function getNotifications(role, id) {
    const db = loadDb();
    return (db.notifications || []).filter(n => n.targetRole === role && n.targetId === id).sort((a, b) => (b.createdAt || '').localeCompare(a.createdAt || ''));
  }
  function getUnreadNotificationCount(role, id) {
    return getNotifications(role, id).filter(n => !n.read).length;
  }
  function markNotificationsRead(role, id) {
    const db = loadDb();
    (db.notifications || []).forEach(n => { if (n.targetRole === role && n.targetId === id) n.read = true; });
    saveDb(db);
  }
  function setCurrentUser(role, user) { localStorage.setItem(SESSION_KEY, JSON.stringify({ role, id: user.id, fullName: user.fullName, ownerName: user.ownerName || '', serviceTitle: user.serviceTitle || '', email: user.email, phone: user.phone || '', address: user.address || '', image: user.image || user.profileImage || '', profileImage: user.profileImage || user.image || '' })); }
  function getCurrentUser() { try { return JSON.parse(localStorage.getItem(SESSION_KEY)); } catch (e) { return null; } }
  function logout() { localStorage.removeItem(SESSION_KEY); }
  function getDefaultAdminId() { return loadDb().admins[0].id; }
  function addClient(data) { const db = loadDb(); const email = (data.email || '').trim(); if (!email || !data.password || !(data.fullName || '').trim()) throw new Error('Veuillez remplir les champs obligatoires.'); if (db.clients.some(c => normalizeEmail(c.email) === normalizeEmail(email))) throw new Error('Cet email existe déjà.'); const img = data.image || data.profileImage || ''; const user = { id: uid('client'), fullName: (data.fullName || '').trim(), email, password: data.password, phone: (data.phone || '').trim(), address: (data.address || '').trim(), image: img, profileImage: img }; db.clients.push(user); saveDb(db); return user; }
  function addAdmin(data) { const db = loadDb(); const email = (data.email || '').trim(); if (!email || !data.password || !(data.fullName || '').trim()) throw new Error('Veuillez remplir les champs obligatoires.'); if (db.admins.some(a => normalizeEmail(a.email) === normalizeEmail(email))) throw new Error('Cet email existe déjà.'); const title = (data.serviceTitle || data.fullName || 'Prestataire').trim(); const loc = (data.location || data.serviceLocation || data.address || 'Non précisé').trim(); const user = { id: uid('admin'), fullName: title, ownerName: (data.fullName || '').trim(), email, password: data.password, phone: (data.phone || '').trim(), specialty: (data.specialty || 'Prestataire').trim(), serviceType: (data.serviceType || data.specialty || 'Prestation').trim(), serviceTitle: title, description: (data.description || 'Prestation disponible avec réservation en ligne.').trim(), duration: (data.duration || '30 minutes').trim(), availability: (data.availability || 'Selon les créneaux disponibles').trim(), durationMinutes: numberFromText(data.durationMinutes || data.duration, 30), workStart: data.workStart || '08:00', workEnd: data.workEnd || '17:00', pauseMinutes: numberFromText(data.pauseMinutes, 0), autoSlots: true, services: normalizeServices(data), location: loc, serviceLocation: loc, address: loc, price: (data.price || 'Sur demande').trim(), image: data.image || 'medical.png', profileImage: data.image || 'medical.png' }; db.admins.push(user); saveDb(db); return user; }
  function login(role, email, password) { const db = loadDb(); const list = role === 'admin' ? db.admins : db.clients; const key = normalizeEmail(email); const pwd = password || ''; const user = list.find(u => (normalizeEmail(u.email) === key || normalizeEmail(u.fullName) === key || normalizeEmail(u.serviceTitle) === key) && u.password === pwd); if (user) setCurrentUser(role, user); return user; }
  function addSlot(data) { const db = loadDb(); const slot = { id: uid('slot'), adminId: data.adminId || getDefaultAdminId(), date: data.date, start: data.start, end: data.end, status: data.status || 'Disponible' }; db.slots.push(slot); saveDb(db); return slot; }
  function generateSlotsForDate(adminId, date) {
    const db = loadDb();
    const admin = db.admins.find(a => a.id === adminId);
    if (!admin || !admin.autoSlots || !date || date < today()) return db.slots.filter(s => s.adminId === adminId && s.date === date);
    const dur = Math.max(5, numberFromText(admin.durationMinutes || admin.duration, 30));
    const pause = Math.max(0, numberFromText(admin.pauseMinutes, 0));
    const signature = [dur, admin.workStart || '08:00', admin.workEnd || '17:00', pause].join('|');
    let existing = db.slots.filter(s => s.adminId === adminId && s.date === date);
    const reserved = existing.filter(s => s.status === 'Réservé' && s.start && s.end && s.start !== 'undefined' && s.end !== 'undefined');
    const reusable = existing.length && existing.every(s => s.signature === signature && s.start && s.end && s.start !== 'undefined' && s.end !== 'undefined');
    if (reusable) return existing;
    db.slots = db.slots.filter(s => !(s.adminId === adminId && s.date === date));
    db.slots.push(...reserved);
    let cursor = toMin(admin.workStart || '08:00'), end = toMin(admin.workEnd || '17:00');
    if (end <= cursor || dur <= 0) { saveDb(db); return db.slots.filter(s => s.adminId === adminId && s.date === date); }
    const created = []; let i = 0;
    while (cursor + dur <= end && i < 120) {
      const st = toHour(cursor), en = toHour(cursor + dur);
      const conflict = reserved.some(s => !(toMin(en) <= toMin(s.start) || toMin(st) >= toMin(s.end)));
      if (!conflict) created.push({ id: uid('slot'), adminId, date, start: st, end: en, status: 'Disponible', signature });
      cursor += dur + pause;
      i++;
    }
    db.slots.push(...created); saveDb(db);
    return db.slots.filter(s => s.adminId === adminId && s.date === date).sort((a, b) => (a.start || '').localeCompare(b.start || ''));
  }
  function updateSlot(id, patch) { const db = loadDb(); const s = db.slots.find(x => x.id === id); if (s) Object.assign(s, patch); saveDb(db); return s; }
  function deleteSlot(id) { const db = loadDb(); db.slots = db.slots.filter(s => s.id !== id); saveDb(db); }
  function syncClient(db, data) { const client = db.clients.find(c => c.id === data.clientId); if (client) { client.fullName = data.patient || client.fullName; client.email = data.email || client.email; client.phone = data.phone || client.phone || ''; client.address = data.address || client.address || ''; } return client; }
  function createAppointment(data) {
    const db = loadDb();
    const slot = db.slots.find(s => s.id === data.slotId && s.status === 'Disponible');
    if (!slot) throw new Error('Ce créneau n’est plus disponible.');
    if (isPastSlot(slot)) {
      slot.status = 'Bloqué';
      saveDb(db);
      throw new Error('Impossible de réserver un créneau déjà passé.');
    }
    const client = syncClient(db, data);
    slot.status = 'Réservé';
    const admin = db.admins.find(a => a.id === slot.adminId);
    const selectedService = (data.selectedService || data.service || 'Prestation').trim();
    const appt = {
      id: uid('rdv'), clientId: data.clientId, adminId: slot.adminId, slotId: slot.id,
      patient: data.patient, phone: data.phone, email: data.email, address: data.address,
      service: selectedService || (admin?.serviceType || 'Prestation'), selectedService,
      price: data.price || '', serviceKind: 'admin', date: slot.date, time: slot.start,
      notes: data.notes || '', status: 'En attente', createdAt: new Date().toISOString()
    };
    db.appointments.push(appt);
    addNotification(db, 'client', data.clientId, 'Demande envoyée', 'Votre demande de rendez-vous pour ' + appt.service + ' est en attente de validation.', appt.id);
    addNotification(db, 'admin', slot.adminId, 'Nouvelle réservation', 'Nouvelle demande de ' + appt.patient + ' pour ' + appt.service + ' le ' + appt.date + ' à ' + appt.time + '.', appt.id);
    saveDb(db);
    if (client) setCurrentUser('client', client);
    return appt;
  }
  function createExternalAppointment(data) { const db = loadDb(); const client = syncClient(db, data); const appt = { id: uid('rdv'), clientId: data.clientId, adminId: null, slotId: null, patient: data.patient, phone: data.phone, email: data.email, address: data.address, service: data.service || 'Prestation externe', serviceKind: 'external', date: data.date, time: data.time || '', notes: data.notes || '', status: 'En attente', createdAt: new Date().toISOString() }; db.appointments.push(appt); saveDb(db); if (client) setCurrentUser('client', client); return appt; }
  function updateAppointment(id, patch) { const db = loadDb(); const r = db.appointments.find(x => x.id === id); if (r) { const oldStatus = r.status; Object.assign(r, patch); if (patch.status && patch.status !== oldStatus && r.clientId && r.clientId !== 'admin_direct') addNotification(db, 'client', r.clientId, 'Rendez-vous ' + patch.status, 'Votre rendez-vous pour ' + r.service + ' le ' + r.date + (r.time ? ' à ' + r.time : '') + ' est maintenant : ' + patch.status + '.', r.id); } saveDb(db); return r; }
  function cancelAppointment(id, options) { const db = loadDb(); const r = db.appointments.find(x => x.id === id); options = options || {}; if (r) { const oldStatus = r.status; r.status = 'Annulé'; r.cancelReason = options.reason || r.cancelReason || ''; r.cancelComment = options.comment || r.cancelComment || ''; r.cancelledBy = options.byPrestataire ? 'prestataire' : (options.byClient ? 'client' : r.cancelledBy || 'system'); r.cancelledAt = new Date().toISOString(); const s = db.slots.find(x => x.id === r.slotId); if (s) s.status = 'Disponible'; if (!options.skipNotification && oldStatus !== 'Annulé' && r.clientId && r.clientId !== 'admin_direct') { const detailReason = r.cancelReason ? ' Motif : ' + r.cancelReason + '.' : ''; const detailComment = r.cancelComment ? ' Commentaire : ' + r.cancelComment : ''; const who = options.byPrestataire ? ' par le prestataire' : ''; addNotification(db, 'client', r.clientId, 'Rendez-vous annulé' + who, 'Votre rendez-vous pour ' + r.service + ' le ' + r.date + (r.time ? ' à ' + r.time : '') + ' a été annulé.' + detailReason + detailComment, r.id, { type: 'cancel', reason: r.cancelReason, comment: r.cancelComment }); } } saveDb(db); return r; }
  function updateUser(role, id, patch, currentPassword) { const db = loadDb(); const list = role === 'admin' ? db.admins : db.clients; const u = list.find(x => x.id === id); if (!u) throw new Error('Compte introuvable.'); if (currentPassword !== u.password) throw new Error('Mot de passe actuel incorrect.'); Object.assign(u, patch); if (role === 'admin' && patch.serviceTitle) { u.fullName = patch.serviceTitle; } saveDb(db); setCurrentUser(role, u); return u; }
  function changePassword(role, id, currentPassword, newPassword) { return updateUser(role, id, { password: newPassword }, currentPassword); }
  function deleteAccount(role, id, currentPassword) {
    const db = loadDb();
    if (role === 'admin') {
      const a = db.admins.find(x => x.id === id);
      if (!a) throw new Error('Compte introuvable.');
      if (currentPassword !== a.password) throw new Error('Mot de passe incorrect.');
      db.admins = db.admins.filter(x => x.id !== id);
      db.slots = db.slots.filter(s => s.adminId !== id);
      db.appointments = db.appointments.filter(r => r.adminId !== id);
    } else {
      const c = db.clients.find(x => x.id === id);
      if (!c) throw new Error('Compte introuvable.');
      if (currentPassword !== c.password) throw new Error('Mot de passe incorrect.');
      db.clients = db.clients.filter(x => x.id !== id);
      db.appointments = db.appointments.filter(r => r.clientId !== id);
    }
    saveDb(db); logout(); return true;
  }
  window.RVDB = { today, loadDb, saveDb, normalizeServices, addClient, addAdmin, login, setCurrentUser, getCurrentUser, logout, getDefaultAdminId, addSlot, updateSlot, deleteSlot, createAppointment, createExternalAppointment, updateAppointment, cancelAppointment, generateSlotsForDate, updateUser, changePassword, deleteAccount, getNotifications, getUnreadNotificationCount, markNotificationsRead, isPastSlot };
})();


