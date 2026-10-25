// VitaTrack — JavaScript
'use strict';

// ─── Notifications badge ───────────────────────────────────────────────
async function loadNotifications() {
  try {
    const r = await fetch('/api/notifications');
    if (!r.ok) return;
    const data = await r.json();
    const items = Array.isArray(data) ? data : [];
    const badge = document.getElementById('notif-badge');
    if (badge && items.length > 0) {
      badge.textContent = items.length;
      badge.style.display = 'inline-flex';
    }
  } catch { /* silencieux */ }
}

// ─── Slider valeur affichée ────────────────────────────────────────────
function setupSliders() {
  document.querySelectorAll('input[type=range]').forEach(slider => {
    const out = document.createElement('output');
    out.textContent = slider.value;
    out.style.marginLeft = '8px';
    out.style.fontWeight = '600';
    slider.after(out);
    slider.addEventListener('input', () => { out.textContent = slider.value; });
  });
}

// ─── Confirmation avant suppression ───────────────────────────────────
function setupDeleteConfirm() {
  document.querySelectorAll('[data-confirm]').forEach(btn => {
    btn.addEventListener('click', e => {
      if (!confirm(btn.dataset.confirm)) e.preventDefault();
    });
  });
}

// ─── Toast notifications ───────────────────────────────────────────────
function toast(msg, type = 'info') {
  const el = document.createElement('div');
  el.className = `toast toast-${type}`;
  el.textContent = msg;
  document.body.appendChild(el);
  setTimeout(() => el.remove(), 3500);
}

// PWA Service Worker
if ('serviceWorker' in navigator) {
  navigator.serviceWorker.register('/assets/sw.js').catch(() => {});
}

document.addEventListener('DOMContentLoaded', () => {
  loadNotifications();
  setupSliders();
  setupDeleteConfirm();
});
