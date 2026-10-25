/* ──────────────────────────────────────────────
   VitaTrack — PWA + Install Prompt + Notifications
   ────────────────────────────────────────────── */

// ── 1. Service Worker ────────────────────────
if ('serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('/assets/sw.js')
      .then(reg => console.log('[SW] Enregistré:', reg.scope))
      .catch(err => console.warn('[SW] Échec:', err));
  });
}

// ── 2. Bannière d'installation PWA ──────────
let deferredInstallPrompt = null;

window.addEventListener('beforeinstallprompt', (e) => {
  e.preventDefault();
  deferredInstallPrompt = e;

  // Affiche la bannière si pas encore installé
  const alreadyDismissed = localStorage.getItem('vtk_install_dismissed');
  if (!alreadyDismissed) {
    showInstallBanner();
  }
});

function showInstallBanner() {
  // Évite le doublon
  if (document.getElementById('vtk-install-banner')) return;

  const banner = document.createElement('div');
  banner.id = 'vtk-install-banner';
  banner.innerHTML = `
    <div style="
      position:fixed; bottom:20px; left:50%; transform:translateX(-50%);
      background:#fff; border:2px solid #1a73e8; border-radius:16px;
      box-shadow:0 8px 32px rgba(26,115,232,.25); padding:18px 24px;
      display:flex; align-items:center; gap:16px; z-index:99999;
      max-width:420px; width:calc(100% - 40px); font-family:inherit;
      animation: vtk-slide-up .4s cubic-bezier(.22,1,.36,1);
    ">
      <img src="/assets/icons/icon-192.png" width="48" height="48"
           style="border-radius:12px; flex-shrink:0;"
           alt="VitaTrack">
      <div style="flex:1; min-width:0;">
        <div style="font-weight:700; color:#1a73e8; font-size:15px;">Installer VitaTrack</div>
        <div style="font-size:13px; color:#555; margin-top:2px;">
          Accédez en 1 clic depuis votre bureau, même hors-ligne.
        </div>
      </div>
      <div style="display:flex; flex-direction:column; gap:8px; flex-shrink:0;">
        <button id="vtk-install-btn" style="
          background:#1a73e8; color:#fff; border:none; border-radius:8px;
          padding:8px 16px; cursor:pointer; font-weight:600; font-size:13px;
          white-space:nowrap;
        ">Installer</button>
        <button id="vtk-dismiss-btn" style="
          background:transparent; color:#888; border:none; cursor:pointer;
          font-size:12px; padding:4px;
        ">Plus tard</button>
      </div>
    </div>
    <style>
      @keyframes vtk-slide-up {
        from { opacity:0; transform:translateX(-50%) translateY(40px); }
        to   { opacity:1; transform:translateX(-50%) translateY(0); }
      }
    </style>
  `;

  document.body.appendChild(banner);

  document.getElementById('vtk-install-btn').addEventListener('click', async () => {
    if (!deferredInstallPrompt) return;
    deferredInstallPrompt.prompt();
    const { outcome } = await deferredInstallPrompt.userChoice;
    console.log('[PWA] Choix installation:', outcome);
    deferredInstallPrompt = null;
    banner.remove();
    if (outcome === 'accepted') {
      localStorage.setItem('vtk_install_dismissed', '1');
    }
  });

  document.getElementById('vtk-dismiss-btn').addEventListener('click', () => {
    banner.remove();
    localStorage.setItem('vtk_install_dismissed', '1');
  });
}

// Masque la bannière si l'app est déjà installée en standalone
window.addEventListener('appinstalled', () => {
  const b = document.getElementById('vtk-install-banner');
  if (b) b.remove();
  deferredInstallPrompt = null;
  localStorage.setItem('vtk_install_dismissed', '1');
  console.log('[PWA] Installée avec succès');
});

// Sur iOS (Safari ne supporte pas beforeinstallprompt)
// On montre un message d'instruction manuel
(function checkiOS() {
  const isIOS = /iphone|ipad|ipod/i.test(navigator.userAgent);
  const isStandalone = window.navigator.standalone === true;
  const dismissed = localStorage.getItem('vtk_install_dismissed');

  if (isIOS && !isStandalone && !dismissed) {
    setTimeout(() => {
      showIOSInstallHint();
    }, 2000);
  }
})();

function showIOSInstallHint() {
  if (document.getElementById('vtk-ios-hint')) return;
  const hint = document.createElement('div');
  hint.id = 'vtk-ios-hint';
  hint.innerHTML = `
    <div style="
      position:fixed; bottom:20px; left:50%; transform:translateX(-50%);
      background:#fff; border:2px solid #1a73e8; border-radius:16px;
      box-shadow:0 8px 32px rgba(26,115,232,.25); padding:18px 24px;
      z-index:99999; max-width:360px; width:calc(100% - 40px);
      text-align:center; font-family:inherit;
    ">
      <div style="font-weight:700; color:#1a73e8; margin-bottom:8px;">
        📲 Installer VitaTrack sur iPhone/iPad
      </div>
      <div style="font-size:13px; color:#444; line-height:1.6;">
        Appuie sur <strong>⬆ Partager</strong> puis
        <strong>« Sur l'écran d'accueil »</strong>
      </div>
      <button onclick="this.closest('#vtk-ios-hint').remove(); localStorage.setItem('vtk_install_dismissed','1')"
        style="margin-top:12px; background:#1a73e8; color:#fff; border:none;
               border-radius:8px; padding:8px 20px; cursor:pointer; font-size:13px;">
        OK, compris
      </button>
    </div>
  `;
  document.body.appendChild(hint);
}

// ── 3. Badge notifications ───────────────────
function refreshNotifBadge() {
  const badge = document.getElementById('notif-badge');
  if (!badge) return;
  fetch('/api/notifications')
    .then(r => r.ok ? r.json() : null)
    .then(data => {
      if (!data) return;
      const count = (data.urgents || 0) + (data.rdvs || 0);
      badge.textContent = count > 0 ? count : '';
      badge.style.display = count > 0 ? 'inline-flex' : 'none';
    })
    .catch(() => {});
}

document.addEventListener('DOMContentLoaded', () => {
  refreshNotifBadge();
  setInterval(refreshNotifBadge, 60000); // toutes les minutes
});

// ── 4. Slider — affiche la valeur en direct ──
document.addEventListener('DOMContentLoaded', () => {
  document.querySelectorAll('input[type=range]').forEach(slider => {
    const out = document.getElementById(slider.id + '-val');
    if (out) {
      out.textContent = slider.value;
      slider.addEventListener('input', () => { out.textContent = slider.value; });
    }
  });
});

// ── 5. Confirmation suppression ──────────────
document.addEventListener('DOMContentLoaded', () => {
  document.querySelectorAll('[data-confirm]').forEach(btn => {
    btn.addEventListener('click', e => {
      if (!confirm(btn.dataset.confirm)) e.preventDefault();
    });
  });
});
