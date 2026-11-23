import { REWARDS } from '../data/rewards.js';
import { RARITIES } from '../data/rarities.js';
import { DoorState } from '../core/calendar.js';

const ICON_LOCK = '<svg viewBox="0 0 24 24"><path d="M12 2a5 5 0 0 0-5 5v3H6a2 2 0 0 0-2 2v8a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8a2 2 0 0 0-2-2h-1V7a5 5 0 0 0-5-5zm-3 8V7a3 3 0 0 1 6 0v3zm3 4a2 2 0 0 1 1 3.7V19h-2v-1.3A2 2 0 0 1 12 14z"/></svg>';
const ICON_CHECK = '<svg viewBox="0 0 24 24"><path d="M5 12.5l4.5 4.5L19 7.5" stroke-linecap="round" stroke-linejoin="round"/></svg>';
const ICON_MISSED = '<svg viewBox="0 0 24 24" fill="#cfd6e4"><path d="M6 11h12v2H6z"/></svg>';
const BOW = '<svg class="door__bow" viewBox="0 0 100 60" aria-hidden="true"><path d="M50 32C38 10 12 2 8 16s20 24 42 16zM50 32c12-22 38-30 42-16S72 40 50 32z"/><path d="M50 32l-14 26 10-4 4 6zM50 32l14 26-10-4-4 6z" opacity=".85"/><circle cx="50" cy="31" r="7" style="fill:var(--rib);filter:brightness(1.15)"/></svg>';

/** Builds and updates the 24 calendar doors. */
export class CalendarView {
  constructor(root, calendar, { onOpen, onInspect, onLocked }) {
    this.root = root; this.calendar = calendar;
    this.handlers = { onOpen, onInspect, onLocked };
    this.doors = new Map();
    this.thumbs = new Map();
    this.build();
  }

  build() {
    this.root.innerHTML = '';
    for (const r of REWARDS) {
      const d = r.door;
      const el = document.createElement('button');
      el.type = 'button';
      el.className = `door r-${r.rarity} pat-${d.pattern} pal-${d.palette} rib-${d.ribbon} open-${d.open}`;
      el.dataset.day = r.day; el.style.setProperty('--i', r.day);
      const halves = d.open === 'double' || d.open === 'grand' ? '<div class="door__half door__half--l"></div><div class="door__half door__half--r"></div>' : '';
      el.innerHTML = `
        <span class="door__badge-new">${r.day === 24 ? 'FINALE' : 'HEUTE'}</span>
        <div class="door__box">
          <div class="door__inside"><img alt="" decoding="async" /></div>
          <i class="door__wall door__wall--t"></i><i class="door__wall door__wall--b"></i><i class="door__wall door__wall--l"></i><i class="door__wall door__wall--r"></i>
          <div class="door__lid">
            ${halves}
            ${r.rarity === 'mythic' ? '<div class="door__mythic-rays"></div>' : ''}
            <div class="door__ribbon"></div>${BOW}
            <span class="door__num">${r.day}</span>
            <span class="door__rarity">${RARITIES[r.rarity].label}</span>
            <span class="door__label">${r.name}</span>
            <span class="door__preview loading"><img alt="" decoding="async" /></span>
          </div>
        </div>
        <span class="door__status"></span>`;
      el.addEventListener('click', () => this.click(r));
      el.addEventListener('pointermove', (e) => {
        const b = el.getBoundingClientRect();
        el.style.setProperty('--ry', `${((e.clientX - b.left) / b.width - 0.5) * 22}deg`);
        el.style.setProperty('--rx', `${-((e.clientY - b.top) / b.height - 0.5) * 22}deg`);
        el.style.setProperty('--mx', `${((e.clientX - b.left) / b.width) * 100}%`);
        el.style.setProperty('--my', `${((e.clientY - b.top) / b.height) * 100}%`);
      });
      el.addEventListener('pointerleave', () => { el.style.setProperty('--rx', '0deg'); el.style.setProperty('--ry', '0deg'); });
      this.root.appendChild(el);
      this.doors.set(r.day, el);
    }
    this.refresh();
  }

  click(reward) {
    const st = this.calendar.doorState(reward.day);
    const el = this.doors.get(reward.day);
    if (st === DoorState.AVAILABLE) this.handlers.onOpen(reward, el);
    else if (st === DoorState.CLAIMED) this.handlers.onInspect(reward);
    else this.handlers.onLocked(reward, st);
  }

  setThumb(day, url) {
    this.thumbs.set(day, url);
    const el = this.doors.get(day); if (!el) return;
    el.querySelectorAll('img').forEach((img) => { img.src = url; });
    el.querySelector('.door__preview').classList.remove('loading');
  }

  refresh() {
    for (const r of REWARDS) {
      const el = this.doors.get(r.day);
      if (el.classList.contains('opening')) continue;
      const st = this.calendar.doorState(r.day);
      for (const s of Object.values(DoorState)) el.classList.toggle(`door--${s}`, s === st);
      const status = el.querySelector('.door__status');
      const icon = st === DoorState.LOCKED ? ICON_LOCK : st === DoorState.CLAIMED ? ICON_CHECK : st === DoorState.MISSED ? ICON_MISSED : '';
      if (status.dataset.icon !== st) { status.innerHTML = icon; status.dataset.icon = st; }
      const label = { locked: 'gesperrt', available: 'verfügbar – jetzt öffnen', claimed: 'eingesammelt', missed: 'verpasst' }[st];
      el.setAttribute('aria-label', `Türchen ${r.day}: ${r.name} (${RARITIES[r.rarity].label}) – ${label}`);
      el.setAttribute('aria-disabled', st === DoorState.LOCKED || st === DoorState.MISSED ? 'true' : 'false');
    }
  }
}
