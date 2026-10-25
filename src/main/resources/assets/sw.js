/* ─────────────────────────────────────────
   VitaTrack Service Worker — Cache & Offline
   ───────────────────────────────────────── */

const CACHE = 'vitatrack-v2';
const STATIC = [
  '/',
  '/assets/css/style.css',
  '/assets/js/app.js',
  '/assets/manifest.json',
  '/assets/icons/icon-192.png',
  '/assets/icons/icon-512.png'
];

// ── Installation : mise en cache des ressources statiques ──
self.addEventListener('install', e => {
  e.waitUntil(
    caches.open(CACHE).then(c => c.addAll(STATIC))
      .then(() => self.skipWaiting())
  );
});

// ── Activation : nettoyage des anciens caches ──
self.addEventListener('activate', e => {
  e.waitUntil(
    caches.keys().then(keys =>
      Promise.all(keys.filter(k => k !== CACHE).map(k => caches.delete(k)))
    ).then(() => self.clients.claim())
  );
});

// ── Fetch : stratégie Network-first pour les pages, Cache-first pour les assets ──
self.addEventListener('fetch', e => {
  const url = new URL(e.request.url);

  // Ignorer les requêtes non-HTTP et les appels API (toujours en ligne)
  if (!url.protocol.startsWith('http')) return;
  if (url.pathname.startsWith('/api/')) return;
  if (e.request.method !== 'GET') return;

  // Assets statiques → cache d'abord
  if (url.pathname.startsWith('/assets/')) {
    e.respondWith(
      caches.match(e.request).then(cached => {
        return cached || fetch(e.request).then(res => {
          if (res.ok) {
            const clone = res.clone();
            caches.open(CACHE).then(c => c.put(e.request, clone));
          }
          return res;
        });
      })
    );
    return;
  }

  // Pages → réseau d'abord, fallback sur cache
  e.respondWith(
    fetch(e.request)
      .then(res => {
        // Mettre en cache la page de login pour offline
        if (url.pathname === '/' || url.pathname === '/login') {
          const clone = res.clone();
          caches.open(CACHE).then(c => c.put(e.request, clone));
        }
        return res;
      })
      .catch(() => caches.match(e.request).then(cached => {
        return cached || caches.match('/');
      }))
  );
});

// ── Notifications push (si activées) ──
self.addEventListener('push', e => {
  if (!e.data) return;
  const data = e.data.json();
  e.waitUntil(
    self.registration.showNotification(data.title || 'VitaTrack', {
      body: data.body || '',
      icon: '/assets/icons/icon-192.png',
      badge: '/assets/icons/icon-192.png',
      tag: data.tag || 'vitatrack',
      renotify: true,
      data: { url: data.url || '/' }
    })
  );
});

self.addEventListener('notificationclick', e => {
  e.notification.close();
  e.waitUntil(
    clients.openWindow(e.notification.data.url || '/')
  );
});
