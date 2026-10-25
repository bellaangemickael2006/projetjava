// Service Worker VitaTrack
const CACHE = 'vitatrack-v1';
const STATIC = ['/assets/css/style.css', '/assets/js/app.js'];

self.addEventListener('install', e => {
  e.waitUntil(caches.open(CACHE).then(c => c.addAll(STATIC)));
  self.skipWaiting();
});
self.addEventListener('fetch', e => {
  if (e.request.url.includes('/api/') || e.request.method !== 'GET') return;
  e.respondWith(
    caches.match(e.request).then(r => r || fetch(e.request))
  );
});
