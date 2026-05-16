(function(){
  const user = window.RVDB ? RVDB.getCurrentUser() : null;
  if(!user || user.role !== 'client') { window.location.href = '../interface.html'; return; }

  function go(url){ window.location.href = url; }
  function buildShell(){
    if(document.getElementById('clientTopbar')) return;
    const dbUser = (RVDB.loadDb().clients || []).find(c => c.id === user.id) || user;
    const name = dbUser.fullName || user.fullName || user.username || 'Client Démo';
    const image = dbUser.image || dbUser.profileImage || '';
    const avatarHtml = image ? '<img src="' + image + '" alt="Profil">' : '👤';
    const notifCount = RVDB.getUnreadNotificationCount ? RVDB.getUnreadNotificationCount('client', user.id) : 0;

    const topbar = document.createElement('div');
    topbar.id = 'clientTopbar';
    topbar.className = 'client-topbar';
    topbar.innerHTML = `
      <a class="brand-mini" href="index.html" aria-label="Retour a l\'accueil">Plateforme RDV</a>
      <nav class="client-main-nav">
        <a href="prestations.html">Nos prestations</a>
        <a href="suivi.html">Suivi</a>
        <a href="historique.html">Historique</a>
      </nav>
      <div class="topbar-actions">
        <button class="notif-btn" id="openNotif" title="Notifications">🔔${notifCount > 0 ? '<span class="notif-badge">'+notifCount+'</span>' : ''}</button>
        <button class="profile-btn" id="openClientPanel" title="Espace Patient">
          <span class="profile-avatar">${avatarHtml}</span>
          <span><strong>${name}</strong><small>Patient</small></span>
        </button>
      </div>`;
    document.body.prepend(topbar);

    const overlay = document.createElement('div');
    overlay.id = 'clientOverlay';
    overlay.className = 'drawer-overlay';
    document.body.appendChild(overlay);

    const panel = document.createElement('aside');
    panel.id = 'clientAccountPanel';
    panel.className = 'account-panel';
    panel.innerHTML = `
      <div class="account-top">
        <div class="account-avatar">${avatarHtml}</div>
        <div><strong>${name}</strong><span>Client</span></div>
        <button class="close-panel" id="closeClientPanel">×</button>
      </div>
      <nav class="account-menu">
        <div class="menu-title">Espace compte</div>
        <a href="parametre.html" class="menu-link">⚙ <span>Paramètres</span><b>›</b></a>
      </nav>
      <button id="logoutClientBtn">↪ Se déconnecter</button>`;
    document.body.appendChild(panel);
    document.body.classList.add('has-client-topbar');

    function openPanel(){ panel.classList.add('open'); overlay.classList.add('show'); }
    function closePanel(){ panel.classList.remove('open'); overlay.classList.remove('show'); }
    document.getElementById('openClientPanel').onclick = openPanel;
    document.getElementById('closeClientPanel').onclick = closePanel;
    overlay.onclick = closePanel;
    document.getElementById('openNotif').onclick = function(){ go('notifications.html'); };
    document.getElementById('logoutClientBtn').onclick = function(){ RVDB.logout(); window.location.href='../interface.html'; };
  }
  document.addEventListener('DOMContentLoaded', buildShell);
})();
