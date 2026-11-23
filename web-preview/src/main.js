import { EVENT_CONFIG } from './config.js';
import { REWARDS } from './data/rewards.js';
import { RARITIES } from './data/rarities.js';
import { EventClock } from './core/clock.js';
import { LocalClaimStore } from './core/storage.js';
import { AdventCalendar, DoorState } from './core/calendar.js';
import { CalendarView } from './ui/calendar-view.js';
import { ShowcaseController } from './ui/showcase.js';
import { Sfx } from './ui/audio.js';
import { snowBurst, wait, toast } from './ui/fx.js';
import { WinterBackground } from './three/background.js';

const $ = (id) => document.getElementById(id);
const params = new URLSearchParams(location.search);

/* ------------------------------------------------------------------ core */
const clock = new EventClock({ timeZone: params.get('tz') || EVENT_CONFIG.timeZone });
const debug = params.has('debug') || params.has('date');
if (params.get('date')) {
  const [y, m, d] = params.get('date').split('-').map(Number);
  if (y && m && d) clock.override = clock.zonedMidnight(y, m, d) + 12 * 3600e3;
}
const storeKey = () => `${EVENT_CONFIG.id}:v1:${clock.today().year}${debug ? ':debug' : ''}`;
let calendar = new AdventCalendar({ clock, store: new LocalClaimStore(storeKey()), config: EVENT_CONFIG });

/* ------------------------------------------------------------------ view */
const sfx = new Sfx();
let background;
try { background = new WinterBackground($('bg')); } catch (e) { console.warn('[advent] WebGL background unavailable', e); background = { paused: false }; }
const showcase = new ShowcaseController({ background });
let busy = false;

const view = new CalendarView($('calendar'), calendar, {
  onOpen: (reward, el) => openDoor(reward, el),
  onInspect: (reward) => { if (!busy) showcase.open(reward, { reveal: false }); },
  onLocked: (reward, st) => {
    if (st === DoorState.MISSED) return toast(`Türchen ${reward.day} wurde verpasst – das Event ist vorbei.`);
    const at = clock.zonedMidnight(calendar.year, EVENT_CONFIG.month, reward.day);
    toast(`🔒 Türchen ${reward.day} öffnet am ${reward.day}. Dezember · noch ${formatSpan(at - clock.now())}`);
    sfx.tick();
  },
});

async function openDoor(reward, el) {
  if (busy) return;
  busy = true;
  const res = calendar.claim(reward.day); // claim first → can never be collected twice
  if (!res.ok) { busy = false; view.refresh(); return toast(res.reason === 'already-claimed' ? 'Bereits eingesammelt.' : 'Dieses Türchen ist noch gesperrt.'); }
  const rarity = RARITIES[reward.rarity];
  const mythic = reward.rarity === 'mythic';
  try {
    el.classList.add('glowing');                     // 1. gift starts glowing
    sfx.bell(1318.5, 0, 0.06, 1);
    await wait(mythic ? 1100 : 650);
    snowBurst(el, mythic ? 60 : 28, rarity.glow);    // 2. snowflakes appear
    await wait(250);
    el.classList.add('opening');                     // 3. door opens
    mythic ? sfx.finale() : sfx.open(rarity.rank);
    await wait(mythic ? 700 : 850);
    if (mythic) { $('finale-flash').classList.remove('play'); void $('finale-flash').offsetWidth; $('finale-flash').classList.add('play'); await wait(800); }
    await showcase.open(reward, { reveal: true });   // 4.–6. cosmetic floats out, FX, name + rarity
  } finally {
    el.classList.remove('glowing', 'opening');
    busy = false;
    view.refresh();
    updateProgress();
  }
  toast(`✦ ${reward.name} wurde deiner Sammlung hinzugefügt`);
}

/* ------------------------------------------------------------------ progress + collection */
function updateProgress() {
  const n = calendar.claimedRewards().length;
  $('progress-fill').style.width = `${(n / 24) * 100}%`;
  $('progress-text').textContent = `${n} / 24`;
}

function renderDrawer() {
  const grid = $('drawer-grid'); grid.innerHTML = '';
  for (const r of REWARDS) {
    const owned = calendar.isClaimed(r.day);
    const b = document.createElement('button');
    b.type = 'button'; b.className = `drawer__item${owned ? '' : ' locked'}`;
    b.style.setProperty('--rc', RARITIES[r.rarity].color);
    b.innerHTML = `<img alt="" src="${view.thumbs.get(r.day) ?? ''}" /><span>${r.day}. ${owned ? r.name : '???'}</span>`;
    b.title = owned ? `${r.name} – ${RARITIES[r.rarity].label}` : 'Noch nicht freigeschaltet';
    if (owned) b.addEventListener('click', () => { $('drawer').hidden = true; showcase.open(r); });
    grid.appendChild(b);
  }
}
$('btn-collection').addEventListener('click', () => { renderDrawer(); $('drawer').hidden = false; });
$('drawer-close').addEventListener('click', () => { $('drawer').hidden = true; });
$('drawer').addEventListener('click', (e) => { if (e.target.id === 'drawer') $('drawer').hidden = true; });
$('btn-sound').addEventListener('click', (e) => { sfx.enabled = !sfx.enabled; e.currentTarget.setAttribute('aria-pressed', String(sfx.enabled)); e.currentTarget.textContent = sfx.enabled ? '🔔' : '🔕'; });

