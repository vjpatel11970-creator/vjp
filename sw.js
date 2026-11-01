/* શાળા વ્યવસ્થાપન સિસ્ટમ — service worker
   - પાનું (index.html): પહેલા ઇન્ટરનેટથી (cache વગર) લાવે, એટલે નવી આવૃત્તિ હંમેશાં તરત મળે;
     ઇન્ટરનેટ ન હોય તો છેલ્લી સચવાયેલી નકલ ખૂલે.
   - Google Sheet (Apps Script), ફોન્ટ, CDN વગેરે બીજી સાઇટની વિનંતીને આ ક્યારેય અડતું નથી —
     ડેટા હંમેશાં સીધો સર્વરથી જ આવે/જાય છે. */
var VERSION = "v2";
var CACHE = "shala-" + VERSION;
var SHELL = ["./", "./index.html", "./manifest.webmanifest", "./manifest-student.webmanifest", "./manifest-teacher.webmanifest",
             "./icon-192.png", "./icon-512.png", "./icon-maskable-512.png"];

self.addEventListener("install", function(e){
  e.waitUntil(
    caches.open(CACHE).then(function(c){
      return Promise.all(SHELL.map(function(u){ return c.add(u).catch(function(){}); }));
    }).then(function(){ return self.skipWaiting(); })
  );
});

self.addEventListener("activate", function(e){
  e.waitUntil(
    caches.keys().then(function(keys){
      return Promise.all(keys.filter(function(k){ return k.indexOf("shala-") === 0 && k !== CACHE; })
        .map(function(k){ return caches.delete(k); }));
    }).then(function(){ return self.clients.claim(); })
  );
});

self.addEventListener("fetch", function(e){
  var req = e.request;
  if (req.method !== "GET") return;
  var url = new URL(req.url);
  if (url.origin !== self.location.origin) return;

  if (req.mode === "navigate"){
    /* નેટવર્ક ધીમું હોય (2.5 સેકન્ડથી વધુ) તો સચવાયેલી નકલ તરત બતાવો; નવી નકલ પાછળ અપડેટ થતી રહે */
    e.respondWith(new Promise(function(resolve){
      var done = false;
      function fromCache(){ return caches.match("./index.html").then(function(r){ return r || caches.match("./"); }); }
      fetch(req.url, {cache: "no-store"}).then(function(res){
        if (res && res.ok){
          var copy = res.clone();
          caches.open(CACHE).then(function(c){ c.put("./index.html", copy); });
        }
        if (!done){ done = true; resolve(res); }
      }).catch(function(){
        if (!done){ done = true; fromCache().then(resolve); }
      });
      setTimeout(function(){
        if (done) return;
        fromCache().then(function(r){ if (r && !done){ done = true; resolve(r); } });
      }, 2500);
    }));
    return;
  }

  e.respondWith(
    fetch(req).then(function(res){
      if (res && res.ok){
        var copy = res.clone();
        caches.open(CACHE).then(function(c){ c.put(req, copy); });
      }
      return res;
    }).catch(function(){ return caches.match(req); })
  );
});