/* ------------------------------------------------------------------ countdown */
const pad = (n) => String(n).padStart(2, '0');
function formatSpan(ms) {
  const s = Math.max(0, Math.floor(ms / 1000));
  const d = Math.floor(s / 86400), h = Math.floor((s % 86400) / 3600), m = Math.floor((s % 3600) / 60);
  return d > 0 ? `${d} T ${h} Std` : `${pad(h)}:${pad(m)}:${pad(s % 60)}`;
}
let lastDay = calendar.currentDay();
function tickCountdown() {
  const now = clock.now();
  const day = calendar.currentDay();
  let target, label;
  if (day < EVENT_CONFIG.lastDay) { target = calendar.christmasEveAt(); label = day === 0 ? 'Countdown bis Heiligabend · Event startet am 1. Dezember' : 'Countdown bis Heiligabend'; }
  else if (day === EVENT_CONFIG.lastDay) { target = clock.zonedMidnight(calendar.year, 12, 25); label = 'Heiligabend! Das Finale ist freigeschaltet'; }
  else { target = now; label = 'Frohe Weihnachten! Das Event ist beendet'; }
  const s = Math.max(0, Math.floor((target - now) / 1000));
  $('cd-d').textContent = pad(Math.floor(s / 86400)); $('cd-h').textContent = pad(Math.floor((s % 86400) / 3600));
  $('cd-m').textContent = pad(Math.floor((s % 3600) / 60)); $('cd-s').textContent = pad(s % 60);
  $('countdown-label').textContent = label;
  const next = calendar.nextUnlockAt();
  const nextDay = Math.max(day + 1, 1);
  $('countdown-next').textContent = next ? `Nächstes Türchen (${nextDay}) öffnet in ${formatSpan(next - now)}` : '';
  if (day !== lastDay) {
    lastDay = day; view.refresh();
    if (day >= 1 && day <= 24) { toast(day === 24 ? '✨ Das große Finale ist da: ULTIMATE CHRISTMAS AURA!' : `🎁 Türchen ${day} ist jetzt verfügbar!`); sfx.bell(1046.5, 0, 0.08); }
  }
}

/* ------------------------------------------------------------------ debug / QA time travel */
function setupDebug() {
  const el = $('debug'); el.hidden = false;
  const today = clock.today();
  el.innerHTML = `<b>QA</b><span>Datum:</span><input type="range" min="0" max="26" step="1" /><span id="dbg-date"></span>
    <button class="btn btn--ghost" id="dbg-reset" type="button">Fortschritt zurücksetzen</button>`;
  const range = el.querySelector('input');
  range.value = today.month === 12 ? Math.min(26, today.day) : 0;
  const apply = () => {
    const v = Number(range.value);
    const y = clock.today().year;
    clock.override = v === 0 ? clock.zonedMidnight(y, 11, 30) + 12 * 3600e3 : clock.zonedMidnight(y, 12, v) + 12 * 3600e3;
    $('dbg-date').textContent = v === 0 ? '30. Nov' : `${v}. Dez`;
    view.refresh(); tickCountdown();
  };
  range.addEventListener('input', apply);
  el.querySelector('#dbg-reset').addEventListener('click', () => { calendar.reset(); view.refresh(); updateProgress(); toast('Fortschritt zurückgesetzt'); });
  apply();
}

/* ------------------------------------------------------------------ boot */
async function boot() {
  $('event-year').textContent = String(calendar.year);
  $('tz-info').textContent = `Tägliche Freischaltung um 00:00 Uhr · Zeitzone: ${clock.timeZone}`;
  if (debug) setupDebug();
  calendar.onChange(() => { view.refresh(); updateProgress(); });
  updateProgress(); tickCountdown();
  setInterval(tickCountdown, 1000);
  if (EVENT_CONFIG.useServerTime && !debug) {
    clock.syncWithServer().then((ok) => { if (ok) { $('tz-info').textContent += ' · Serverzeit synchronisiert'; view.refresh(); tickCountdown(); } });
  }
  // door previews: render every cosmetic once (offscreen, async)
  const idle = window.requestIdleCallback ?? ((fn) => setTimeout(fn, 200));
  if (debug) window.__advent = { showcase, calendar, view, clock, REWARDS };
  if (!params.has('nopreviews')) idle(async () => {
    try {
      const { renderThumbnails } = await import('./three/thumbnails.js');
      await renderThumbnails(REWARDS, 320, (r, url) => view.setThumb(r.day, url));
    } catch (e) { console.warn('[advent] preview rendering failed', e); }
  }, { timeout: 1200 });
}
boot();
